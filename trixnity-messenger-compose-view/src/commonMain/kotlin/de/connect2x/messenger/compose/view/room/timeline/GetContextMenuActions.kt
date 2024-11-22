package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsDebug
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.ABORT_SEND
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.DEBUG
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.DOWNLOAD
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.EDIT
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.REDACT
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.REACT
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.REPLY
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.REPORT
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.RETRY_SEND
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.INFO
import de.connect2x.messenger.compose.view.room.timeline.BaseTimelineElementHolderContextMenuActionType.REACTOR_LIST
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FileBasedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

enum class BaseTimelineElementHolderContextMenuActionType {
    DOWNLOAD,
    EDIT,
    REDACT,
    REACT,
    REACTOR_LIST,
    REPLY,
    REPORT,
    RETRY_SEND,
    ABORT_SEND,
    DEBUG,
    INFO,
}

class BaseTimelineElementHolderContextMenuAction(
    val type: BaseTimelineElementHolderContextMenuActionType,
    val label: String,
    private val action: () -> Unit
) {
    operator fun invoke() = action()
}

// TODO TIM
interface GetContextMenuActionsView {
    @Composable
    fun BaseTimelineElementHolderViewModel.create(
        i18n: I18nView,
        downloadAction: () -> Unit,
        debugAction: () -> Unit,
    ): State<List<BaseTimelineElementHolderContextMenuAction>>
}

@Composable
fun BaseTimelineElementHolderViewModel.getContextMenuActions(
    i18n: I18nView,
    downloadAction: () -> Unit,
    debugAction: () -> Unit
): State<List<BaseTimelineElementHolderContextMenuAction>> {
    return with(DI.get<GetContextMenuActionsView>()) { create(i18n, downloadAction, debugAction) }
}

class GetContextMenuActionsViewImpl : GetContextMenuActionsView {
    @Composable
    override fun BaseTimelineElementHolderViewModel.create(
        i18n: I18nView,
        downloadAction: () -> Unit,
        debugAction: () -> Unit
    ): State<List<BaseTimelineElementHolderContextMenuAction>> {
        data class CanFlows(
            val canBeEditedFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeRedactedFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeRepliedToFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canRetrySendFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canAbortSendFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeReportedFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canBeReactedToFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canGetReactedFlow: StateFlow<Boolean> = MutableStateFlow(false),
            val canGetInfoFlow: StateFlow<Boolean> = MutableStateFlow(false),
        )

        val timelineElementHolderViewModel = remember(this) {
            when (this) {
                is TimelineElementHolderViewModel -> CanFlows(
                    canBeEditedFlow = canBeEdited,
                    canBeRedactedFlow = canBeRedacted,
                    canBeRepliedToFlow = canBeRepliedTo,
                    canBeReportedFlow = canBeReported,
                    canBeReactedToFlow = canBeReactedTo,
                    canGetReactedFlow = canGetReacted,
                    canGetInfoFlow = canGetInfo,
                )

                is OutboxElementHolderViewModel -> CanFlows(
                    canRetrySendFlow = canRetrySend,
                    canAbortSendFlow = canAbortSend,
                )
            }
        }
        val canDownload = remember(timelineElementViewModel) {
            timelineElementViewModel.map { elementViewModel ->
                elementViewModel is FileBasedMessageViewModel
            }
        }.collectAsState(false)
        val canBeEdited = timelineElementHolderViewModel.canBeEditedFlow.collectAsState()
        val canBeRedacted = timelineElementHolderViewModel.canBeRedactedFlow.collectAsState()
        val canBeRepliedTo = timelineElementHolderViewModel.canBeRepliedToFlow.collectAsState()
        val canBeReportedTo = timelineElementHolderViewModel.canBeReportedFlow.collectAsState()
        val canRetrySend = timelineElementHolderViewModel.canRetrySendFlow.collectAsState()
        val canAbortSend = timelineElementHolderViewModel.canAbortSendFlow.collectAsState()
        val canBeReactedTo = timelineElementHolderViewModel.canBeReactedToFlow.collectAsState()
        val canGetReacted = timelineElementHolderViewModel.canGetReactedFlow.collectAsState()
        val canGetInfo = timelineElementHolderViewModel.canGetInfoFlow.collectAsState()
        val canDebug = IsDebug.current

        return remember(this) {
            derivedStateOf {
                buildList {
                    if (canDownload.value) add(
                        BaseTimelineElementHolderContextMenuAction(
                            type = DOWNLOAD,
                            label = i18n.downloadMessage(),
                            action = downloadAction
                        )
                    )
                    when (val baseTimelineElementHolderViewModel = this@create) {
                        is TimelineElementHolderViewModel -> {
                            if (canBeEdited.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    type = EDIT,
                                    label = i18n.editMessage(),
                                    action = baseTimelineElementHolderViewModel::edit
                                )
                            )
                            if (canBeRedacted.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    type = REDACT,
                                    label = i18n.redactMessage(),
                                    action = baseTimelineElementHolderViewModel::redact
                                )
                            )
                            if (canBeRepliedTo.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    type = REPLY,
                                    label = i18n.replyMessage(),
                                    action = baseTimelineElementHolderViewModel::replyTo
                                )
                            )

                            if (canBeReactedTo.value) add(
                                BaseTimelineElementHolderContextMenuAction(
                                    type = REACT,
                                    label = i18n.reactMessage(),
                                    action = {
                                        baseTimelineElementHolderViewModel.reactionsOpen.value = true
                                    }
                                )
                            )

                            if (canGetReacted.value) {
                                add(
                                    BaseTimelineElementHolderContextMenuAction(
                                        type = REACTOR_LIST,
                                        label = i18n.reactorListMessage(),
                                        action = {
                                            baseTimelineElementHolderViewModel.reactorListOpen.value = true
                                        },
                                    )
                                )
                            }

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
                                    action = baseTimelineElementHolderViewModel::reportTo
                                )
                            )

                            baseTimelineElementHolderViewModel.timelineElementViewModel.value
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
                    if (canDebug) add(
                        BaseTimelineElementHolderContextMenuAction(
                            type = DEBUG,
                            label = i18n.debugMessage(),
                            action = debugAction
                        )
                    )
                }
            }
        }
    }
}


