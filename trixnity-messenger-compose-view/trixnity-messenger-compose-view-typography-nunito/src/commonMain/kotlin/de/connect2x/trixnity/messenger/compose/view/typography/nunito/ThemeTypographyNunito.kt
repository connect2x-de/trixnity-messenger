package de.connect2x.trixnity.messenger.compose.view.typography.nunito

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.theme.ThemeTypography
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import de.connect2x.trixnity.messenger.FontKind
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.compose.view.theme.withFontFamily
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_Black
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_BlackItalic
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_Bold
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_BoldItalic
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_ExtraBold
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_ExtraBoldItalic
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_ExtraLight
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_ExtraLightItalic
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_Italic
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_Light
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_LightItalic
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_Medium
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_MediumItalic
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_Regular
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_SemiBold
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Nunito_SemiBoldItalic
import org.jetbrains.compose.resources.Font
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view_typography_nunito.generated.resources.Res
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun MatrixMultiMessengerConfiguration.addNunitoThemeTypography() {
    enableBundledFont = true
    modulesFactories += ::nunitoThemeTypographyModule
    messengerConfiguration { addNunitoThemeTypography() }
}

fun MatrixMessengerConfiguration.addNunitoThemeTypography() {
    modulesFactories += ::nunitoThemeTypographyModule
}

fun nunitoThemeTypographyModule(): Module {
    return module {
        single<ThemeTypography>(named(FontKind.BUNDLED)) { ThemeTypographyNunito() }
    }
}

class ThemeTypographyNunito : ThemeTypography {

    @Composable
    override fun create(): Typography {
        return Typography().withFontFamily(
            FontFamily(
                fonts = listOf(
                    Font(Res.font.Nunito_Black, FontWeight.Black, FontStyle.Normal),
                    Font(Res.font.Nunito_BlackItalic, FontWeight.Black, FontStyle.Italic),

                    Font(Res.font.Nunito_Bold, FontWeight.Bold, FontStyle.Normal),
                    Font(Res.font.Nunito_BoldItalic, FontWeight.Bold, FontStyle.Italic),

                    Font(Res.font.Nunito_ExtraBold, FontWeight.Bold, FontStyle.Normal),
                    Font(Res.font.Nunito_ExtraBoldItalic, FontWeight.Bold, FontStyle.Italic),

                    Font(Res.font.Nunito_ExtraLight, FontWeight.ExtraLight, FontStyle.Normal),
                    Font(Res.font.Nunito_ExtraLightItalic, FontWeight.ExtraLight, FontStyle.Italic),

                    Font(Res.font.Nunito_Regular, FontWeight.Normal, FontStyle.Normal),
                    Font(Res.font.Nunito_Italic, FontWeight.Normal, FontStyle.Italic),

                    Font(Res.font.Nunito_Light, FontWeight.Light, FontStyle.Normal),
                    Font(Res.font.Nunito_LightItalic, FontWeight.Light, FontStyle.Italic),

                    Font(Res.font.Nunito_Medium, FontWeight.Medium, FontStyle.Normal),
                    Font(Res.font.Nunito_MediumItalic, FontWeight.Medium, FontStyle.Italic),

                    Font(Res.font.Nunito_SemiBold, FontWeight.SemiBold, FontStyle.Normal),
                    Font(Res.font.Nunito_SemiBoldItalic, FontWeight.SemiBold, FontStyle.Italic),
                )
            )
        )
    }
}
