package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass


interface TimelineElementDetailsView<V : TimelineElementViewModel<*>> {
    val supports: KClass<V>
    val supportedMimeTypes: List<String>
    val supportsAllMimeTypes : Boolean
        get() = false

    @Composable
    fun create(element: V, onSave: () -> Unit, onClose: () -> Unit)
}
