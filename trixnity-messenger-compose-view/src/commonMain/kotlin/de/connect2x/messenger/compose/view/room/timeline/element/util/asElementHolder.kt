package de.connect2x.messenger.compose.view.room.timeline.element.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel


@Composable
internal fun BaseTimelineElementHolderViewModel.asOutboxElementHolder(): OutboxElementHolderViewModel? =
    remember(this) {
        this as? OutboxElementHolderViewModel
    }

@Composable
internal fun BaseTimelineElementHolderViewModel.asTimelineElementHolder(): TimelineElementHolderViewModel? =
    remember(this) {
        this as? TimelineElementHolderViewModel
    }
