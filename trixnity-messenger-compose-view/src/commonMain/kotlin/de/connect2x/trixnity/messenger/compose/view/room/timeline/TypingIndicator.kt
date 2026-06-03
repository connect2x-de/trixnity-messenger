package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.TypingIndicator
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface TypingIndicatorView {
    @Composable fun create(timelineViewModel: TimelineViewModel)
}

@Composable
fun TypingIndicator(timelineViewModel: TimelineViewModel) {
    with(DI.get<TypingIndicatorView>()) { create(timelineViewModel) }
}

class TypingIndicatorViewImpl : TypingIndicatorView {
    @Composable
    override fun create(timelineViewModel: TimelineViewModel) {
        val typing = timelineViewModel.roomHeaderViewModel.usersTyping.collectAsState().value != null
        Box(Modifier.focusProperties { canFocus = false }) {
            AnimatedVisibility(
                visible = typing,
                enter = fadeIn(animationSpec = tween(150)) + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(shrinkTowards = Alignment.Bottom),
            ) {
                MessageBubble(
                    holder = NoopHolder,
                    needsMaxWidth = false,
                    isPreview = true, // removes the action menu
                    isCurrentUserMentioned = false,
                    index = -1,
                ) {
                    val style = MaterialTheme.typography.titleLarge
                    TypingIndicator(
                        "",
                        Modifier.padding(start = 10.dp).minSizeOfText("...", style),
                        style,
                        cycleDuration = 1_500,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.minSizeOfText(text: String, style: TextStyle): Modifier {
    val density = LocalDensity.current
    val measured = rememberTextMeasurer().measure(AnnotatedString(text), style)
    return this.sizeIn(
        minWidth = with(density) { measured.size.width.toDp() },
        minHeight = with(density) { measured.size.height.toDp() },
    )
}

private object NoopHolder : OutboxElementHolderViewModel {
    override val key: String = "de.connect2x.trixnity.messenger.compose.view.room.timeline.TypingIndicator"
    override val element: StateFlow<TimelineElementViewModel<*>?> = MutableStateFlow(null)
    override val isReply: StateFlow<Boolean?> = MutableStateFlow(false)
    override val repliedElement: StateFlow<TimelineElementHolderViewModel?> = MutableStateFlow(null)
    override val isFirstInUserSequence: StateFlow<Boolean?> = MutableStateFlow(false)
    override val formattedTime: String = ""
    override val formattedDate: String = ""
    override val isByMe: Boolean = false
    override val isSent: StateFlow<Boolean> = MutableStateFlow(true)
    override val sender: StateFlow<UserInfoElement?> = MutableStateFlow(null)
    override val showSender: StateFlow<Boolean?> = MutableStateFlow(false)
    override val showBigGapBefore: StateFlow<Boolean?> = MutableStateFlow(false)
    override val sendError: StateFlow<String?> = MutableStateFlow(null)

    override fun jumpTo() {}

    override val transactionId: String = ""
    override val uploadProgress: StateFlow<FileTransferProgressElement?> = MutableStateFlow(null)
    override val canRetrySend: StateFlow<Boolean> = MutableStateFlow(false)
    override val canAbortSend: StateFlow<Boolean> = MutableStateFlow(false)

    override fun retrySend() {}

    override fun abortSend() {}
}
