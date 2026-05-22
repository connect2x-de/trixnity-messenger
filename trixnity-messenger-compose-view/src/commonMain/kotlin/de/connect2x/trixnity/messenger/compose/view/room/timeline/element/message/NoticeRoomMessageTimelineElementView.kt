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
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased.Notice
import kotlin.reflect.KClass

interface NoticeRoomMessageTimelineElementView : TimelineElementView<Notice>

class NoticeRoomMessageTimelineElementViewImpl : NoticeRoomMessageTimelineElementView {
    override val supports: KClass<Notice> = Notice::class

    override suspend fun waitFor(element: Notice) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(holder: BaseTimelineElementHolderViewModel, element: Notice, index: Int) {
        TextBasedRoomMessageTimelineElementView(holder, element, isPreview = false, index = index)
    }

    @Composable
    override fun createAsPreview(holder: TimelineElementHolderViewModel, element: Notice, index: Int) {
        TextBasedRoomMessageTimelineElementView(holder, element, isPreview = true, index = index)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Notice,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        TextReplyInTimeline(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Notice,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        TextReplyInSendMessage(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(holder: BaseTimelineElementHolderViewModel, element: Notice): ClipEntry? =
        element.toClipEntry()

    override fun a11yLabel(element: Notice, i18n: I18nView): String {
        return "${i18n.automated()}: ${element.body}"
    }
}
