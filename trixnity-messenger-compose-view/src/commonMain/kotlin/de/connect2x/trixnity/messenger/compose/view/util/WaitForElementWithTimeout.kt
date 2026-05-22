package de.connect2x.trixnity.messenger.compose.view.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.util.WaitForElementWithTimeoutKt")

private fun CoroutineScope.launchWithTimeoutHint(message: () -> String, hint: () -> String, block: suspend () -> Unit) =
    launch {
        try {
            block()
        } catch (_: TimeoutCancellationException) {
            log.warn { message() + hint() }
        }
    }

internal suspend fun waitForElementWithTimeout(
    timelineElementViewSelector: TimelineElementViewSelector,
    element: BaseTimelineElementHolderViewModel,
) {
    val message = { "waited for element ${element.key}, but timed out: " }

    withTimeoutOrNull(3.seconds) {
        val elementElement = element.element.filterNotNull().first()
        if (elementElement is TimelineElementViewModel.Empty) return@withTimeoutOrNull
        launchWithTimeoutHint(message, { "element ${elementElement::class.simpleName}" }) {
            timelineElementViewSelector.waitFor(elementElement)
        }
        launchWithTimeoutHint(message, { "isFirstInUserSequence" }) {
            element.isFirstInUserSequence.filterNotNull().first()
        }
        launchWithTimeoutHint(message, { "sender" }) {
            val showSender = element.showSender.filterNotNull().first()
            if (showSender) element.sender.filterNotNull().first()
        }
        launchWithTimeoutHint(message, { "showBigGapBefore" }) { element.showBigGapBefore.filterNotNull().first() }
        launchWithTimeoutHint(message, { "repliedElement" }) {
            val isReply = element.isReply.filterNotNull().first()
            if (isReply) {
                val repliedElement = element.repliedElement.filterNotNull().first()
                timelineElementViewSelector.waitFor(repliedElement.element.filterNotNull().first())
                repliedElement.sender.filterNotNull().first()
            }
        }
        when (element) {
            is TimelineElementHolderViewModel -> {
                launchWithTimeoutHint(message, { "showUnreadMarker" }) {
                    element.showUnreadMarker.filterNotNull().first()
                }
                launchWithTimeoutHint(message, { "showLoadingIndicatorBefore" }) {
                    element.showLoadingIndicatorBefore.filterNotNull().first()
                }
                launchWithTimeoutHint(message, { "showLoadingIndicatorAfter" }) {
                    element.showLoadingIndicatorAfter.filterNotNull().first()
                }
                if (element.isByMe)
                    launchWithTimeoutHint(message, { "isRead" }) { element.isRead.filterNotNull().first() }
                launchWithTimeoutHint(message, { "reactions" }) { element.reactions.filterNotNull().first() }
                launchWithTimeoutHint(message, { "isReplaced" }) { element.isReplaced.filterNotNull().first() }
            }

            is OutboxElementHolderViewModel -> {}
        }
    }
}
