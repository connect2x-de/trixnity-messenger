package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.metadata

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

interface TimelineElementMetadataSenderListItemView : TimelineElementMetadataListItemView

class TimelineElementMetadataSenderListItemViewImpl : TimelineElementMetadataSenderListItemView {
    @Composable
    override fun ColumnScope.create(
        viewModel: TimelineElementMetadataViewModel,
        elementHistory: List<TimelineElementHolderViewModel>,
        firstElement: TimelineElementHolderViewModel?,
        lastElement: TimelineElementHolderViewModel?,
        scrollState: ScrollState,
        isBottomOfStack: Boolean,
        isSinglePane: Boolean,
    ) {
        val i18n = DI.get<I18nView>()
        val sender = lastElement?.sender?.collectAsState()?.value

        if (sender == null) {
            LoadingSpinner()
        } else {
            SubHeading(i18n.timelineElementMetadataSender())
            UserInfo(sender, onOpenUserProfile = viewModel::openUserProfile)
        }
    }
}
