package de.connect2x.messenger.compose.view.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun InfoPopup(
    isOpen: Boolean,
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    readers: List<String>,
    modifier: Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    if (isOpen) {
        ModalBottomSheet(onDismiss, modifier, sheetState) {
            Info(readers, focusRequester)
        }
    }
}
