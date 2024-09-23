package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.HistoryVisibilityChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.MemberStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomAliasChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomAvatarChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomCreatedStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomEncryptionEnabledViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomNameChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomTopicChangeStatusViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

// FIXME

@Composable
fun RoomCreate(roomCreatedStatusViewModel: RoomCreatedStatusViewModel) {
    val roomCreatedMessage = roomCreatedStatusViewModel.roomCreatedMessage.collectAsState().value
    roomCreatedMessage?.let {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
fun RoomAvatarChange(roomAvatarChangeStatusViewModel: RoomAvatarChangeStatusViewModel) {
    val roomAvatarChangeMessage = roomAvatarChangeStatusViewModel.roomAvatarChangeMessage.collectAsState().value
    roomAvatarChangeMessage?.let {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
fun RoomNameChange(roomNameChangeStatusViewModel: RoomNameChangeStatusViewModel) {
    val roomNameChangeMessage = roomNameChangeStatusViewModel.roomNameChangeMessage.collectAsState().value
    roomNameChangeMessage?.let {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
fun RoomTopicChange(roomTopicChangeStatusViewModel: RoomTopicChangeStatusViewModel) {
    val roomTopicChangeMessage = roomTopicChangeStatusViewModel.roomTopicChangeMessage.collectAsState().value
    roomTopicChangeMessage?.let {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
fun RoomAliasChange(roomAliasChangeStatusViewModel: RoomAliasChangeStatusViewModel) {
    val roomAliasChangeMessage = roomAliasChangeStatusViewModel.roomAliasChangeMessage.collectAsState().value
    roomAliasChangeMessage.forEach {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
        }
    }
}


@Composable
fun RoomEncryptionEnabled(roomEncryptionEnabledViewModel: RoomEncryptionEnabledViewModel) {
    val roomEncryptionEnabledMessage =
        roomEncryptionEnabledViewModel.roomEncryptionEnabledMessage.collectAsState().value
    roomEncryptionEnabledMessage?.let {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
fun MemberChangeIndicator(
    memberStatusViewModel: MemberStatusViewModel
) {
    // not all member changes should get an indicator -> nullable
    val message = memberStatusViewModel.formattedMemberStatus.collectAsState().value
    message?.let {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(message, MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
fun HistoryVisibilityChange(historyVisibilityChangeStatusViewModel: HistoryVisibilityChangeStatusViewModel) {
    val historyVisibilityChangeMessage = historyVisibilityChangeStatusViewModel.historyVisibilityMessage.collectAsState().value
    historyVisibilityChangeMessage?.let {
        Indicator(MaterialTheme.colorScheme.tertiary) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiary)
        }
    }
}
@Composable
fun UnreadMessagesIndicator(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
        val hasUnreadMarker = timelineElementHolderViewModel.shouldShowUnreadMarkerFlow.collectAsState().value
        if (hasUnreadMarker) {
            val i18n = DI.get<I18nView>()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(
                    Modifier.weight(1.0f).padding(end = 20.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    thickness = 3.dp
                )
                Text(
                    text = i18n.indicatorUnreadMessages(),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(
                    Modifier.weight(1.0f).padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    thickness = 3.dp
                )
            }
        }
    }
}

@Composable
fun DateChangeIndicator(timelineElementHolderViewModel: BaseTimelineElementHolderViewModel) {
    val viewModel = timelineElementHolderViewModel.timelineElementViewModel.collectAsState().value
    if (viewModel?.showDateAbove == true) {
        Indicator(MaterialTheme.colorScheme.tertiaryContainer, withPadding = true) {
            IndicatorText(viewModel.formattedDate, MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun DateStickyHeader(
    timelineViewModel: TimelineViewModel
) {
    val stickyDate = timelineViewModel.stickyDate.collectAsState().value
    stickyDate?.let {
        Indicator(MaterialTheme.colorScheme.tertiaryContainer, withPadding = false) {
            IndicatorText(it, MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun Indicator(
    containerColor: Color,
    withPadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(
                    top = if (withPadding) 10.dp else 0.dp,
                    start = maxWidth / 4, // only use max half the screen's width (on both sides == 4 // )
                    end = maxWidth / 4,
                )
        ) {
            Box(
                Modifier.background(color = containerColor, shape = RoundedCornerShape(8.dp))
                    .align(Alignment.Center)
                    .padding(5.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun IndicatorText(message: String, color: Color) {
    Tooltip(tooltip = {
        TooltipText(message)
    }) {
        Text(
            message,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
    }
}

@Composable
fun LoadingIndicatorBefore(timelineElementHolderViewModel: BaseTimelineElementHolderViewModel) {
    if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
        val showLoadingIndicator = timelineElementHolderViewModel.showLoadingIndicatorBefore.collectAsState().value

        if (showLoadingIndicator) {
            LoadingSpinner()
        }
    }
}

@Composable
fun LoadingIndicatorAfter(timelineElementHolderViewModel: BaseTimelineElementHolderViewModel) {
    if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
        val showLoadingIndicator = timelineElementHolderViewModel.showLoadingIndicatorAfter.collectAsState().value

        if (showLoadingIndicator) {
            LoadingSpinner()
        }
    }
}

@Composable
fun LeaveRoom(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    timelineViewModel: TimelineViewModel
) {
    val i18n = DI.get<I18nView>()
    val viewModel = timelineElementHolderViewModel.timelineElementViewModel.collectAsState().value
    val isDirect = timelineViewModel.isDirect.collectAsState().value
    if (viewModel != null) {
        val invitation = viewModel.invitation.collectAsState(null).value

        invitation?.let {
            val groupOrChat = if (isDirect) i18n.commonChat() else i18n.commonGroup()
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(if (maxWidth > 600.dp) 0.6f else 1.0f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Center) {
                        Column(Modifier.align(Alignment.CenterVertically)) {
                            Text(i18n.indicatorLeave(groupOrChat))
                            Spacer(Modifier.size(5.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall)

                        }
                        Spacer(Modifier.size(10.dp))
                        Button(
                            { timelineViewModel.leaveRoom() },
                            Modifier
                                .clip(CircleShape)
                                .size(48.dp)
                                .buttonPointerModifier(),
                            contentPadding = PaddingValues(start = 2.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                i18n.indicatorLeave(groupOrChat),
                            )
                        }
                    }
                }
            }
        }
    }
}
