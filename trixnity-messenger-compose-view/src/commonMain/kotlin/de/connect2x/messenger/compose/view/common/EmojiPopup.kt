package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EmojiPopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isByMe: Boolean,
) {
    TimelinePopup(isOpen, onDismiss, modifier, isByMe) {
        EmojiSelector(onTextAdded = onSelect, onDismiss = onDismiss)
    }
}
