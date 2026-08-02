package org.churchpresenter.app.churchpresenter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun `getPlatform returns a JVMPlatform whose name reports the running Java version`() {
        assertEquals("Java ${System.getProperty("java.version")}", getPlatform().name)
    }

    @Test
    fun `JVMPlatform name reports the running Java version`() {
        val platform = JVMPlatform()
        assertEquals("Java ${System.getProperty("java.version")}", platform.name)
    }

    @Test
    fun `JVMPlatform name is prefixed with Java`() {
        assertTrue(JVMPlatform().name.startsWith("Java "))
    }
}
