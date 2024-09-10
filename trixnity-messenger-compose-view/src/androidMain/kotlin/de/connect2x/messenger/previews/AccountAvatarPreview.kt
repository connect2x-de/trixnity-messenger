package de.connect2x.messenger.previews

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.roomlist.header.AccountAvatar
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewAccountViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun AccountAvatarPreview() {
    InitMessengerPreview {
        Row(Modifier.sizeIn(maxWidth = 300.dp)) {
            AccountAvatar(accountViewModel = PreviewAccountViewModel())
        }
    }
}
