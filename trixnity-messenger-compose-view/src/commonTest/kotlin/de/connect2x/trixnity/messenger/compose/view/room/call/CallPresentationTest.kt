package de.connect2x.trixnity.messenger.compose.view.room.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallConnectionState
import de.connect2x.trixnity.messenger.call.CallMediaError
import de.connect2x.trixnity.messenger.call.CallMediaState
import de.connect2x.trixnity.messenger.call.CallSessionState
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.util.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.datetime.TimeZone

@OptIn(TrixnityMessengerCallApi::class)
class CallPresentationTest {
    private val i18n =
        I18nView(DefaultLanguages, createTestMatrixMessengerSettingsHolder(), GetSystemLang { "en" }, TimeZone.UTC)

    @Test
    fun signallingErrorsLeaveTransmittingMediaControlsAvailable() {
        val presentation =
            CallSessionState(
                    media =
                        CallMediaState(
                            connection = CallConnectionState.CONNECTED,
                            microphoneEnabled = true,
                            cameraEnabled = true,
                        ),
                    error = "Membership renewal failed",
                )
                .toCallState(i18n)
        val active = assertIs<CallState.Active>(presentation)
        assertTrue(active.microphone.active && active.microphone.enabled)
        assertTrue(active.camera.active && active.camera.enabled)
        assertTrue(active.messages.any { it.isError && it.text == "Membership renewal failed" })
    }

    @Test
    fun connectionFailuresAndPendingChangesLeaveBothControlsAvailable() {
        for (connection in CallConnectionState.entries) {
            val active =
                assertIs<CallState.Active>(
                    CallSessionState(
                            media =
                                CallMediaState(
                                    connection = connection,
                                    changingMedia = true,
                                    error = CallMediaError.CONNECTION,
                                    participantsUnavailable = true,
                                )
                        )
                        .toCallState(i18n)
                )
            assertTrue(active.microphone.enabled)
            assertTrue(active.camera.enabled)
            assertTrue(active.messages.any { it.isError })
        }
    }
}
