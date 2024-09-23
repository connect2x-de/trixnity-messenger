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
        val privacyInfoUrl = DI.get<MatrixMessengerConfiguration>().privacyInfoUrl
        if (privacyInfoUrl != null) {
            val richTextState = rememberRichTextState()
            LaunchedEffect(Unit) {
                richTextState.addLink(i18n.appInfoPrivacyLink(), privacyInfoUrl)
            }
            MessengerModal(onDismiss = { appInfoViewModel.showPrivacy.value = false }, i18n.appInfoPrivacy()) {
                MessengerModalContent {
                    RichText(richTextState)
                }
                MessengerModalButtonRow({
                    NextButton(text = i18n.commonBack()) { appInfoViewModel.showPrivacy.value = false }
                })
            }
        }
    }
}
