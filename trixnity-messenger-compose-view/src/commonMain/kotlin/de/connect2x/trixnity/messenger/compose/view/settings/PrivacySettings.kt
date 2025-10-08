package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsSingleAccountViewModel

// TODO TIM
interface PrivacySettingsView {
    @Composable
    fun create(privacySettingsViewModel: PrivacySettingsAllAccountsViewModel)
}

@Composable
fun PrivacySettings(privacySettingsViewModel: PrivacySettingsAllAccountsViewModel) {
    DI.get<PrivacySettingsView>().create(privacySettingsViewModel)
}

class PrivacySettingsViewImpl : PrivacySettingsView {
    @Composable
    override fun create(privacySettingsViewModel: PrivacySettingsAllAccountsViewModel) {
        val privacySettings = privacySettingsViewModel.privacySettings.collectAsState().value
        val i18n = DI.get<I18nView>()
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            Column {
                Header(privacySettingsViewModel::back, i18n.privacyTitle())

                Box {
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                        privacySettings.map { privacySetting ->
                            PrivacySettingsSingleAccount(privacySetting)
                        }
                    }
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        scroll,
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsSingleAccount(privacySettingViewModel: PrivacySettingsSingleAccountViewModel) {
    val presenceIsPublic = privacySettingViewModel.presenceIsPublic.collectAsState().value
    val readMarkerIsPublic = privacySettingViewModel.readMarkerIsPublic.collectAsState().value
    val typingIsPublic = privacySettingViewModel.typingIsPublic.collectAsState().value
    val i18n = DI.get<I18nView>()

    SettingsAccountCard(privacySettingViewModel.account) {
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.privacyPresenceIsPublic()) },
            supportingContent = { Text(i18n.privacyPresenceIsPublicExplanation(DI.get<MatrixMessengerConfiguration>().appName)) },
            selected = presenceIsPublic,
            onChange = { privacySettingViewModel.togglePresenceIsPublic() },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.privacyReadMarkerIsPublic()) },
            supportingContent = { Text(i18n.privacyReadMarkerIsPublicExplanation()) },
            selected = readMarkerIsPublic,
            onChange = { privacySettingViewModel.toggleReadMarkerIsPublic() },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.privacyTypingIsPublic()) },
            supportingContent = { Text(i18n.privacyTypingIsPublicExplanation()) },
            selected = typingIsPublic,
            onChange = { privacySettingViewModel.toggleTypingIsPublic() },
        )

        val blockedCount = privacySettingViewModel.blockedContactsCount.collectAsState().value
        val interactionSource = remember { MutableInteractionSource() }
        ElevatedCard(
            Modifier
                .padding(bottom = 10.dp)
                .clickable(
                    interactionSource,
                    LocalIndication.current
                ) { privacySettingViewModel.showBlockedContactsSettings() }
                .focusHighlighting(interactionSource)
                .buttonPointerModifier()
                .fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = i18n.blockedContactsButtonCaption(blockedCount),
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, i18n.blockedContactsHeader())
            }
        }
    }
}
