package org.churchpresenter.app.churchpresenter.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.churchpresenter.app.churchpresenter.utils.Constants
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Not exercised: CallbackResult.Timeout (CALLBACK_TIMEOUT_MS, 5 minutes, not injectable) and the
// bind-failure branch (Netty takes ~4s to report a taken port, which is a duration, not a test).
class PlanningCenterAuthServerTest {

    private fun url(query: String) = "http://127.0.0.1:${Constants.PLANNING_CENTER_OAUTH_PORT}/callback$query"

    /** True once something is actually accepting connections on 127.0.0.1:port — a bind-based
     *  check is wrong here since a wildcard probe bind can coexist with a loopback-specific one. */
    private fun isListening(): Boolean =
        runCatching { Socket().apply { connect(InetSocketAddress("127.0.0.1", Constants.PLANNING_CENTER_OAUTH_PORT), 200) }.close() }.isSuccess

    // The fixed port isn't released by the OS the instant the previous test's server.stop()
    // returns; without this, the next test's bind can race a still-closing socket.
    @AfterTest
    fun awaitPortFree() {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (!isListening()) return
            Thread.sleep(20)
        }
    }

    private suspend fun awaitServerReady(resultDeferred: kotlinx.coroutines.Deferred<PlanningCenterAuthServer.CallbackResult>? = null) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (resultDeferred?.isCompleted == true) {
                error("server never bound: awaitAuthorizationCode already resolved to ${resultDeferred.await()}")
            }
            if (isListening()) return
            delay(10)
        }
        error("server on port ${Constants.PLANNING_CENTER_OAUTH_PORT} never started")
    }

    @Test
    fun `a callback carrying a code resolves to Success`() = runBlocking {
        val resultDeferred = async(Dispatchers.IO) { PlanningCenterAuthServer.awaitAuthorizationCode() }
        awaitServerReady(resultDeferred)

        val client = HttpClient(CIO)
        try {
            client.get(url("?code=abc123"))
        } finally {
            client.close()
        }

        val result = withTimeout(10_000) { resultDeferred.await() }
        val success = assertIs<PlanningCenterAuthServer.CallbackResult.Success>(result)
        assertEquals("abc123", success.code)
    }

    @Test
    fun `a callback carrying an error resolves to Error with that message`() = runBlocking {
        val resultDeferred = async(Dispatchers.IO) { PlanningCenterAuthServer.awaitAuthorizationCode() }
        awaitServerReady(resultDeferred)

        val client = HttpClient(CIO)
        try {
            client.get(url("?error=access_denied"))
        } finally {
            client.close()
        }

        val result = withTimeout(10_000) { resultDeferred.await() }
        val error = assertIs<PlanningCenterAuthServer.CallbackResult.Error>(result)
        assertEquals("access_denied", error.message)
    }

    @Test
    fun `a callback with neither code nor error resolves to a generic Error`() = runBlocking {
        val resultDeferred = async(Dispatchers.IO) { PlanningCenterAuthServer.awaitAuthorizationCode() }
        awaitServerReady(resultDeferred)

        val client = HttpClient(CIO)
        try {
            client.get(url(""))
        } finally {
            client.close()
        }

        val result = withTimeout(10_000) { resultDeferred.await() }
        val error = assertIs<PlanningCenterAuthServer.CallbackResult.Error>(result)
        assertEquals("No authorization code returned", error.message)
    }

    @Test
    fun `the callback page tells the user they can close the window`() = runBlocking {
        val resultDeferred = async(Dispatchers.IO) { PlanningCenterAuthServer.awaitAuthorizationCode() }
        awaitServerReady(resultDeferred)

        val client = HttpClient(CIO)
        val body = try {
            client.get(url("?code=xyz")).bodyAsText()
        } finally {
            client.close()
        }
        assertTrue(body.contains("close this window"), body)

        val finalResult = withTimeout(10_000) { resultDeferred.await() }
        assertIs<PlanningCenterAuthServer.CallbackResult.Success>(finalResult)
        Unit
    }
}
