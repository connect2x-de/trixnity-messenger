package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.RadioSetting
import de.connect2x.trixnity.messenger.compose.view.common.RadioSettingOption
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.ThemeMode
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel

interface AppearanceSettingsThemeView {
    @Composable
    fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel)
}

@Composable
fun ColumnScope.AppearanceSettingsTheme(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
    with(DI.get<AppearanceSettingsThemeView>()) { create(appearanceSettingsViewModel) }
}

class AppearanceSettingsThemeViewImpl : AppearanceSettingsThemeView {
    @Composable
    override fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val themeMode by appearanceSettingsViewModel.themeMode.collectAsState()
        val themeName = when (themeMode) {
            ThemeMode.LIGHT -> i18n.appearanceThemeLightHeading()
            ThemeMode.DARK -> i18n.appearanceThemeDarkHeading()
            else -> i18n.appearanceThemeDefaultHeading()
        }
        val themeExplanation = when (themeMode) {
            ThemeMode.LIGHT -> i18n.appearanceThemeLightExplanation()
            ThemeMode.DARK -> i18n.appearanceThemeDarkExplanation()
            else -> i18n.appearanceThemeDefaultExplanation()
        }
        RadioSetting(
            title = {
                Tooltip({ Text(themeExplanation) }) {
                    Text(i18n.appearanceThemeHeading(themeName), style = MaterialTheme.typography.titleSmall)
                }
            },
            options = mapOf(
                ThemeMode.DEFAULT to RadioSettingOption(
                    i18n.appearanceThemeDefaultHeading(),
                    i18n.appearanceThemeDefaultHeading(),
                ),
                ThemeMode.LIGHT to RadioSettingOption(
                    i18n.appearanceThemeLightHeading(),
                    i18n.appearanceThemeLightExplanation(),
                ),
                ThemeMode.DARK to RadioSettingOption(
                    i18n.appearanceThemeDarkHeading(),
                    i18n.appearanceThemeDarkExplanation(),
                ),
            ),
            value = themeMode,
            set = { appearanceSettingsViewModel.setThemeMode(it) },
        )
    }
}

