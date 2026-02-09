package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.trixnity.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.DateStickyHeader
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.compose.view.util.waitForElementWithTimeout
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
            {
                Tooltip(i18n.devInfoButtonTooltip()){
                    IconButton({viewModel.openDevInfo()}){
                        Icon(Icons.Default.Info, i18n.devInfoButtonTooltip())
                    }
                }
            }
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
                        HorizontalDivider()
                        MiddleSpacer()
                        ReadersAndReactions(reactions, readers, scrollState, viewModel::openUserProfile)
                        SmallSpacer()
                    }
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
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
    parentScrollState: ScrollState,
    onOpenUserProfile: (UserId) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val state = rememberLazyListState()

    val allReadersAndReactions = remember(readers, reactions) {
        readers.associate {
            it.userId to EventReactions.ByUserInfo(
                mapOf(),
                it,
                false
            )
        }.plus(reactions.byUser).values.sortedByDescending { it.reactions.size }
    }
    val hasReadersOrReactions = allReadersAndReactions.isNotEmpty()
    var focusedItem by remember(allReadersAndReactions) {
        mutableStateOf(allReadersAndReactions.map { it.sender.userId }.firstOrNull())
    }

    Column(Modifier.heightIn(min = 25.dp, max = 500.dp)) {
        if (hasReadersOrReactions) {
            ThemedListItem(
                style = MaterialTheme.components.settingsItem,
                headlineContent = {
                    Text(
                        i18n.timelineElementMetadataReadersAndReactions(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            )
            Box {
                LazyColumn(Modifier.rovingFocusContainer(), state) {
                    items(allReadersAndReactions) { eventReaction ->
                        UserInfo(
                            eventReaction.sender,
                            Modifier.rovingFocusItem(
                                isFocused = focusedItem == eventReaction.sender.userId,
                                onFocus = { focusedItem = eventReaction.sender.userId }
                            ),
                            eventReaction.reactions.keys,
                            onOpenUserProfile = onOpenUserProfile,
                        )
                        Spacer(Modifier.height(5.dp))
                    }
                }
                if (state.canScrollForward || state.canScrollBackward) {
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
    modifier: Modifier = Modifier,
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
    Tooltip({ Text(tooltipText) }) {
        ThemedListItemButton(
            style = MaterialTheme.components.settingsItem,
            leadingContent = { ThemedUserAvatar(userInfo.initials, image) },
            headlineContent = {
                Text(
                    userInfo.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.paddingFromBaseline(0.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            supportingContent = if (!hasReactions) null else {
                {
                    Text(
                        compiledReactionsList,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                        modifier = Modifier.paddingFromBaseline(0.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            },
            modifier = modifier,
            onClick = {
                onOpenUserProfile(userInfo.userId)
            }
        )
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
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.timelineElementMetadataHistory()) },
            selected = showHistory,
            onChange = { showHistory = it },
        )
    }

    if (showHistory) {
        MessageHistory(elementHistory)
    } else {
        Column(Modifier.padding(end = 10.dp)) {
            DateStickyHeader(element.formattedDate, focusable = true)
            Spacer(Modifier.height(8.dp))
            MessageContent(element)
        }
    }
}

@Composable
private fun MessageContent(messageHolder: TimelineElementHolderViewModel) {
    val element = messageHolder.element.collectAsState().value
    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
    Column {
        element?.let { element ->
            timelineElementViewSelector.createAsPreview(messageHolder, element, index = 0)
        }
    }
}

@Composable
private fun MessageHistory(elementHistory: List<TimelineElementHolderViewModel>) {
    val scrollState = rememberLazyListState()
    val canScroll by remember { derivedStateOf { scrollState.canScrollForward || scrollState.canScrollBackward } }

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


        // The max height is required here due to this component being a nested scroll container; no max height results in a crash
        Box(Modifier.heightIn(max = 400.dp)) {
            LazyColumn(Modifier.fillMaxWidth().padding(end = 10.dp), state = scrollState) {
                elementHistoryGrouped.forEach { (date, viewModel) ->
                    if (date != null) {
                        item("date-$date-${viewModel.key}") {
                            DateStickyHeader(date, focusable = true)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item(viewModel.key) {
                        MessageContent(viewModel)
                    }
                }
            }

            // If the scroll bar were always shown it would force the box to its maximum height, creating a lot of empty space.
            if (canScroll)
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState, false)
        }
    }
}
