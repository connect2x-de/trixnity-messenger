package de.connect2x.messenger.compose.view.room.timeline.element.message.details

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.reflect.KClass

interface ElementDetailsView<V : TimelineElementViewModel<*>> {
    val supports: KClass<V>
    val supportedMimeTypes: List<String>

    @Composable
    fun create(element: V, onSave: () -> Unit, onClose: () -> Unit)
}
