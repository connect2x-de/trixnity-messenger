package de.connect2x.messenger.compose.view.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import de.connect2x.messenger.compose.view.common.deriveFromHue

// Light theme template (mostly used for lightness/saturation)
internal val md_theme_light_onPrimary = Color(0xFF040404)
internal val md_theme_light_primaryContainer = Color(0xfffa9f76)
internal val md_theme_light_onPrimaryContainer = Color(0xFF212121)
internal val md_theme_light_secondary = Color(0xFFE0E8FF)
internal val md_theme_light_onSecondary = Color(0xFF101010)
internal val md_theme_light_secondaryContainer = Color(0xFF666666)
internal val md_theme_light_onSecondaryContainer = Color(0xFFE6E6E6)
internal val md_theme_light_tertiary = Color(0xFF333333)
internal val md_theme_light_onTertiary = Color(0xFFFFFFFF)
internal val md_theme_light_tertiaryContainer = Color(0xFF888888)
internal val md_theme_light_onTertiaryContainer = Color(0xFFFEFEFE)
internal val md_theme_light_error = Color(0xFF8A0D1D)
internal val md_theme_light_errorContainer = Color(0xFFFFDAD6)
internal val md_theme_light_onError = Color(0xFFFFFFFF)
internal val md_theme_light_onErrorContainer = Color(0xFF410002)
internal val md_theme_light_background = Color(0xFFFFFFFF)
internal val md_theme_light_onBackground = Color(0xFF000000)
internal val md_theme_light_surface = Color(0xFFFCFEFE)
internal val md_theme_light_onSurface = Color(0xFF121212)
internal val md_theme_light_surfaceVariant = Color(0xFFdcf1fc)
internal val md_theme_light_onSurfaceVariant = Color(0xFF222222)
internal val md_theme_light_outline = Color(0xFF002336)
internal val md_theme_light_inverseOnSurface = Color(0xFFD6F6FF)
internal val md_theme_light_inverseSurface = Color(0xFF00363F)
internal val md_theme_light_inversePrimary = Color(0xFF5BD5F9)   // FIXME
internal val md_theme_light_surfaceTint = Color(0xFF000000)      // FIXME
internal val md_theme_light_outlineVariant = Color(0xFFBFC8CC)   // FIXME
internal val md_theme_light_scrim = Color(0xFF000000)            // FIXME

// Dark theme template (mostly used for lightness/saturation)
internal val md_theme_dark_onPrimary = Color(0xFF040404)
internal val md_theme_dark_primaryContainer = Color(0xff5e0000)
internal val md_theme_dark_onPrimaryContainer = Color(0xFFEEEEEE)
internal val md_theme_dark_secondary = Color(0xFF384144)
internal val md_theme_dark_onSecondary = Color(0xFFFFFFFF)
internal val md_theme_dark_secondaryContainer = Color(0xFF666666)
internal val md_theme_dark_onSecondaryContainer = Color(0xFFE6E6E6)
internal val md_theme_dark_tertiary = Color(0xFFE6E6E6)
internal val md_theme_dark_onTertiary = Color(0xFF101010)
internal val md_theme_dark_tertiaryContainer = Color(0xFF555555)
internal val md_theme_dark_onTertiaryContainer = Color(0xFFE6E6E6)
// TODO: use https://material-foundation.github.io/material-theme-builder/
internal val md_theme_dark_error = Color(0x66FFFFFF).compositeOver(md_theme_light_error)
internal val md_theme_dark_errorContainer = Color(0xFFFFDAD6)
internal val md_theme_dark_onError = Color(0xFFFFFFFF)
internal val md_theme_dark_onErrorContainer = Color(0xFF410002)
internal val md_theme_dark_background = Color(0xFF121212)
internal val md_theme_dark_onBackground = Color(0xFFFFFFFF)
internal val md_theme_dark_surface = Color(0xFF040404)
internal val md_theme_dark_onSurface = Color(0xFFFFFFFF)
internal val md_theme_dark_surfaceVariant = Color(0xFF274d61)
internal val md_theme_dark_onSurfaceVariant = Color(0xFFFFFFFF)
internal val md_theme_dark_outline = Color(0xFF8ed5fa)
internal val md_theme_dark_inverseOnSurface = Color(0xFF161616)
internal val md_theme_dark_inverseSurface = Color(0xFFA6EEFF)    // FIXME
internal val md_theme_dark_inversePrimary = Color(0xFF00677E)    // FIXME
internal val md_theme_dark_surfaceTint = Color(0xFF5BD5F9)       // FIXME
internal val md_theme_dark_outlineVariant = Color(0xFF40484B)    // FIXME
internal val md_theme_dark_scrim = Color(0xFF101010)

class DefaultMessengerLightColors internal constructor(accentHue: Float) : MessengerColors {
    companion object {
        private val userColors: List<Color> by lazy {
            listOf(
                Color(0xFFA41515),
                Color(0xFFA46715),
                Color(0xFF908213),
                Color(0xFF139057),
                Color(0xFF138190),
                Color(0xFF135F90),
                Color(0xFF132F90),
                Color(0xFF3F1390),
                Color(0xFF6E1390),
                Color(0xFF901355)
            )
        }
    }

    override val success: Color = Color(0xFF519325)
    override val presenceOnline: Color = Color(0xFF519325)
    override val presenceOffline: Color = Color(0xFF777777)
    override val presenceUnavailable: Color = Color(0xFFFB5607)
    override val verificationTrusted: Color = Color(0xFF519325)
    override val verificationUntrusted: Color = Color(0xFFFB5607)
    override val verificationNeutral: Color = Color(0xFF777777)
    override val metaDataPreview: Color = Color.White.copy(alpha = 0.7f)
    override val metaDataPreviewBackground: Color = Color.Black.copy(alpha = 0.7f)
    override val blockedUser: Color = Color(0xFFFB5607)
    override val warning: Color = Color(0xFFeedc82)
    override val link: Color = Color(0xFF004066).deriveFromHue(accentHue)
    override val linkByMe: Color = Color(0xFFb8e4ff).deriveFromHue(accentHue)
    override val userColors: List<Color> = Companion.userColors
}

class DefaultMessengerDarkColors internal constructor(accentHue: Float) : MessengerColors {
    companion object {
        private val userColors: List<Color> by lazy {
            listOf(
                Color(0xFFe61c1c),
                Color(0xFFdb8818),
                Color(0xFFd6c11c),
                Color(0xFF1cd480),
                Color(0xFF18b6cc),
                Color(0xFF1b8ad1),
                Color(0xFF4767d6),
                Color(0xFF753cde),
                Color(0xFFa424d4),
                Color(0xFFe61e87)
            )
        }
    }

    override val success: Color = Color(0xFF51CC25)
    override val presenceOnline: Color = Color(0xFF51CC25)
    override val presenceOffline: Color = Color(0xFF777777)
    override val presenceUnavailable: Color = Color(0xFFFF5607)
    override val verificationTrusted: Color = Color(0xFF51CC25)
    override val verificationUntrusted: Color = Color(0xFFFB5607)
    override val verificationNeutral: Color = Color(0xFF777777)
    override val metaDataPreview: Color = Color.White.copy(alpha = 0.7f)
    override val metaDataPreviewBackground: Color = Color.Black.copy(alpha = 0.7f)
    override val blockedUser: Color = Color(0xFFFB5607)
    override val warning: Color = Color(0xFFeedc82)
    override val link: Color = Color(0xFFb8e4ff).deriveFromHue(accentHue)
    override val linkByMe: Color = Color(0xFF004066).deriveFromHue(accentHue)
    override val userColors: List<Color> = Companion.userColors
}
