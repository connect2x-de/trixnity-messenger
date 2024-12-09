package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

interface ReadMarkerView {
    @Composable
    fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    )
}

@Composable
fun ReadMarker(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    DI.get<ReadMarkerView>().create(timelineElementHolderViewModel)
}

class ReadMarkerViewImpl : ReadMarkerView {
    @Composable
    override fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel
    ) {
        val i18n = DI.get<I18nView>()
        if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
            val isRead = timelineElementHolderViewModel.isRead.collectAsState().value == true
            val isByMe = timelineElementHolderViewModel.isByMe
            if (isByMe) {
                Box(
                    Modifier
                        .size(MaterialTheme.typography.labelSmall.dp)
                        .padding(start = 2.dp)
                ) {
                    if (isRead) {
                        Icon(
                            Icons.Filled.DoneAll,
                            i18n.messageBubbleRead(),
                            Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Filled.Done,
                            i18n.messageBubbleRead(),
                            Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else Box(Modifier.size(MaterialTheme.typography.labelSmall.dp).padding(start = 2.dp))
    }
}
