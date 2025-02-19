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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementHolder
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel2
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel3
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

        val fontSize by appearanceSettingsViewModel.fontSize.collectAsState()
        val controlsSize by appearanceSettingsViewModel.controlsSize.collectAsState()

        Column(Modifier.padding(end = 10.dp)) {
            TimelineElementHolder(PreviewTimelineElementViewModel2())
            TimelineElementHolder(PreviewTimelineElementViewModel3())
        }
        Spacer(Modifier.height(15.dp))
        Spacer(Modifier.height(15.dp))

        Text(
            text = "${i18n.appearanceFontSizeHeading()}:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = fontSize,
            onValueChange = { appearanceSettingsViewModel.setFontSize(it) },
            steps = 5,
            valueRange = 0.7f..1.3f
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "${i18n.appearanceControlsSizeHeading()}:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        /*Slider(
            value = controlsSize.toFloat(),
            onValueChange = { appearanceSettingsViewModel.setControlsSize(it.toInt()) },
            steps = 7,
            valueRange = 0f..7f
        )*/
        Spacer(Modifier.height(10.dp))
        Button({
            appearanceSettingsViewModel.setControlsSize(1f) // TODO: Change
            appearanceSettingsViewModel.setFontSize(1f) // TODO: Change
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
