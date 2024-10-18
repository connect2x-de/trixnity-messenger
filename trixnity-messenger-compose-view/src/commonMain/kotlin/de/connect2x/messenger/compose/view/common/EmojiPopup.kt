package de.connect2x.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@Composable
fun EmojiPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isByMe: Boolean,
) {
    TimelinePopup(isOpen, onDismiss, modifier, isByMe) {
        EmojiSelector(onSelect, focusRequester)
    }
}
