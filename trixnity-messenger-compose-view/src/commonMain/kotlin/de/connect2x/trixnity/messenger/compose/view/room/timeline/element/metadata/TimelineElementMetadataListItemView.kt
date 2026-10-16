package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.metadata

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

interface TimelineElementMetadataListItemView {
    @Composable
    fun ColumnScope.create(
        viewModel: TimelineElementMetadataViewModel,
        elementHistory: List<TimelineElementHolderViewModel>,
        firstElement: TimelineElementHolderViewModel?,
        lastElement: TimelineElementHolderViewModel?,
        scrollState: ScrollState,
        BottomOfStack: Boolean,
        isSinglePane: Boolean,
    )
}
