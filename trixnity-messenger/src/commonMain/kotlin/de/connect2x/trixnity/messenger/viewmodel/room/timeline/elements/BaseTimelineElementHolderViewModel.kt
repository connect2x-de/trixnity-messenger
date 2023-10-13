package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import kotlinx.coroutines.flow.StateFlow

sealed interface BaseTimelineElementHolderViewModel {
    val key: String
    val timelineElementViewModel: StateFlow<BaseTimelineElementViewModel?>
}