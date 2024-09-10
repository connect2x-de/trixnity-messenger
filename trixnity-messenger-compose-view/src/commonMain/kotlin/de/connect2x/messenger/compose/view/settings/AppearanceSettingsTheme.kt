package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.ThemeMode
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel

interface AppearanceSettingsThemeView {
    @Composable
    fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel)
}

@Composable
fun ColumnScope.AppearanceSettingsTheme(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
    with(DI.current.get<AppearanceSettingsThemeView>()) {create(appearanceSettingsViewModel)}
}

class AppearanceSettingsThemeViewImpl : AppearanceSettingsThemeView {
    @Composable
    override fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
        val i18n = DI.current.get<I18nView>()
        val themeMode by appearanceSettingsViewModel.themeMode.collectAsState()
        val themeName = when (themeMode) {
            ThemeMode.LIGHT -> i18n.appearanceThemeLightHeading()
            ThemeMode.DARK -> i18n.appearanceThemeDarkHeading()
            else -> i18n.appearanceThemeDefaultHeading()
        }
        MoreOptions(i18n.appearanceThemeHeading(themeName)) {
            ThemeSetting(
                appearanceSettingsViewModel,
                i18n.appearanceThemeDefaultHeading(),
                i18n.appearanceThemeDefaultExplanation(),
                ThemeMode.DEFAULT
            )
            ThemeSetting(
                appearanceSettingsViewModel,
                i18n.appearanceThemeLightHeading(),
                i18n.appearanceThemeLightExplanation(),
                ThemeMode.LIGHT
            )
            ThemeSetting(
                appearanceSettingsViewModel,
                i18n.appearanceThemeDarkHeading(),
                i18n.appearanceThemeDarkExplanation(),
                ThemeMode.DARK
            )
        }
    }
}

@Composable
fun ThemeSetting(
    appearanceSettingsViewModel: AppearanceSettingsViewModel,
    title: String,
    explanation: String,
    value: ThemeMode,
) {
    val themeMode by appearanceSettingsViewModel.themeMode.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clickable {
                appearanceSettingsViewModel.setThemeMode(value)
            }
            .buttonPointerModifier(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HelpIcon(explanation)
        Text(title, modifier = Modifier.weight(1.0f, fill = true))
        RadioButton(
            selected = themeMode == value,
            onClick = {
                appearanceSettingsViewModel.setThemeMode(value)
            }
        )
    }
}
