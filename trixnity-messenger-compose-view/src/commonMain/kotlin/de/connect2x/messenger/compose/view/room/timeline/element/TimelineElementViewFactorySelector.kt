package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel

interface TimelineElementViewFactorySelector<T> {
    @Composable
    fun rememberFactory(element: TimelineElementViewModel<*>) : T =
        remember(element) { selectFactory(element) }

    fun selectFactory(element: TimelineElementViewModel<*>) : T
}
