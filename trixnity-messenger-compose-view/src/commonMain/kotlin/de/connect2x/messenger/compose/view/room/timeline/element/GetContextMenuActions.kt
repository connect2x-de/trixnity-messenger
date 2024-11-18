package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.BaseTimelineElementHolderContextMenuActionType.ABORT_SEND
import de.connect2x.messenger.compose.view.room.timeline.element.BaseTimelineElementHolderContextMenuActionType.INFO
import de.connect2x.messenger.compose.view.room.timeline.element.BaseTimelineElementHolderContextMenuActionType.REPORT
import de.connect2x.messenger.compose.view.room.timeline.element.BaseTimelineElementHolderContextMenuActionType.RETRY_SEND
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.FileBasedRoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class BaseTimelineElementHolderContextMenuAction(
    val label: String,
    private val action: () -> Unit
) {
    operator fun invoke() = action()
}

interface GetContextMenuActionsView {
    @Composable
    fun BaseTimelineElementHolderViewModel.create(
        i18n: I18nView,
        onAction: () -> Unit,
    ): State<List<BaseTimelineElementHolderContextMenuAction>>
}

@Composable
fun BaseTimelineElementHolderViewModel.getContextMenuActions(
    i18n: I18nView,
    downloadAction: () -> Unit,
): State<List<BaseTimelineElementHolderContextMenuAction>> {
    return with(DI.get<GetContextMenuActionsView>()) { create(i18n, downloadAction) }
}

class GetContextMenuActionsViewImpl : GetContextMenuActionsView {
    @Composable
    override fun BaseTimelineElementHolderViewModel.create(
        i18n: I18nView,
        downloadAction: () -> Unit,
    ): State<List<BaseTimelineElementHolderContextMenuAction>> {
        data class CanFlows(
            val canBeEditedFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeRedactedFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeRepliedToFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canRetrySendFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canAbortSendFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeReportedFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeReactedToFlow: StateFlow<Boolean> = MutableStateFlow(false),
        )

        val timelineElementHolderViewModel = remember(this) {
            when (this) {
                is TimelineElementHolderViewModel -> CanFlows(
                    canBeEditedFlow = canBeEdited,
                    canBeRedactedFlow = canBeRedacted,
                    canBeRepliedToFlow = canBeRepliedTo,
                    canBeReportedFlow = canBeReported,
                    canBeReactedToFlow = canBeReactedTo,
                )

                is OutboxElementHolderViewModel -> CanFlows(
                    canRetrySendFlow = canRetrySend,
                    canAbortSendFlow = canAbortSend,
                )
            }
        }
        val canDownload = remember {
            element.map { elementViewModel ->
                elementViewModel is FileBasedRoomMessageTimelineElementViewModel
            }
        }.collectAsState(false)
        val canBeEdited = timelineElementHolderViewModel.canBeEditedFlow.collectAsState()
        val canBeRedacted = timelineElementHolderViewModel.canBeRedactedFlow.collectAsState()
        val canBeRepliedTo = timelineElementHolderViewModel.canBeRepliedToFlow.collectAsState()
        val canBeReportedTo = timelineElementHolderViewModel.canBeReportedFlow.collectAsState()
        val canRetrySend = timelineElementHolderViewModel.canRetrySendFlow.collectAsState()
        val canAbortSend = timelineElementHolderViewModel.canAbortSendFlow.collectAsState()
        val canBeReactedTo = timelineElementHolderViewModel.canBeReactedToFlow.collectAsState()

        return remember(this) {
            derivedStateOf {
                buildList {
                    if (canDownload.value) add(
                        BaseTimelineElementHolderContextMenuAction(
                            label = i18n.downloadMessage(),
                            action = downloadAction
                        )
                    )
                    when (val baseTimelineElementHolderViewModel = this@create) {
                        is TimelineElementHolderViewModel -> {
                            if (canBeEdited.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    label = i18n.editMessage(),
                                    action = baseTimelineElementHolderViewModel::edit
                                )
                            )
                            if (canBeRedacted.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    label = i18n.redactMessage(),
                                    action = baseTimelineElementHolderViewModel::redact
                                )
                            )
                            if (canBeRepliedTo.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    label = i18n.replyMessage(),
                                    action = baseTimelineElementHolderViewModel::replyTo
                                )
                            )

                            if (canBeReactedTo.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    label = i18n.reactMessage(),
                                    action = {
                                        baseTimelineElementHolderViewModel.reactionsOpen.value = true
                                    }
                                )
                            )

                            if (canGetInfo.value) {
                                add(
                                    BaseTimelineElementHolderContextMenuAction(
                                        type = INFO,
                                        label = i18n.infoMessage(),
                                        action = {
                                            baseTimelineElementHolderViewModel.infoOpen.value = true
                                        },
                                    )
                                )
                            }

                            if (canBeReportedTo.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    type = REPORT,
                                    label = i18n.reportMessage(),
                                    action = baseTimelineElementHolderViewModel::report
                                )
                            )

                            baseTimelineElementHolderViewModel.element.value
                        }

                        is OutboxElementHolderViewModel -> {
                            if (canRetrySend.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    type = RETRY_SEND,
                                    label = i18n.retrySendMessage(),
                                    action = baseTimelineElementHolderViewModel::retrySend
                                )
                            )
                            if (canAbortSend.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    type = ABORT_SEND,
                                    label = i18n.abortSendMessage(),
                                    action = baseTimelineElementHolderViewModel::abortSend
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}


