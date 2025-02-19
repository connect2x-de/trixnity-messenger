package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementHolder
import de.connect2x.messenger.compose.view.theme.DefaultSizes
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel2
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel3
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel

interface AppearanceSettingsSizeView {
    @Composable
    fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel)
}

@Composable
fun ColumnScope.AppearanceSettingsSize(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
    with(DI.get<AppearanceSettingsSizeView>()) { create(appearanceSettingsViewModel) }
}

class AppearanceSettingsSizeViewImpl : AppearanceSettingsSizeView {
    @Composable
    override fun ColumnScope.create(appearanceSettingsViewModel: AppearanceSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val defaultSizes = DI.get<DefaultSizes>()

        val fontSize = appearanceSettingsViewModel.fontSize.collectAsState().value ?: defaultSizes.fontSize
        var newFontSize by remember { mutableStateOf(-1F) }
        fun getNewFontSize(): Float = if (newFontSize != -1F) newFontSize else fontSize

        val displaySize = appearanceSettingsViewModel.displaySize.collectAsState().value ?: defaultSizes.displaySize
        var newDisplaySize by remember { mutableStateOf(-1F) }
        fun getNewDisplaySize(): Float = if (newDisplaySize != -1F) newDisplaySize else displaySize

        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density * getNewDisplaySize(), newFontSize)) {
            Column(Modifier.padding(end = 10.dp)) {
                TimelineElementHolder(PreviewTimelineElementViewModel2())
                TimelineElementHolder(PreviewTimelineElementViewModel3())
            }
        }
        Spacer(Modifier.height(30.dp))

        Text(
            text = "${i18n.appearanceFontSizeHeading()}:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = getNewFontSize(),
            onValueChange = { newFontSize = it },
            steps = 5,
            valueRange = 0.7f..1.3f
        )
        Spacer(Modifier.height(5.dp))

        Text(
            text = "${i18n.appearanceDisplaySizeHeading()}:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = getNewDisplaySize(),
            onValueChange = { newDisplaySize = it },
            valueRange = 0.5f..1.5f,
            steps = 3
        )

        Spacer(Modifier.height(10.dp))
        Button({
            appearanceSettingsViewModel.setDisplaySize(newDisplaySize)
            appearanceSettingsViewModel.setFontSize(newFontSize)
        }) {
            /*Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )*/
            Text(i18n.appearanceSizesApply())
        }

        Button({
            appearanceSettingsViewModel.setDisplaySize(defaultSizes.displaySize)
        }) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(i18n.appearanceSizesReset())
        }
    }
}
