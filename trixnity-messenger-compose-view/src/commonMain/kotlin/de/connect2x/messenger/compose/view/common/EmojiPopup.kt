package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.common.modifier.customKeyNavigation

@Composable
fun EmojiPopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isByMe: Boolean,
) {
    TimelinePopup(isOpen, onDismiss, modifier, isByMe) {
        EmojiSelector(Modifier.fillMaxSize().customKeyNavigation(), onSelect)
    }
}
