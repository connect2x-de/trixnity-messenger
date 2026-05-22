package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.details

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import io.ktor.http.*
import kotlin.reflect.KClass

interface TimelineElementDetailsView<V : TimelineElementViewModel<*>> {
    val supports: KClass<V>

    fun supportsMimeType(mimeType: ContentType): Boolean

    @Composable fun create(element: V, onSave: () -> Unit, onClose: () -> Unit)
}
