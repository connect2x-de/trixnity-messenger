package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
actual fun TimelinePopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
    isByMe: Boolean,
    content: @Composable () -> Unit
) {
    if (isOpen) {
        Popup(
            onDismissRequest = onDismiss,
            alignment = if (isByMe) Alignment.BottomEnd else Alignment.BottomStart,
            properties = PopupProperties(
                focusable = true,
            ),
        ) {
            Surface(
                Modifier.size(320.dp, 240.dp),
                shadowElevation = 4.dp,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                content()
            }
        }
    }
}
