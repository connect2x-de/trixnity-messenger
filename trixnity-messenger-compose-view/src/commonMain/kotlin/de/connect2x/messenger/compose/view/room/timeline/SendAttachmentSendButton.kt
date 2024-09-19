package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModel


interface SendAttachmentSendButtonView {
    @Composable
    fun create(sendAttachmentViewModel: SendAttachmentViewModel)
}

@Composable
fun SendAttachmentSendButton(sendAttachmentViewModel: SendAttachmentViewModel) {
    with(DI.get<SendAttachmentSendButtonView>()) { create(sendAttachmentViewModel) }
}

class SendAttachmentSendButtonViewImpl : SendAttachmentSendButtonView {
    @Composable
    override fun create(sendAttachmentViewModel: SendAttachmentViewModel) {
        val i18n = DI.get<I18nView>()
        val sendEnabled = sendAttachmentViewModel.sendEnabled.collectAsState().value
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Spacer(Modifier.fillMaxWidth().weight(1.0f, false))
            Button(
                onClick = { sendAttachmentViewModel.send() },
                modifier = Modifier
                    .size(40.dp)
                    .buttonPointerModifier(),
                shape = CircleShape,
                contentPadding = PaddingValues(start = 2.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
                enabled = sendEnabled
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    i18n.inputAreaSend(),
                )
            }
        }
    }
}
