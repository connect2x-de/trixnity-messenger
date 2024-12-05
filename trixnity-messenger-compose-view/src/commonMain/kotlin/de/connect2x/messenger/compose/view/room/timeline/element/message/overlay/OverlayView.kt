package de.connect2x.messenger.compose.view.room.timeline.element.message.overlay

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass

interface OverlayView<V : TimelineElementViewModel<*>> {
    val supports: KClass<V>
    val supportedMimeTypes: List<String>?

    @Composable
    fun create(element: TimelineElementViewModel<*>, onSave: () -> Unit, onClose: () -> Unit)
}
