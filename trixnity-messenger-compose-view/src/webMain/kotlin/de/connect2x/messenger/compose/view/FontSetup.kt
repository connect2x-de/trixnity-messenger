package de.connect2x.messenger.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import de.connect2x.trixnity.messenger.trixnity_messenger_compose_view.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun PreloadEmojis() {
    val fontMgr = LocalFontFamilyResolver.current

    LaunchedEffect(fontMgr) {
        // We have to use Res.readBytes to read the font verbatim instead of already converting it via Res.font methods
        // The ttf file is places into composeResources/files do avoid accidentally loading it via the font apis.
        val bytes = Res.readBytes("files/NotoColorEmoji.ttf")

        // Somehow using androidx.compose.ui.text.platform.Font with bytes and manually creating the font works
        // org.jetbrains.compose.resources.Font does not
        val fontFamily = FontFamily(listOf(Font("NotoColorEmoji", bytes)))

        fontMgr.preload(fontFamily)
    }
}
