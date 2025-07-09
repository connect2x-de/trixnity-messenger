package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.roomlist.create.CreateNewChat
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewCreateNewChatViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun CreateNewChatPreview() {
    InitMessengerPreview {
        val createNewChatViewModel = PreviewCreateNewChatViewModel()
        createNewChatViewModel.error.value = "Error creating a room"
        createNewChatViewModel.errorDetails.value = "The users cannot be invited."
        CreateNewChat(createNewChatViewModel)
    }
}
