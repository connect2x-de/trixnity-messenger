package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased.Text
import kotlin.reflect.KClass

interface TextRoomMessageTimelineElementView : TimelineElementView<Text>

class TextRoomMessageTimelineElementViewImpl : TextRoomMessageTimelineElementView {
    override val supports: KClass<Text> = Text::class

    override suspend fun waitFor(element: Text) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: Text, index: Int) {
        TextBasedRoomMessageTimelineElementView(holder, element, isPreview = false, index = index)
    }

    @Composable
    override fun createAsPreview(holder: TimelineElementHolderViewModel, element: Text, index: Int) {
        TextBasedRoomMessageTimelineElementView(holder, element, isPreview = true, index = index)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Text,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        TextReplyInTimeline(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Text,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        TextReplyInSendMessage(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(holder: BaseTimelineElementHolderViewModel, element: Text): ClipEntry? =
        element.toClipEntry()

    override fun a11yLabel(element: Text, i18n: I18nView): String {
        return element.body
    }
}
