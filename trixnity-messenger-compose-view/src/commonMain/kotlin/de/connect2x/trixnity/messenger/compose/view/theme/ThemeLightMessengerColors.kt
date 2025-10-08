package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import de.connect2x.trixnity.messenger.compose.view.common.deriveFromHue
import de.connect2x.trixnity.messenger.compose.view.common.hue

interface ThemeLightMessengerColors {
    @Composable
    @Stable
    fun create(accentColor: Color): MessengerColors
}

class ThemeLightMessengerColorsImpl : ThemeLightMessengerColors {
    @Composable
    @Stable
    override fun create(accentColor: Color): MessengerColors {
        val accentHue = accentColor.hue
        return MessengerColors(
            success = messenger_theme_light_success,
            presenceOnline = messenger_theme_light_success,
            presenceUnavailable = messenger_theme_light_warning,
            presenceOffline = messenger_theme_light_neutral,
            verificationTrusted = messenger_theme_light_success,
            verificationUntrusted = messenger_theme_light_error,
            verificationNeutral = messenger_theme_light_neutral,
            metaDataPreview = messenger_theme_light_preview_content,
            metaDataPreviewBackground = messenger_theme_light_preview_scrim,
            blockedUser = messenger_theme_light_error,
            warning = messenger_theme_light_warning,
            link = messenger_theme_light_link_other.deriveFromHue(accentHue),
            linkByMe = messenger_theme_light_link_own.deriveFromHue(accentHue),
            userColors = messenger_theme_light_users,
        )
    }
}
