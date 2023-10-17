package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import kotlinx.coroutines.flow.StateFlow

sealed interface BaseTimelineElementHolderViewModel {
    val key: String
    val timelineElementViewModel: StateFlow<BaseTimelineElementViewModel?>
}

operator fun List<BaseTimelineElementHolderViewModel>.get(key: String) = firstOrNull { it.key == key }