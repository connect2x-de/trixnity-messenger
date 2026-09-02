package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.focusHighlighting
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.HorizontalDividerWithText
import de.connect2x.trixnity.messenger.compose.view.room.timeline.Indicator
import de.connect2x.trixnity.messenger.compose.view.room.timeline.IndicatorText
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSurface
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.MemberStateTimelineElementViewModel
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

interface MemberStateTimelineElementView : TimelineElementView<MemberStateTimelineElementViewModel>

class MemberStateTimelineElementViewImpl : MemberStateTimelineElementView {
    override val supports: KClass<MemberStateTimelineElementViewModel> = MemberStateTimelineElementViewModel::class

    override suspend fun waitFor(element: MemberStateTimelineElementViewModel) {
        element.changeMessage.filterNotNull().first()

        if (element.showRejoinRoomInfo.filterNotNull().first()) {
            element.rejoinRoomInfoType.filterNotNull().first()
        }
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        index: Int,
    ) {
        element.undecryptableHistoryInfo.collectAsState().value?.let { HorizontalDividerWithText(it) }
        StateElement(element)
        RejoinRoomInfoSwitcher(element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        index: Int,
    ) {
        StateElement(element)
        RejoinRoomInfoSwitcher(element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            element = element,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { StateElement(element) },
        )
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        ReferencedMessagePill(
            holder = holder,
            element = element,
            modifier = modifier,
            interactionSource = interactionSource,
            content = { StateElement(element) },
        )
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: MemberStateTimelineElementViewModel,
    ): ClipEntry? = null

    @Composable
    private fun StateElement(element: MemberStateTimelineElementViewModel) {
        val changeMessage = element.changeMessage.collectAsState().value
        changeMessage
            ?.takeIf { it.isNotBlank() }
            ?.let {
                Indicator(MaterialTheme.colorScheme.tertiary, focusable = true) {
                    IndicatorText(changeMessage, MaterialTheme.colorScheme.onTertiary)
                }
            }
    }

    @Composable
    private fun RejoinRoomInfoSwitcher(element: MemberStateTimelineElementViewModel) {
        val showRejoinRoomInfo = element.showRejoinRoomInfo.collectAsState().value
        if (showRejoinRoomInfo != true) {
            return
        }

        val i18n = DI.get<I18nView>()

        val error = element.rejoinRoomInfoError.collectAsState().value

        if (error != null) {
            RejoinRoomInfo(text = error, null)
            return
        }

        when (val joinRuleAction = element.rejoinRoomInfoType.collectAsState().value) {
            is JoinRoomActionViewModel.JoinRoomAction.Join -> {
                RejoinRoomInfo(text = i18n.rejoinRoomJoin(), buttonText = i18n.rejoinRoomJoinButton()) {
                    joinRuleAction.onJoinRoom()
                }
            }

            is JoinRoomActionViewModel.JoinRoomAction.Knock -> {
                val hasKnocked = joinRuleAction.hasKnocked.collectAsState().value
                if (hasKnocked == false) {
                    RejoinRoomInfo(text = i18n.rejoinRoomKnock(), buttonText = i18n.rejoinRoomKnockButton()) {
                        joinRuleAction.onKnock()
                    }
                } else {
                    RejoinRoomInfo(text = i18n.rejoinRoomKnockKnocked(), buttonText = null)
                }
            }

            is JoinRoomActionViewModel.JoinRoomAction.Restricted -> {

                val nullCount = joinRuleAction.requiredUnknownRooms
                val nonNullRooms = joinRuleAction.requiredRooms

                RejoinRoomInfo(
                    text =
                        if (nullCount == 0) {
                            i18n.rejoinRoomRestricted(nonNullRooms)
                        } else {
                            if (nonNullRooms.isEmpty()) {
                                i18n.rejoinRoomRestricted(nullCount)
                            } else {
                                i18n.rejoinRoomRestricted(nonNullRooms, nullCount)
                            }
                        },
                    buttonText = null,
                )
            }

            JoinRoomActionViewModel.JoinRoomAction.Private,
            JoinRoomActionViewModel.JoinRoomAction.NotFound,
            is JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation,
            null -> {}
        }
    }

    @Composable
    private fun RejoinRoomInfo(text: String?, buttonText: String?, onClick: () -> Unit = {}) {
        val interactionSource = remember { MutableInteractionSource() }

        BoxWithConstraints {
            Box(
                Modifier.fillMaxWidth()
                    .padding(
                        top = 10.dp, // Same as de.connect2x.trixnity.messenger.compose.view.room.timeline.Indicator
                        start = maxWidth / 16,
                        end = maxWidth / 16,
                    )
            ) {
                // Same as de.connect2x.trixnity.messenger.compose.view.room.timeline.Indicator
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    ThemedSurface(
                        style = MaterialTheme.components.messageBubbleOther,
                        modifier = Modifier.align(Alignment.Center).focusHighlighting(interactionSource),
                        onClick = {},
                        interactionSource = interactionSource,
                    ) {
                        Box(Modifier.padding(10.dp)) {
                            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                                if (text != null) {
                                    Box(modifier = Modifier.weight(1f, fill = false)) {
                                        Tooltip(tooltip = { Text(text) }) {
                                            Text(
                                                text,
                                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.75f),
                                                style = MaterialTheme.typography.labelMedium,
                                                textAlign = TextAlign.Center,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 4,
                                            )
                                        }
                                    }
                                }
                                if (buttonText != null) {
                                    SmallSpacer()
                                    ThemedButton(onClick = onClick, style = MaterialTheme.components.primaryButton) {
                                        Text(buttonText, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
