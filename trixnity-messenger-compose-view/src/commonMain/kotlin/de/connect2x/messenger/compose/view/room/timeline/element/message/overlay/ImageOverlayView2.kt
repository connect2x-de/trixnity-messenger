package de.connect2x.messenger.compose.view.room.timeline.element.message.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass

class ImageOverlayView2 : OverlayView<RoomMessageTimelineElementViewModel.FileBased.Image> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.FileBased.Image> =
        RoomMessageTimelineElementViewModel.FileBased.Image::class

    override val supportedMimeTypes: List<String>? = null

    @Composable
    override fun create(
        element: TimelineElementViewModel<*>,
        onSave: () -> Unit,
        onClose: () -> Unit,
    ) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Blue)) {
                Button(onClick = {
                    onClose()
                }) {
                    Text("Hello")
                }
            }
        }
    }
}
