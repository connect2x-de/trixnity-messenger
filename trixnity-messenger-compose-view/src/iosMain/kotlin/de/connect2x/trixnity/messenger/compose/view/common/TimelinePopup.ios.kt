package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun TimelinePopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
    isByMe: Boolean,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    if (isOpen) {
        ModalBottomSheet(onDismiss, modifier, sheetState) { content() }
    }
}
