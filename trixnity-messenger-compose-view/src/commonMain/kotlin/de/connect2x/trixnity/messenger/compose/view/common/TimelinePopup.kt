package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun TimelinePopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
    isByMe: Boolean,
    content: @Composable () -> Unit
)
