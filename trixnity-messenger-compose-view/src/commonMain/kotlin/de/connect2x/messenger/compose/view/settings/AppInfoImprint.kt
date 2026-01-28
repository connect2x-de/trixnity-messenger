package de.connect2x.messenger.compose.view.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.richtext.RichTextColors
import de.connect2x.messenger.compose.view.richtext.RichTextDisplay
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.messenger.compose.view.theme.components.ThemedAdaptiveDialog
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.util.html.AutoLinkifyVisitor
import de.connect2x.trixnity.messenger.util.html.HtmlVisitor
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModel

interface AppInfoImprintView {
    @Composable
    fun create(appInfoViewModel: AppInfoViewModel)
}

@Composable
fun AppInfoImprint(appInfoViewModel: AppInfoViewModel) {
    DI.get<AppInfoImprintView>().create(appInfoViewModel)
}

class AppInfoImprintViewImpl : AppInfoImprintView {
    @Composable
    override fun create(appInfoViewModel: AppInfoViewModel) {
        Imprint { appInfoViewModel.showImprint.value = false }
    }
}

@Composable
internal fun Imprint(onClose: () -> Unit) {
    val i18n = DI.get<I18nView>()
    val imprint = DI.get<MatrixMessengerBaseConfiguration>().imprint ?: return
    val uriHandler = LocalUriHandler.current
    val content = AutoLinkifyVisitor.process(HtmlVisitor.process(imprint))

    ThemedAdaptiveDialog(onClose) {
        AdaptiveDialogHeader(onClose = onClose) {
            Text(i18n.appInfoImprint())
        }
        AdaptiveDialogScrollContent {
            RichTextDisplay(
                content,
                colors = RichTextColors.default(linkColor = MaterialTheme.messengerColors.link),
                onLinkClick = { uriHandler.openUri(it) }
            )
        }
    }
}
