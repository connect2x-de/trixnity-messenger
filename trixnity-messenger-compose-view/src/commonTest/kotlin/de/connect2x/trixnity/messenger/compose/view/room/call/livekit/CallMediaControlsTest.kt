package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallMediaError
import de.connect2x.trixnity.messenger.call.CallPermissionKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(TrixnityMessengerCallApi::class)
class CallMediaControlsTest {
    @Test
    fun mediaChoicesCanChangeBeforeThereIsAConnection() {
        val requests = mutableListOf<CallPermissionKind>()
        val controls = CallMediaControls(requests::add)
        controls.toggle(CallPermissionKind.MICROPHONE)
        controls.toggle(CallPermissionKind.CAMERA)
        assertTrue(controls.microphoneEnabled.value)
        assertTrue(controls.cameraEnabled.value)
        assertEquals(listOf(CallPermissionKind.MICROPHONE), requests)
        controls.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        assertEquals(listOf(CallPermissionKind.MICROPHONE, CallPermissionKind.CAMERA), requests)
        controls.onPermissionRequestResult(CallPermissionKind.CAMERA, false)
        assertTrue(controls.microphoneEnabled.value)
        assertFalse(controls.cameraEnabled.value)
        assertEquals(CallMediaError.CAMERA_PERMISSION, controls.error.value)
    }

    @Test
    fun turningMediaOffDuringAPermissionPromptStaysOffAfterGrantingIt() {
        val requests = mutableListOf<CallPermissionKind>()
        val controls = CallMediaControls(requests::add)
        controls.toggle(CallPermissionKind.MICROPHONE)
        controls.toggle(CallPermissionKind.MICROPHONE)
        controls.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        assertFalse(controls.microphoneEnabled.value)
        controls.toggle(CallPermissionKind.MICROPHONE)
        controls.toggle(CallPermissionKind.MICROPHONE)
        assertEquals(listOf(CallPermissionKind.MICROPHONE), requests)
        assertFalse(controls.microphoneEnabled.value)
    }

    @Test
    fun turningOneDeviceOffDoesNotWaitForPermissionToEnableTheOther() {
        val requests = mutableListOf<CallPermissionKind>()
        val controls = CallMediaControls(requests::add)
        controls.toggle(CallPermissionKind.MICROPHONE)
        controls.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        controls.toggle(CallPermissionKind.CAMERA)
        controls.toggle(CallPermissionKind.MICROPHONE)
        assertFalse(controls.microphoneEnabled.value)
        assertEquals(listOf(CallPermissionKind.MICROPHONE, CallPermissionKind.CAMERA), requests)
    }

    @Test
    fun failedMuteReportsTheMicrophoneStillActiveAndAllowsRetry() {
        val controls = CallMediaControls {}
        controls.toggle(CallPermissionKind.MICROPHONE)
        controls.onPermissionRequestResult(CallPermissionKind.MICROPHONE, true)
        controls.toggle(CallPermissionKind.MICROPHONE)
        controls.onMediaChangeFailure(CallPermissionKind.MICROPHONE, requested = false, actual = true)
        assertTrue(controls.microphoneEnabled.value)
        assertEquals(CallMediaError.MICROPHONE, controls.error.value)
        controls.toggle(CallPermissionKind.MICROPHONE)
        assertFalse(controls.microphoneEnabled.value)
    }
}
