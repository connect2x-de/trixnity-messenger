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
        val i18n = DI.get<I18nView>()
        val imprint = DI.get<MatrixMessengerConfiguration>().imprint
        if (imprint != null) {
            ThemedAdaptiveDialog({ appInfoViewModel.showImprint.value = false }) {
                AdaptiveDialogHeader(onClose = { appInfoViewModel.showImprint.value = false }) {
                    Text(i18n.appInfoImprint())
                }
                AdaptiveDialogScrollContent {
                    RichTextDisplay(imprint)
                }
            }
        }
    }
}
