package de.connect2x.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.WizardImage
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle
import de.connect2x.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity_messenger_compose_view.generated.resources.Res
import de.connect2x.trixnity_messenger_compose_view.generated.resources.vault

@Composable
@Preview
fun CrossSigningBootstrapWizardPreview() {
    InitMessengerPreview {
        val settings = DI.get<MatrixMessengerSettingsHolder>()
        // Small hack for force-changing theme for preview
        LaunchedEffect(Unit) {
            settings.update { settings: MatrixMessengerSettingsBase ->
                settings.copy(themeMode = ThemeMode.DARK)
            }
        }
        val i18n = DI.get<I18nView>()
        ThemedSurface(style = SurfaceStyle.default()) {
            WizardImage(Res.drawable.vault, i18n.bootstrapVault(), 300.dp)
        }
    }
}
