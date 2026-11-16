package de.connect2x.trixnity.messenger.internal.compose.view.scenes.call

import androidx.navigation3.runtime.NavEntry
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(TrixnityMessengerCallApi::class, MSC4143::class)
class CallControlsVisibilityTest {
    private val target =
        CallTarget(UserId("alice", "example.org"), RoomId("!call:example.org"), CallRtcApplication.SLOT_ID)
    private val timeline = TimelineRoute(target.userId, target.roomId)
    private val timelineEntries = listOf(NavEntry("timeline") {})
    private val callEntries = listOf(NavEntry("call", metadata = CallControlsScene.expandedCall(target)) {})

    @Test
    fun openingCallDoesNotShowControlsInThePreviousTimelineScene() {
        assertFalse(CallControlsScene.showCompactControls(null, timeline, timelineEntries))
        val call = RoomCallRoute(target)
        assertFalse(CallControlsScene.showCompactControls(target, call, timelineEntries))
        assertFalse(CallControlsScene.showCompactControls(target, call, callEntries))
        assertTrue(CallControlsScene.showCompactControls(target, timeline, timelineEntries))
    }

    @Test
    fun visibleExpandedCallHidesControlsEvenWhenTheTimelineIsTheForegroundRoute() {
        assertFalse(CallControlsScene.showCompactControls(target, timeline, callEntries))
    }

    @Test
    fun anotherSlotDoesNotHideTheActiveCallsControls() {
        val other = RoomCallRoute(target.copy(slotId = RtcSlotId("breakout")))
        assertTrue(CallControlsScene.showCompactControls(target, other, timelineEntries))
    }
}
