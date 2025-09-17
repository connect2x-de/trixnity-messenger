package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.DateStickyHeader
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.messenger.compose.view.theme.components.ThemedSwitch
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.messenger.compose.view.util.waitForElementWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.core.model.UserId

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

        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        var elementHistory by remember { mutableStateOf(listOf<TimelineElementHolderViewModel>()) }
        val firstElement = elementHistory.firstOrNull()
        var lastElement by remember { mutableStateOf<TimelineElementHolderViewModel?>(null) }
        val messageElement =
            lastElement?.element?.collectAsState()?.value as? RoomMessageTimelineElementViewModel.TextBased<*>
        val sender = lastElement?.sender?.collectAsState()?.value
        val reactions = firstElement?.reactions?.collectAsState()?.value
        val readers = firstElement?.readers?.collectAsState()?.value
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            launch {
                viewModel.elementHistory.filterNotNull().collect { history ->
                    withContext(Dispatchers.Default) {
                        history.forEach { element ->
                            launch {
                                waitForElementWithTimeout(timelineElementViewSelector, element)
                            }
                        }
                    }
                    elementHistory = history
                }
            }
            viewModel.element.filterNotNull().collect { newElement ->
                waitForElementWithTimeout(timelineElementViewSelector, newElement)
                lastElement = newElement
            }
        }

        ExtrasPaneHeader(
            title = i18n.timelineElementMetadataTitle(),
            error = null,
            onBack = { viewModel.back() },
            backButtonType = if (isSinglePane || isBottomOfStack.not()) BACK else CLOSE,
        ) {
            if (reactions == null || readers == null || sender == null || lastElement == null || elementHistory.isEmpty()) {
                LoadingSpinner(Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier.fillMaxSize()
                ) {
                    Column(
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.padding(PaddingValues(vertical = 0.dp, horizontal = 20.dp)).fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        SubHeading(i18n.timelineElementMetadataSender())
                        UserInfo(
                            sender,
                            onOpenUserProfile = viewModel::openUserProfile,
                        )
                        SubHeading(i18n.timelineElementMetadataMessage())
                        lastElement?.let {
                            MessageContentHistorySwitch(it, elementHistory)
                        }
                        SmallSpacer()
                        messageElement?.body?.let { content ->
                            ExpandableSection(i18n.timelineElementMetadataBody(), Icons.Default.Code) {
                                ThemedSelectableText(content, MaterialTheme.components.selectionOnSurface)
                            }
                            SmallSpacer()
                        }
                        messageElement?.formattedBody?.let { content ->
                            ExpandableSection(i18n.timelineElementMetadataFormattedBody(), Icons.Default.Code) {
                                ThemedSelectableText(content, MaterialTheme.components.selectionOnSurface)
                            }
                            SmallSpacer()
                        }
                        HorizontalDivider()
                        MiddleSpacer()
                        ReadersAndReactions(reactions, readers, viewModel::openUserProfile)
                        SmallSpacer()
                    }
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
                }
            }
        }
    }
}

@Composable
fun ColumnScope.ExpandableSection(
    heading: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val rotateState by animateFloatAsState(if (expanded.value) 180F else 0F)

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.clickable(interactionSource, LocalIndication.current) {
                        expanded.value = !expanded.value
                    }.buttonPointerModifier(true).padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(heading, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1F).padding(end = 10.dp))
                Icon(
                    Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(rotateState)
                )
            }
            AnimatedVisibility(expanded.value) {
                Box(Modifier.padding(8.dp)) {
                    content()
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
    reactions: EventReactions,
    readers: List<UserInfoElement>,
    onOpenUserProfile: (UserId) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val state = rememberLazyListState()

    val allReadersAndReactions = remember(readers, reactions) {
        (readers.associate {
            it.userId to EventReactions.ByUserInfo(
                mapOf(),
                it,
                false
            )
        } + reactions.byUser).values.toList()
    }.sortedByDescending { it.reactions.size }
    val hasReadersOrReactions = allReadersAndReactions.isNotEmpty()

    Column(Modifier.heightIn(min = 100.dp, max = 500.dp)) {
        if (hasReadersOrReactions) {
            Text(
                text = i18n.timelineElementMetadataReadersAndReactions(),
                style = MaterialTheme.typography.titleMedium,
            )
            SmallSpacer()
            Box {
                LazyColumn(state = state) {
                    items(allReadersAndReactions) { eventReaction ->
                        UserInfo(
                            eventReaction.sender,
                            eventReaction.reactions.keys,
                            onOpenUserProfile = onOpenUserProfile,
                        )
                        Spacer(Modifier.height(5.dp))
                    }
                }
                if (allReadersAndReactions.size > 6) {
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd), state, false)
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
    Tooltip({ TooltipText(tooltipText) }) {
        Row(Modifier.fillMaxWidth().clickable {
                onOpenUserProfile(userInfo.userId)
            }.buttonPointerModifier()) {
            Box(
                Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp).align(Alignment.CenterVertically)
            ) {
                ThemedUserAvatar(userInfo.initials, image)
            }
            Column(
                Modifier.align(Alignment.CenterVertically).padding(start = 8.dp)
            ) {
                Text(
                    userInfo.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.paddingFromBaseline(0.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                if (hasReactions) {
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
    elementHistory: List<TimelineElementHolderViewModel>,
) {
    val i18n = DI.get<I18nView>()
    var showHistory by remember { mutableStateOf(false) }

    if (elementHistory.isNotEmpty() && elementHistory.size > 1) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showHistory = showHistory.not() }.buttonPointerModifier(),
        ) {
            Text(text = i18n.timelineElementMetadataHistory(), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.size(5.dp).weight(1f, true))
            ThemedSwitch(
                checked = showHistory,
                onCheckedChange = { showHistory = it },
            )
        }
    }

    Column(Modifier.heightIn(min = 50.dp, max = 500.dp)) {
        if (showHistory) {
            MessageHistory(elementHistory)
        } else {
            val scrollState = rememberScrollState()
            Box {
                Column(Modifier.verticalScroll(scrollState).padding(end = 10.dp)) {
                    DateStickyHeader(element.formattedDate)
                    Spacer(Modifier.height(8.dp))
                    MessageContent(element)
                }
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
            }
        }
    }
}

@Composable
private fun MessageContent(messageHolder: TimelineElementHolderViewModel) {
    val element = messageHolder.element.collectAsState().value
    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
    Column {
        element?.let { element ->
            timelineElementViewSelector.createAsPreview(messageHolder, element)
        }
    }
}

@Composable
private fun MessageHistory(elementHistory: List<TimelineElementHolderViewModel>) {
    val scrollState = rememberLazyListState()

    if (elementHistory.isNotEmpty()) {
        val elementHistoryGrouped by derivedStateOf {
            buildList(elementHistory.size) {
                var lastDate: String? = null
                for (index in elementHistory.indices) {
                    val vm = elementHistory[index]
                    when {
                        lastDate == vm.formattedDate -> add(null to vm)
                        vm.element.value is TimelineElementViewModel.Empty -> add(null to vm)
                        else -> {
                            add(vm.formattedDate to vm)
                            lastDate = vm.formattedDate
                        }
                    }
                }
            }
        }

        Box {
            LazyColumn(Modifier.fillMaxWidth().padding(end = 10.dp), state = scrollState) {
                elementHistoryGrouped.forEach { (date, viewModel) ->
                    if (date != null) {
                        item("date-$date-${viewModel.key}") {
                            DateStickyHeader(date)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item(viewModel.key) {
                        MessageContent(viewModel)
                    }
                }
            }
            VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState, false)
        }
    }
}
