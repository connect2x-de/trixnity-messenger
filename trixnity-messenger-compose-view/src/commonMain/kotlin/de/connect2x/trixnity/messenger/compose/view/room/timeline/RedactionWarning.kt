package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

interface RedactionWarningView {
    @Composable fun create(holder: BaseTimelineElementHolderViewModel)
}

@Composable
fun RedactionWarning(holder: BaseTimelineElementHolderViewModel) {
    DI.get<RedactionWarningView>().create(holder)
}

class RedactionWarningViewImpl : RedactionWarningView {
    @Composable
    override fun create(holder: BaseTimelineElementHolderViewModel) {
        if (holder !is TimelineElementHolderViewModel) {
            return
        }
        val i18n = DI.get<I18nView>()
        ThemedModalDialog(onDismissRequest = { holder.cancelRedactionWarning() }) {
            ModalDialogHeader { Text(i18n.redactionWarningInfoTitle()) }
            ModalDialogContent {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, i18n.commonWarning(), tint = MaterialTheme.messengerColors.warning)
                    MiddleSpacer()
                    Text(i18n.redactionWarningInfo())
                }
            }
            ModalDialogFooter {
                ThemedButton(
                    onClick = { holder.cancelRedactionWarning() },
                    style = MaterialTheme.components.commonButton,
                ) {
                    Text(i18n.commonCancel())
                }
                ThemedButton(
                    onClick = { holder.acceptRedactionWarning() },
                    style = MaterialTheme.components.primaryButton,
                ) {
                    Text(i18n.commonConfirm())
                }
            }
        }
    }
}
