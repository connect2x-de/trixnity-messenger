package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.createTestMatrixMultiMessenger
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.platformType
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewRoomSettingsViewModel
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalTestApi::class)
class RoomSettingsWarningUiTest {
    @Test
    fun confirmationRendersTheSpaceWarningForLeavingAndForgetting() = runComposeUiTest {
        val messenger = createTestMatrixMultiMessenger(Dispatchers.Default)
        val i18n = messenger.di.get<I18n>()
        val viewModel =
            PreviewRoomSettingsViewModel().apply {
                isDirect.value = false
                leaveRoomWarningMessage.value = i18n.settingsSpaceLeaveRoomWarningMessage()
            }
        try {
            setContent {
                CompositionLocalProvider(
                    Platform provides platformType(),
                    DI provides messenger.di,
                    IsFocusHighlighting provides false,
                ) {
                    MessengerTheme { RoomSettingsLeaveRoomWarning(viewModel) }
                }
            }
            onNodeWithText(i18n.settingsSpaceLeaveRoomWarningMessage()).assertIsDisplayed()
            viewModel.isLeave.value = true
            viewModel.leaveRoomWarningMessage.value = i18n.settingsSpaceForgetRoomWarningMessage()
            waitForIdle()
            onNodeWithText(i18n.settingsSpaceForgetRoomWarningMessage()).assertIsDisplayed()
        } finally {
            messenger.closeSuspending()
        }
    }
}
