package de.connect2x.trixnity.messenger.compose.view.space

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsJoinRulesView
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsMemberList
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsNameView
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsPowerlevel
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomSettingsTopicView
import de.connect2x.trixnity.messenger.compose.view.room.settings.SettingsIdentity
import de.connect2x.trixnity.messenger.compose.view.room.settings.SettingsLeave
import de.connect2x.trixnity.messenger.compose.view.room.settings.SettingsPane
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNameViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceSettingsViewModel

interface SpaceSettingsView {
    @Composable fun create(viewModel: SpaceSettingsViewModel, isSinglePane: Boolean)
}

@Composable
fun SpaceSettings(viewModel: SpaceSettingsViewModel, isSinglePane: Boolean) {
    DI.get<SpaceSettingsView>().create(viewModel, isSinglePane)
}

class SpaceSettingsViewImpl : SpaceSettingsView {
    @Composable
    override fun create(viewModel: SpaceSettingsViewModel, isSinglePane: Boolean) {
        val i18n = DI.get<I18nView>()
        val details = viewModel.details
        SettingsPane(details, isSinglePane, i18n.spacesSettings()) {
            SettingsIdentity(
                details,
                name = { SpaceSettingsName(details.roomSettingsNameViewModel) },
                topic = { SpaceSettingsTopic(details.roomSettingsTopicViewModel) },
            )
            HorizontalDivider()
            SpaceSettingsJoinRules(details)
            HorizontalDivider()
            RoomSettingsMemberList(details)
            HorizontalDivider()
            RoomSettingsPowerlevel(details)
            SettingsLeave(details)
        }
    }
}

@Composable
private fun SpaceSettingsName(viewModel: RoomSettingsNameViewModel) {
    val i18n = DI.get<I18nView>()
    DI.get<RoomSettingsNameView>()
        .create(viewModel, i18n.spacesName(), i18n.spacesNamePlaceholder(), i18n.spacesEditName())
}

@Composable
private fun SpaceSettingsTopic(viewModel: RoomSettingsTopicViewModel) {
    val i18n = DI.get<I18nView>()
    DI.get<RoomSettingsTopicView>()
        .create(viewModel, i18n.spacesDescription(), i18n.spacesDescriptionPlaceholder(), i18n.spacesEditDescription())
}

@Composable
private fun SpaceSettingsJoinRules(viewModel: RoomSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    DI.get<RoomSettingsJoinRulesView>().create(viewModel, i18n.spacesJoinRule(), i18n.spacesJoinRule())
}
