package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.DefaultAccentColor
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel

interface AppearanceSettingsView {
    @Composable
    fun create(appearanceSettingsViewModel: AppearanceSettingsViewModel)
}

@Composable
fun AppearanceSettings(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
    DI.get<AppearanceSettingsView>().create(appearanceSettingsViewModel)
}

class AppearanceSettingsViewImpl : AppearanceSettingsView {
    @Composable
    override fun create(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val scroll = rememberScrollState()

        val defaultAccentColor = DI.get<DefaultAccentColor>().value
        val isHighContrast by appearanceSettingsViewModel.isHighContrast.collectAsState()
        val packedAccentColor by appearanceSettingsViewModel.accentColor.collectAsState()
        val a11yMode by appearanceSettingsViewModel.isFocusHighlighting.collectAsState()
        val accentColor = packedAccentColor?.let { Color(it.toULong()) } ?: defaultAccentColor

        Box(Modifier.fillMaxSize()) {
            Column {
                Header(appearanceSettingsViewModel::back, i18n.appearanceTitle())
                Box {
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                        SettingsCard(title = i18n.appearanceColorsTitle(), icon = Icons.Filled.Colorize) {
                            AppearanceSettingsTheme(appearanceSettingsViewModel)
                            Spacer(Modifier.height(15.dp))
                            AppearanceSettingsColor(
                                i18n.appearanceAccentColorHeading(),
                                defaultAccentColor,
                                accentColor
                            ) {
                                appearanceSettingsViewModel.setAccentColor(it.value.toLong())
                            }
                        }
                        SettingsCard(title = i18n.appearanceAccessibilityTitle(), icon = Icons.Filled.FormatSize) {
                            AppearanceSettingsSize(appearanceSettingsViewModel)
                            Spacer(Modifier.height(15.dp))
                            Setting(
                                text = i18n.appearanceHighContrastHeading(),
                                explanation = i18n.appearanceHighContrastExplanation(),
                                value = isHighContrast
                            ) {
                                appearanceSettingsViewModel.toggleHighContrast()
                            }
                            Setting(
                                text = i18n.appearanceFocusHighlightingHeading(),
                                explanation = i18n.appearanceFocusHighlightingExplanation(),
                                value = a11yMode,
                            ) {
                                appearanceSettingsViewModel.toggleA11yMode()
                            }
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
