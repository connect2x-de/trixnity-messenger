package de.connect2x.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.common.deriveFromHue
import de.connect2x.messenger.compose.view.common.hue

interface ThemeDarkMessengerColors {
    @Composable
    @Stable
    fun create(accentColor: Color): MessengerColors
}

class ThemeDarkMessengerColorsImpl : ThemeDarkMessengerColors {
    @Composable
    @Stable
    override fun create(accentColor: Color): MessengerColors {
        val accentHue = accentColor.hue
        return MessengerColors(
            success = messenger_theme_dark_success,
            presenceOnline = messenger_theme_dark_success,
            presenceUnavailable = messenger_theme_dark_warning,
            presenceOffline = messenger_theme_dark_neutral,
            verificationTrusted = messenger_theme_dark_success,
            verificationUntrusted = messenger_theme_dark_error,
            verificationNeutral = messenger_theme_dark_neutral,
            metaDataPreview = messenger_theme_dark_preview_content,
            metaDataPreviewBackground = messenger_theme_dark_preview_scrim,
            blockedUser = messenger_theme_dark_error,
            warning = messenger_theme_dark_warning,
            link = messenger_theme_dark_link_other.deriveFromHue(accentHue),
            linkByMe = messenger_theme_dark_link_own.deriveFromHue(accentHue),
            userColors = messenger_theme_dark_users
        )
    }
}
