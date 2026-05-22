package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModel

interface SendAttachmentTitleView {
    @Composable fun create(sendAttachmentViewModel: SendAttachmentViewModel)
}

@Composable
fun SendAttachmentTitle(sendAttachmentViewModel: SendAttachmentViewModel) {
    with(DI.get<SendAttachmentTitleView>()) { create(sendAttachmentViewModel) }
}

class SendAttachmentTitleViewImpl : SendAttachmentTitleView {
    @Composable
    override fun create(sendAttachmentViewModel: SendAttachmentViewModel) {
        val i18n = DI.get<I18nView>()
        val sendEnabled = sendAttachmentViewModel.sendEnabled.collectAsState().value
        val errorHappened = sendAttachmentViewModel.error.collectAsState().value != null

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                i18n.sendAttachmentTitle(),
                modifier = Modifier.padding(start = 20.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.fillMaxWidth().weight(1.0f, false))
            Tooltip(tooltip = { Text(i18n.commonCancel()) }) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { sendAttachmentViewModel.cancel() },
                    modifier =
                        Modifier.padding(
                            8.dp
                        ), // 24.dp Icon + 24.dp Button = 12.dp padding on each side; 20.dp - 12.dp = 8.dp
                    enabled = sendEnabled || errorHappened,
                ) {
                    Icon(Icons.Outlined.Cancel, i18n.commonCancel())
                }
            }
        }
        HorizontalDivider(Modifier.fillMaxWidth())
        Spacer(Modifier.size(20.dp))
    }
}
