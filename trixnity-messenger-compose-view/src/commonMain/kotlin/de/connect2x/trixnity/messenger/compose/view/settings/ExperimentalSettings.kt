package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.PlatformType
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.viewmodel.settings.ExperimentalSettingsViewModel

interface ExperimentalSettingsView {
    @Composable fun create(experimentalSettingsViewModel: ExperimentalSettingsViewModel)
}

@Composable
fun ExperimentalSettings(experimentalSettingsViewModel: ExperimentalSettingsViewModel) {
    DI.get<ExperimentalSettingsView>().create(experimentalSettingsViewModel)
}

class ExperimentalSettingsViewImpl : ExperimentalSettingsView {
    @Composable
    @TrixnityMessengerCallApi
    override fun create(experimentalSettingsViewModel: ExperimentalSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val configuration = DI.get<MatrixMessengerConfiguration>()
        val scroll = rememberScrollState()
        val platform = Platform.current

        val enableVideoCalls by experimentalSettingsViewModel.videoCallsEnabled.collectAsState()
        val videoCallsSupported = platform == PlatformType.ANDROID || platform == PlatformType.WEB

        Box(Modifier.fillMaxSize()) {
            Column {
                Header(experimentalSettingsViewModel::back, i18n.experimentalSettingsTitle())
                Box {
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                        if (configuration.features.enableVideoCalls) {
                            SettingsCard(title = i18n.experimentalSettingsCallsTitle(), icon = Icons.Default.Videocam) {
                                ThemedListItemSwitch(
                                    style = MaterialTheme.components.settingsItem,
                                    headlineContent = { Text(i18n.experimentalSettingsVideoCalls()) },
                                    supportingContent = {
                                        Column {
                                            Text(i18n.experimentalSettingsVideoCallsExplanation())
                                            if (!videoCallsSupported) {
                                                Text(
                                                    text = i18n.experimentalSettingsVideoCallsDisableExplanation(),
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            }
                                        }
                                    },
                                    selected = enableVideoCalls && videoCallsSupported,
                                    enabled = videoCallsSupported,
                                    onChange = { experimentalSettingsViewModel.toggleVideoCallsEnabled() },
                                )
                            }
                        }
                    }
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), scroll)
                }
            }
        }
    }
}
