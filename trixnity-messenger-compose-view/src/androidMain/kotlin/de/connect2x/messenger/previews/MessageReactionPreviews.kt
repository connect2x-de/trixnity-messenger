package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import kotlinx.datetime.Instant
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.UserId
import de.connect2x.messenger.compose.view.room.timeline.element.MessageReactionButton

private fun previewReactionEvent(sender: String, isMe: Boolean = false) =
    TimelineElementHolderViewModel.ReactionEvent(
        eventId = EventId(""),
        sender = UserInfoElement(
            name = sender,
            userId = UserId(""),
            initials = Initials.compute(sender),
            image = null,
        ),
        isMe = isMe,
        timestamp = Instant.fromEpochMilliseconds(0),
    )


@Preview
@Composable
fun MessageReactionPreview1() {
    MessageReactionButton(
        reaction = "\uD83D\uDC4D",
        count = 3,
        myReaction = null,
        onAddReaction = { },
        onRemoveReaction = { },
    )
}

@Preview
@Composable
fun MessageReactionPreview2() {
    MessageReactionButton(
        reaction = "\uD83D\uDC4D",
        count = 2,
        myReaction = previewReactionEvent("username", isMe = false),
        onAddReaction = { },
        onRemoveReaction = { },
    )
}
