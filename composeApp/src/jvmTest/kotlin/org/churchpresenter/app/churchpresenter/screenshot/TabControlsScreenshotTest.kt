@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.churchpresenter.app.churchpresenter.composables.ActionIconButton
import org.churchpresenter.app.churchpresenter.composables.AddToScheduleButton
import org.churchpresenter.app.churchpresenter.composables.FocusLostBanner
import org.churchpresenter.app.churchpresenter.composables.FocusLostRescueState
import org.churchpresenter.app.churchpresenter.composables.GoLiveButton
import kotlin.test.Test

class TabControlsScreenshotTest {

    @Test
    fun `go live enabled`() = captureComponent(SECTION, "go_live_enabled") {
        GoLiveButton(onClick = {}, tooltipText = "Go Live")
    }

    @Test
    fun `go live disabled`() = captureComponent(SECTION, "go_live_disabled") {
        GoLiveButton(onClick = {}, tooltipText = "Go Live", enabled = false)
    }

    @Test
    fun `go live dimmed`() = captureComponent(SECTION, "go_live_dimmed") {
        GoLiveButton(onClick = {}, tooltipText = "Go Live", dimmed = true)
    }

    @Test
    fun `add to schedule enabled`() = captureComponent(SECTION, "add_to_schedule_enabled") {
        AddToScheduleButton(onClick = {}, tooltipText = "Add to Schedule")
    }

    @Test
    fun `add to schedule disabled`() = captureComponent(SECTION, "add_to_schedule_disabled") {
        AddToScheduleButton(onClick = {}, tooltipText = "Add to Schedule", enabled = false)
    }

    @Test
    fun `action icon enabled`() = captureComponent(SECTION, "action_icon_enabled") {
        ActionIconButton(onClick = {}, tooltipText = "Search", icon = Icons.Filled.Search)
    }

    @Test
    fun `action icon disabled`() = captureComponent(SECTION, "action_icon_disabled") {
        ActionIconButton(
            onClick = {},
            tooltipText = "Search",
            icon = Icons.Filled.Search,
            enabled = false,
        )
    }

    @Test
    fun `the action row as a tab draws it`() = captureComponent(SECTION, "action_row") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionIconButton(
                onClick = {},
                tooltipText = "Search",
                icon = Icons.Filled.Search,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            AddToScheduleButton(onClick = {}, tooltipText = "Add to Schedule")
            GoLiveButton(onClick = {}, tooltipText = "Go Live")
        }
    }

    @Test
    fun `focus lost banner`() = captureComponent(SECTION, "focus_lost_banner") {
        FocusLostBanner(
            state = rescueState(),
            text = "Keyboard shortcuts paused — click here to restore",
        )
    }

    private fun rescueState() = FocusLostRescueState(
        null,
        FocusRequester(),
        CoroutineScope(Dispatchers.Unconfined),
    )

    private companion object {
        const val SECTION = "tabControls"
    }
}
