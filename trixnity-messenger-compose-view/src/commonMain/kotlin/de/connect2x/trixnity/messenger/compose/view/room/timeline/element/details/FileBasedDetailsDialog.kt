package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun FileBasedDetailsDialog(
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onSave: () -> Unit,
    onClose: () -> Unit,
    additions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints {
            ThemedSurface(
                style = MaterialTheme.components.fileViewerSurface,
            ) {
                Column(Modifier.fillMaxSize()) {
                    FileBasedDetailsHeader(element, onSave, onClose, additions)
                    Box(Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    }
}
