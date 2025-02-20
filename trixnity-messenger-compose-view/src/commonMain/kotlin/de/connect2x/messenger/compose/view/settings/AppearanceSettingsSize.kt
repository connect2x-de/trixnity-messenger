package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
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
import de.connect2x.messenger.compose.view.theme.SystemDensity
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel1
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel2
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel
import kotlin.math.round

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
        val applySystemSizes by appearanceSettingsViewModel.applySystemSizes.collectAsState()

        // Font size
        val fontSize = appearanceSettingsViewModel.fontSize.collectAsState().value ?: defaultSizes.fontSize
        var newFontSize by remember { mutableStateOf(-1F) }
        fun getNewFontSize(): Float = if (newFontSize != -1F && newFontSize != fontSize) newFontSize else fontSize

        // Display size
        val displaySize = appearanceSettingsViewModel.displaySize.collectAsState().value ?: defaultSizes.displaySize
        var newDisplaySize by remember { mutableStateOf(-1F) }
        fun getNewDisplaySize(): Float = if (newDisplaySize != -1F && newDisplaySize != displaySize)
            newDisplaySize else displaySize

        // Preview
        val systemDensity = SystemDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                systemDensity.density * getNewDisplaySize(),
                systemDensity.fontScale * getNewFontSize()
            )
        ) {
            Column(Modifier.padding(end = 10.dp).fillMaxWidth(1.0f).aspectRatio(1.0f)) {
                TimelineElementHolder(PreviewTimelineElementViewModel1())
                TimelineElementHolder(PreviewTimelineElementViewModel2())
            }
        }
        Spacer(Modifier.height(30.dp))
        HorizontalDivider()

        // Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = applySystemSizes,
                onCheckedChange = {
                    appearanceSettingsViewModel.toggleApplySystemSizes()
                    newFontSize = -1F
                    newDisplaySize = -1F
                    appearanceSettingsViewModel.setDisplaySize(defaultSizes.displaySize)
                    appearanceSettingsViewModel.setFontSize(defaultSizes.fontSize)
                }
            )
            Text(i18n.appearanceSizesApplySystemHeading())
        }

        Column(Modifier.padding(16.dp).fillMaxSize()) {
            Row(Modifier.padding(2.dp)) {
                Text(
                    text = "${i18n.appearanceFontSizeHeading()}:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.weight(1.0f))
                Text(
                    text = "${round(getNewFontSize() * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Slider(
                value = getNewFontSize(),
                onValueChange = { newFontSize = it },
                steps = 3,
                valueRange = defaultSizes.minFontSize..defaultSizes.maxFontSize,
                enabled = !applySystemSizes
            )
            Spacer(Modifier.height(5.dp))

            Row(Modifier.padding(2.dp)) {
                Text(
                    text = "${i18n.appearanceDisplaySizeHeading()}:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.weight(1.0f))
                Text(
                    text = "${round(getNewDisplaySize() * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Slider(
                value = getNewDisplaySize(),
                onValueChange = { newDisplaySize = it },
                valueRange = defaultSizes.minDisplaySize..defaultSizes.maxDisplaySize,
                steps = 5,
                enabled = !applySystemSizes
            )

            Spacer(Modifier.height(10.dp))

            Button(
                enabled = !applySystemSizes,
                onClick = {
                    appearanceSettingsViewModel.setDisplaySize(getNewDisplaySize())
                    appearanceSettingsViewModel.setFontSize(getNewFontSize())
                }
            ) {
                Text(i18n.appearanceSizesApply())
            }
        }
    }
}
