package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.PlatformType
import de.connect2x.trixnity.messenger.compose.view.createTestMatrixMultiMessenger
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.platformType
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettings
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsJoinRulesView
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsNameView
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsPowerlevelView
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsPowerlevelViewImpl
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsTopicView
import de.connect2x.trixnity.messenger.compose.view.theme.IsFocusHighlighting
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.compose.view.util.screenshot
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.PreviewApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewRoomSettingsNameViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewRoomSettingsTopicViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewRoomSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNameViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceSettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class SpaceSettingsUiTest {
    @Test
    fun spaceSettingsUseRegisteredEditorOverrides() = runComposeUiTest {
        val messenger = createTestMatrixMultiMessenger(Dispatchers.Default)
        messenger.di.loadModules(
            listOf(
                module {
                    single<RoomSettingsNameView> {
                        object : RoomSettingsNameView {
                            @Composable
                            override fun create(roomSettingsNameViewModel: RoomSettingsNameViewModel) {
                                Text("Custom name editor")
                            }
                        }
                    }
                    single<RoomSettingsTopicView> {
                        object : RoomSettingsTopicView {
                            @Composable
                            override fun create(roomSettingsTopicViewModel: RoomSettingsTopicViewModel) {
                                Text("Custom description editor")
                            }
                        }
                    }
                    single<RoomSettingsJoinRulesView> {
                        object : RoomSettingsJoinRulesView {
                            @Composable
                            override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
                                Text("Access managed by extension")
                            }
                        }
                    }
                }
            )
        )
        val i18n = messenger.di.get<I18nView>()
        val viewModel =
            object : SpaceSettingsViewModel {
                override val details =
                    PreviewRoomSettingsViewModel().apply {
                        isDirect.value = false
                        memberListViewModel.filterByMemberships.value = MemberListViewModel.defaultMemberships
                    }
            }
        try {
            setContent {
                CompositionLocalProvider(
                    Platform provides platformType(),
                    DI provides messenger.di,
                    IsFocusHighlighting provides false,
                ) {
                    MessengerTheme {
                        ThemedSurface(style = MaterialTheme.components.details) {
                            SettingsTestViewport(DpSize(360.dp, 720.dp)) { SpaceSettings(viewModel, true) }
                        }
                    }
                }
            }
            onNodeWithText("Custom name editor").assertIsDisplayed()
            onNodeWithText("Custom description editor").assertIsDisplayed()
            onNodeWithText("Access managed by extension").performScrollTo().assertIsDisplayed()
            onNodeWithContentDescription(i18n.spacesEditName()).assertDoesNotExist()
            onNodeWithContentDescription(i18n.spacesEditDescription()).assertDoesNotExist()
            onNodeWithText(i18n.spacesJoinRule()).assertDoesNotExist()
        } finally {
            messenger.closeSuspending()
        }
    }

    @Test
    fun settingsShareEditorsAndKeepTimelineOptionsInRooms() = runComposeUiTest {
        val messenger = createTestMatrixMultiMessenger(Dispatchers.Default)
        messenger.di.loadModules(
            listOf(
                module {
                    single<UriCaller> { UriCaller { _, _ -> } }
                    single<RoomSettingsPowerlevelView> { RoomSettingsPowerlevelViewImpl() }
                }
            )
        )
        val i18n = messenger.di.get<I18nView>()
        fun editableField() =
            object : ApprovableTextFieldViewModel by PreviewApprovableTextFieldViewModel() {
                override val isEdit = MutableStateFlow(false)

                override fun startEdit() {
                    isEdit.value = true
                }

                override fun cancelEdit() {
                    isEdit.value = false
                }
            }
        val preview =
            PreviewRoomSettingsViewModel().apply {
                isDirect.value = false
                memberListViewModel.filterByMemberships.value = MemberListViewModel.defaultMemberships
            }
        var closes = 0
        val details =
            object : RoomSettingsViewModel by preview {
                override fun close() {
                    closes++
                }

                override val roomSettingsNameViewModel =
                    object : RoomSettingsNameViewModel by PreviewRoomSettingsNameViewModel() {
                        override val roomName = editableField()
                    }
                override val roomSettingsTopicViewModel =
                    object : RoomSettingsTopicViewModel by PreviewRoomSettingsTopicViewModel() {
                        override val roomTopic = editableField()
                    }
            }
        val viewModel =
            object : SpaceSettingsViewModel {
                override val details = details
            }
        lateinit var inputModeManager: InputModeManager
        val width = mutableStateOf(360.dp)
        val showRoom = mutableStateOf(false)
        try {
            setContent {
                inputModeManager = LocalInputModeManager.current
                CompositionLocalProvider(
                    Platform provides platformType(),
                    DI provides messenger.di,
                    IsFocusHighlighting provides false,
                ) {
                    MessengerTheme {
                        ThemedSurface(style = MaterialTheme.components.details) {
                            SettingsTestViewport(DpSize(width.value, 720.dp)) {
                                Box(Modifier.testTag("Settings")) {
                                    if (showRoom.value) RoomSettings(details, true)
                                    else SpaceSettings(viewModel, width.value == 360.dp)
                                }
                            }
                        }
                    }
                }
            }
            for (size in listOf(360.dp, 900.dp)) {
                width.value = size
                waitForIdle()
                onNodeWithText(i18n.spacesSettings()).assertIsDisplayed()
                val closeLabel = if (size == 360.dp) i18n.commonBack() else i18n.commonClose()
                val otherLabel = if (size == 360.dp) i18n.commonClose() else i18n.commonBack()
                onNodeWithContentDescription(closeLabel).assertIsDisplayed()
                onNodeWithContentDescription(otherLabel).assertDoesNotExist()
                runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
                onNodeWithContentDescription(i18n.spacesEditName()).performSemanticsAction(
                    SemanticsActions.RequestFocus
                ) {
                    it()
                }
                onNodeWithContentDescription(i18n.spacesEditName()).assertIsFocused().performKeyInput {
                    pressKey(Key.Enter)
                }
                onNodeWithText(i18n.spacesName()).assertIsDisplayed()
                onNodeWithText(i18n.actionCancel()).performClick()
                onNodeWithContentDescription(i18n.spacesEditDescription()).performClick()
                onNodeWithText(i18n.spacesDescription()).assertIsDisplayed()
                onNodeWithText(i18n.actionCancel()).performClick()
                onNodeWithText(i18n.chatJoinRule()).assertDoesNotExist()
                onNodeWithText(i18n.chatHistoryVisibility()).assertDoesNotExist()
                onNodeWithText(i18n.roomEndToEndEncryption()).assertDoesNotExist()
                onNodeWithText(i18n.exportRoom(i18n.commonGroup())).assertDoesNotExist()
                runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
                onNodeWithContentDescription(closeLabel).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                onNodeWithContentDescription(closeLabel).assertIsFocused().performKeyInput { pressKey(Key.Enter) }
                assertEquals(if (size == 360.dp) 1 else 2, closes)
                screenshot("SpaceSettingsUiTest", "settings-${size.value.toInt()}", "Settings")
            }
            onNodeWithText(i18n.spacesJoinRule()).performScrollTo().assertIsDisplayed()
            onNodeWithText(i18n.changePowerLevelHeader()).performScrollTo().assertIsDisplayed()
            showRoom.value = true
            waitForIdle()
            onNodeWithText(i18n.roomSettings().replaceFirstChar { it.uppercase() }).assertIsDisplayed()
            onNodeWithText(i18n.chatHistoryVisibility()).performScrollTo().assertIsDisplayed()
            onNodeWithText(i18n.chatJoinRule()).performScrollTo().assertIsDisplayed()
            onNodeWithText(i18n.exportRoom(i18n.commonGroup())).performScrollTo().assertIsDisplayed()
        } finally {
            messenger.closeSuspending()
        }
    }
}

@Composable
private fun SettingsTestViewport(size: DpSize, content: @Composable () -> Unit) {
    if (platformType() == PlatformType.ANDROID) {
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size), content)
    } else {
        // ForcedSize is not implemented on the other Compose targets yet.
        Box(Modifier.requiredSize(size)) { content() }
    }
}
