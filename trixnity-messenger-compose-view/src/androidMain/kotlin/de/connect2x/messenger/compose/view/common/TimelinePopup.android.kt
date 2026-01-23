package de.connect2x.messenger.compose.view.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun TimelinePopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
    isByMe: Boolean,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    if (isOpen) {
        val density = LocalDensity.current
        ModalBottomSheet(onDismiss, modifier, sheetState) {
            CompositionLocalProvider(LocalDensity provides density) {
                content()
            }
        }
    }
}
