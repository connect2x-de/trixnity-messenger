package de.connect2x.messenger.compose.view.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material.RichText
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalButtonRow
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.common.NextButton
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
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
        val imprintUrl = DI.get<MatrixMessengerConfiguration>().imprintUrl
        if (imprintUrl != null) {
            val richTextState = rememberRichTextState()
            LaunchedEffect(Unit) {
                richTextState.addLink(i18n.appInfoImprintLink(), imprintUrl)
            }
            MessengerModal(onDismiss = { appInfoViewModel.showImprint.value = false }, i18n.appInfoImprint()) {
                MessengerModalContent {
                    RichText(richTextState)
                }
                MessengerModalButtonRow({
                    NextButton(text = i18n.commonBack()) { appInfoViewModel.showImprint.value = false }
                })
            }
        }
    }
}
