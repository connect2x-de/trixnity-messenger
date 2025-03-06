package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.DateStickyHeader
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.room.timeline.element.util.Tooltip
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration.Companion.milliseconds


interface TimelineElementMetadataView {
    @Composable
    fun create(
        viewModel: TimelineElementMetadataViewModel,
        isBottomOfStack: Boolean, isSinglePane: Boolean,
    )
}

@Composable
fun TimelineElementMetadata(
    viewModel: TimelineElementMetadataViewModel,
    isBottomOfStack: Boolean, isSinglePane: Boolean,
) {
    DI.get<TimelineElementMetadataView>().create(viewModel, isBottomOfStack, isSinglePane)
}

class TimelineElementMetadataViewImpl : TimelineElementMetadataView {
    @Composable
    override fun create(
        viewModel: TimelineElementMetadataViewModel,
        isBottomOfStack: Boolean, isSinglePane: Boolean,
    ) {
        val i18n = DI.get<I18nView>()

        val elementHistory = viewModel.elementHistory.collectAsState().value
        val element = viewModel.element.collectAsState().value
        val sender = element?.sender?.collectAsState()?.value
        val reactions = element?.reactions?.collectAsState()?.value
        val readers = element?.readers?.collectAsState()?.value

        if (element == null || reactions == null || readers == null || sender == null) {
            LoadingSpinner(Modifier.fillMaxSize())
        } else {
            ExtrasPaneHeader(
                title = i18n.timelineElementMetadataTitle(),
                error = null,
                onBack = { viewModel.back() },
                backButtonType = if (isSinglePane || isBottomOfStack.not()) BACK else CLOSE,
            ) {
                Box(
                    Modifier.fillMaxSize()
                ) {
                    Column(
                        Modifier
                            .padding(PaddingValues(vertical = 0.dp, horizontal = 20.dp))
                            .fillMaxSize()
                    ) {
                        SubHeading(i18n.timelineElementMetadataSender())
                        UserInfo(
                            sender,
                            onOpenUserProfile = viewModel::openUserProfile,
                        )
                        SubHeading(i18n.timelineElementMetadataMessage())
                        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f)) {
                            MessageContentHistorySwitch(element, elementHistory)
                        }
                        SmallSpacer()
                        HorizontalDivider()
                        MiddleSpacer()
                        ReadersAndReactions(element, viewModel)
                        SmallSpacer()
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.SubHeading(heading: String) {// TODO re-use in other components
    MiddleSpacer()
    Text(
        text = heading,
        style = MaterialTheme.typography.titleMedium,
    )
    SmallSpacer()
}

@Composable
fun ColumnScope.ReadersAndReactions(
    element: TimelineElementHolderViewModel,
    viewModel: TimelineElementMetadataViewModel,
) {
    val i18n = DI.get<I18nView>()
    val reactions = element.reactions.collectAsState().value
    val readers = element.readers.collectAsState().value
    if (reactions != null && readers != null) {
        val allReadersAndReactions = remember(readers, reactions) {
            (readers.associate { it.userId to EventReactions.ByUserInfo(mapOf(), it, false) } +
                    reactions.byUser).values.toList()
        }.sortedByDescending { it.reactions.size }
        val hasReadersOrReactions = allReadersAndReactions.isNotEmpty()

        if (hasReadersOrReactions) {
            Text(
                text = i18n.timelineElementMetadataReadersAndReactions(),
                style = MaterialTheme.typography.titleMedium,
            )
            SmallSpacer()
            LazyColumn {
                items(allReadersAndReactions) { eventReaction ->
                    UserInfo(
                        eventReaction.sender,
                        eventReaction.reactions.keys,
                        onOpenUserProfile = viewModel::openUserProfile,
                    )
                    Spacer(Modifier.height(5.dp))
                }
            }
        } else {
            Text(
                text = i18n.timelineElementMetadataReadersAndReactionsNone(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun UserInfo(
    userInfo: UserInfoElement,
    reactions: Set<String> = setOf(),
    onOpenUserProfile: (UserId) -> Unit,
) {
    val image = userInfo.image?.collectAsState()?.value
    val i18n = DI.get<I18nView>()
    val compiledReactionsList: String = reactions.joinToString(" ")
    val hasReactions = compiledReactionsList.isNotEmpty()
    val tooltipText = buildString {
        append("${userInfo.name}: ${userInfo.userId.full}")
        if (hasReactions) {
            appendLine()
            append(i18n.timelineElementMetadataUserInfoTooltipReactions(compiledReactionsList))
        }
    }
    Tooltip(
        { TooltipText(tooltipText) },
        delay = 50.milliseconds,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenUserProfile(userInfo.userId)
                }
                .buttonPointerModifier()
        ) {
            Box(
                Modifier
                    .align(CenterVertically)
            ) {
                Avatar(image, userInfo.initials)
            }
            Column(
                Modifier
                    .align(CenterVertically)
                    .padding(start = 8.dp)
            ) {
                Text(
                    userInfo.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.paddingFromBaseline(0.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                if (hasReactions) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        compiledReactionsList,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                        modifier = Modifier.paddingFromBaseline(0.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.MessageContentHistorySwitch(
    element: TimelineElementHolderViewModel,
    elementHistory: List<TimelineElementHolderViewModel>?,
) {
    val i18n = DI.get<I18nView>()
    var showHistory by remember { mutableStateOf(false) }

    if (elementHistory.isNullOrEmpty().not() && elementHistory.size > 1) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier.clickable { showHistory = showHistory.not() }.buttonPointerModifier(),
        ) {
            Text(text = i18n.timelineElementMetadataHistory(), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.size(5.dp).weight(1f, true))
            Switch(
                checked = showHistory,
                onCheckedChange = { showHistory = it },
                modifier = Modifier.buttonPointerModifier(),
            )
        }
    }

    if (showHistory) {
        MessageHistory(elementHistory)
    } else {
        MessageContent(element)
    }
}

@Composable
private fun ColumnScope.MessageContent(
    messageHolder: TimelineElementHolderViewModel,
    lastMessageHolder: TimelineElementHolderViewModel? = null,
) {
    if (lastMessageHolder == null || messageHolder.formattedDate != lastMessageHolder.formattedDate) {
        DateStickyHeader(messageHolder.formattedDate)
        Spacer(Modifier.height(8.dp))
    }

    messageHolder.element.collectAsState().value?.let { element ->
        Column(
            Modifier.padding(end = 8.dp),
        ) {
            DI.get<TimelineElementViewSelector>().createAsPreview(messageHolder, element)
        }
        SmallSpacer()
    }
}

@Composable
private fun ColumnScope.MessageHistory(elementHistory: List<TimelineElementHolderViewModel>?) {
    if (elementHistory?.isNotEmpty() == true) {
        LazyColumn {
            var lastMessageHolder: TimelineElementHolderViewModel? = null
            items(elementHistory) { elementHolder ->
                MessageContent(elementHolder, lastMessageHolder)
                lastMessageHolder = elementHolder
            }
        }
    }
}
