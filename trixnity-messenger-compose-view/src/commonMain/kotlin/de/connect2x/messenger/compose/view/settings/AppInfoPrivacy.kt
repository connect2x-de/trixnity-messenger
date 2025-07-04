package de.connect2x.messenger.compose.view.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.richtext.RichTextDisplay
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogHeader
import de.connect2x.messenger.compose.view.theme.components.AdaptiveDialogScrollContent
import de.connect2x.messenger.compose.view.theme.components.ThemedAdaptiveDialog
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
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
        val i18n = DI.get<I18nView>()
        val privacyInfo = DI.get<MatrixMessengerConfiguration>().privacyInfo
        if (privacyInfo != null) {
            ThemedAdaptiveDialog({ appInfoViewModel.showPrivacy.value = false }) {
                AdaptiveDialogHeader(onClose = { appInfoViewModel.showPrivacy.value = false }) {
                    Text(i18n.appInfoPrivacy())
                }
                AdaptiveDialogScrollContent {
                    RichTextDisplay(privacyInfo)
                }
            }
        }
    }
}
