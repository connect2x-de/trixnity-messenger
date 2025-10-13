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

interface AppInfoPrivacyView {
    @Composable
    fun create(appInfoViewModel: AppInfoViewModel)
}

@Composable
fun AppInfoPrivacy(appInfoViewModel: AppInfoViewModel) {
    DI.get<AppInfoPrivacyView>().create(appInfoViewModel)
}

class AppInfoPrivacyViewImpl : AppInfoPrivacyView {
    @Composable
    override fun create(appInfoViewModel: AppInfoViewModel) {
        Privacy { appInfoViewModel.showPrivacy.value = false }
    }
}

@Composable
internal fun Privacy(onClose: () -> Unit) {
    val i18n = DI.get<I18nView>()
    val uriHandler = LocalUriHandler.current
    val privacyInfo = DI.get<MatrixMessengerBaseConfiguration>().privacyInfo ?: return
    val content = AutoLinkifyVisitor.process(HtmlVisitor.process(privacyInfo))

    ThemedAdaptiveDialog(onClose) {
        AdaptiveDialogHeader(onClose = onClose) {
            Text(i18n.appInfoPrivacy())
        }
        AdaptiveDialogScrollContent {
            RichTextDisplay(
                content,
                colors = RichTextColors.default(linkColor = MaterialTheme.messengerColors.link),
                onLinkClick = { uriHandler.openUri(it) },
            )
        }
    }
} 
