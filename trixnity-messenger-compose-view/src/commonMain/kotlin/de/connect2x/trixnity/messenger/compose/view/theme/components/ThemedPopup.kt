package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun ThemedPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable (() -> Unit)
) {
    val localDensity = LocalDensity.current
    val localToolbar = LocalTextToolbar.current
    Popup(alignment, offset, onDismissRequest, properties) {
        CompositionLocalProvider(LocalDensity provides localDensity) {
            CompositionLocalProvider(LocalTextToolbar provides localToolbar) {
                content()
            }
        }
    }
}
