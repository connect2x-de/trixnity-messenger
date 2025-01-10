package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun FileBasedDetailsDialog(
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onClose: () -> Unit,
    additions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                FileBasedDetailsHeader(element, onClose, additions)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    content()
                }
            }
        }
    }
}
