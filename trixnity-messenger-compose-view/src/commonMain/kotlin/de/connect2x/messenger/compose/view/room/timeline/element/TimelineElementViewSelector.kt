package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedErrorTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedWaitTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

interface TimelineElementViewSelector { // FIXME DI
    @Composable
    fun create(holder: BaseTimelineElementHolderViewModel, element: TimelineElementViewModel<*>)
}

@Composable
fun TimelineElementSelector(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    element: TimelineElementViewModel<*>
) {
    with(DI.get<TimelineElementViewSelector>()) { create(timelineElementHolderViewModel, element) }
}

class TimelineElementSelectorImpl(private val factories: List<TimelineElementView<*>>) : TimelineElementViewSelector {
    private val factoryMapping =
        MutableStateFlow<Map<KClass<out TimelineElementViewModel<*>>, TimelineElementView<TimelineElementViewModel<*>>>>(
            emptyMap()
        )

    @Composable
    override fun create(
        holder: BaseTimelineElementHolderViewModel,
        element: TimelineElementViewModel<*>
    ) {
        when (element) {
            is EncryptedWaitTimelineElementViewModel -> {
                // FIXME
            }

            is EncryptedErrorTimelineElementViewModel -> {
                // FIXME
            }

            else -> {
                val timelineElementViewModelClass = element::class
                val factory = remember {
                    factoryMapping.value[timelineElementViewModelClass]
                        ?: run {
                            val foundFactory =
                                factories.firstOrNull { it.supports.isInstance(timelineElementViewModelClass) }
                            if (foundFactory == null) return@run null
                            @Suppress("UNCHECKED_CAST")
                            foundFactory as TimelineElementView<TimelineElementViewModel<*>>
                            factoryMapping.update { it + (timelineElementViewModelClass to foundFactory) }
                            foundFactory
                        }
                }
                factory?.create(holder, element)
            }
        }
    }
}
