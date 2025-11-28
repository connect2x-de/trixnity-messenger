package de.connect2x.messenger.compose.view.room.timeline.element.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedHorizontalDivider
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.RoomUpgradedStateTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

interface RoomUpgradedStateTimelineElementView : TimelineElementView<RoomUpgradedStateTimelineElementViewModel>


class RoomUpgradedStateTimelineElementViewImpl : RoomUpgradedStateTimelineElementView {
    override val supports: KClass<RoomUpgradedStateTimelineElementViewModel> = RoomUpgradedStateTimelineElementViewModel::class

    override suspend fun waitFor(element: RoomUpgradedStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomUpgradedStateTimelineElementViewModel,
        index: Int,
    ) {
        val message = element.changeMessage.collectAsState().value
        StateElement(message)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: RoomUpgradedStateTimelineElementViewModel,
        index: Int,
    ) {
        val message = element.changeMessage.collectAsState().value
        StateElement(message)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: RoomUpgradedStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource
    ) {
        val message = element.changeMessage.collectAsState().value
        StateElement(message)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: RoomUpgradedStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource
    ) {
        val message = element.changeMessage.collectAsState().value
        StateElement(message)
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomUpgradedStateTimelineElementViewModel
    ): ClipEntry? = null
}

@Composable
private fun StateElement(message: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ThemedHorizontalDivider(Modifier.weight(1f), MaterialTheme.components.horizontalDivider)
        Text(message)
        ThemedHorizontalDivider(Modifier.weight(1f), MaterialTheme.components.horizontalDivider)
    }
}


