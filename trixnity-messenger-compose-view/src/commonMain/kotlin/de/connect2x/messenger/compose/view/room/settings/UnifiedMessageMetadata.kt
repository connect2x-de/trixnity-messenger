package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.pointerEventWrapper
import de.connect2x.messenger.compose.view.room.settings.ExtrasPaneHeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.room.settings.ExtrasPaneHeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageUserInteraction
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import kotlinx.coroutines.launch


@Composable
@OptIn(ExperimentalLayoutApi::class)
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel, stackPosition: Int, isSinglePane: Boolean) {
    val scrollState = rememberScrollState()
    val edits = viewModel.edits.collectAsState().value
    val reactionCounts = viewModel.reactionCounts.collectAsState().value
    val userInteractions = viewModel.userInteractions.collectAsState().value
    val senderInfo = viewModel.senderInfo.collectAsState().value
    val interactionFilterByReaction = remember { mutableStateOf<ReactionKey?>(null) }
    val reactionFilterHeight = if (reactionCounts.isEmpty()) 0.dp else 64.dp // TODO: Derive the value from a config.
    ExtrasPaneHeader(
        "Message details", // TODO: i18n
        null, // TODO
        { viewModel.back() },
        if (isSinglePane || stackPosition > 2) BACK else CLOSE,
    ) {
        Column(
            Modifier
//            .background(Color.Blue)
        ) {
            Spacer(Modifier.size(8.dp))
            Text("Message Sender:") // TODO: i18n
            senderInfo?.let { info ->
                val avatarImage = info.image?.collectAsState(null)?.value
                Box(Modifier.padding(4.dp)) {
                    Row {
                        Box(Modifier.padding(top = 6.dp, start = 6.dp)) {
                            Avatar(avatarImage, info.initials ?: "?")
                        }
                        Spacer(Modifier.size(8.dp))
                        FlowRow {
                            Text(info.name, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.size(8.dp))
                            Text(info.userId.full, fontWeight = FontWeight.Light)
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            Text("Editing history:") // TODO: i18n
//            MessageHistory(edits.sortedBy { "${it.formattedDate} - ${it.formattedTime}" }.reversed())
            Spacer(Modifier.size(8.dp))
        }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = reactionFilterHeight),
            ) {
                Column(
                    Modifier.verticalScroll(scrollState),
                ) {
                    Text("Message read by:") // TODO: i18n
                    UserInteractions(userInteractions, interactionFilterByReaction.value)
                    Spacer(Modifier.size(8.dp))
                }
                VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                ReactionsFilter(
                    reactionCounts,
                    interactionFilterByReaction,
                    reactionFilterHeight,
                )
            }
        }
    }
}

@Composable
private fun MessageHistory(
    edits: List<TimelineElementHolderViewModel>,
) {
    Column {
        edits.forEach {
            it.element.collectAsState().value?.let { element ->
                Column(
                    Modifier.padding(end = 8.dp),
                ) {
                    with(DI.get<TimelineElementViewSelector>()) { createAsMessagePreview(it, element) }
                }
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun UserInteractions(
    userInteractions: List<MessageUserInteraction>,
    interactionFilterByReaction: ReactionKey?,
) {
    Column {
        userInteractions.filter {
            interactionFilterByReaction == null || it.reactions.contains(interactionFilterByReaction)
        }.sortedByDescending {
            it.reactions.firstOrNull()?.hashCode()
        }.forEach { interaction ->
            val avatarImage = interaction.userInfo.image?.collectAsState(null)?.value
            Box(Modifier.padding(4.dp)) {
                Row {
                    Box(Modifier.padding(top = 6.dp, start = 6.dp)) {
                        Avatar(avatarImage, interaction.userInfo.initials ?: "?")
                    }
                    Spacer(Modifier.size(8.dp))
                    // TODO: Use LazyColumn instead.
                    Column {
                        FlowRow {
                            Text(interaction.userInfo.name, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.size(8.dp))
                            Text(interaction.userInfo.userId.full, fontWeight = FontWeight.Light)
                        }
                        FlowRow {
                            interaction.reactions.forEach { reactionKey ->
                                Row {
                                    Text(
                                        reactionKey,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                                        modifier = Modifier.paddingFromBaseline(0.dp),
                                        maxLines = 1,
                                    )
                                    Spacer(Modifier.size(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionsFilter(
    reactionCounts: Map<ReactionKey, UInt>,
    interactionFilterByReaction: MutableState<ReactionKey?>,
    reactionFilterHeight: Dp,
) {
    if (reactionCounts.isEmpty()) return
    val i18n = DI.get<I18nView>()
    var selectedTabIndex by remember { mutableStateOf<Int?>(0) }
    val reactionList = reactionCounts.asSequence()
    val reactionListWithSum: List<Pair<String, UInt>> =
        listOf(i18n.commonAll() to reactionList.map { it.value }.sum()) +
                reactionList.map { it.toPair() }
    Column {
        val filterScrollState = rememberLazyListState()
        TabsRow(
            tabsCount = reactionListWithSum.size,
            selectedTabIndex = selectedTabIndex,
            scrollableState = filterScrollState,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            selectionIndicatorColor = MaterialTheme.colorScheme.primary,
            onTabClick = {
                selectedTabIndex = it
                if (it > 0) interactionFilterByReaction.value = reactionListWithSum[it].first
                else interactionFilterByReaction.value = null
            },
        ) { tabIndex, isSelected ->
            reactionListWithSum[tabIndex].let { (reaction, count) ->
                Row(
                    Modifier
                        .size(96.dp, reactionFilterHeight)
                        .align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        reaction,
                        style = MaterialTheme.typography.labelLarge.let {
                            if (tabIndex > 0) it.copy(fontSize = 24.sp) else it
                        }
                    )
                    Spacer(Modifier.size(2.dp))
                    Text("$count")
                }
            }
        }
        // TODO: use disappearing scrollbar?
        HorizontalScrollbar(Modifier, filterScrollState, false)
    }
}

@Composable
private fun TabsRow(
    tabsCount: Int,
    selectedTabIndex: Int?,
    modifier: Modifier = Modifier,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    selectionIndicatorColor: Color = TabRowDefaults.secondaryContentColor,
    scrollableState: LazyListState = rememberLazyListState(),
    onTabClick: (tabIndex: Int) -> Unit = {},
    onTabContent: @Composable (BoxScope.(tabIndex: Int, isSelected: Boolean) -> Unit),
) {
    val density = LocalDensity.current.density
    val coroutineScope = rememberCoroutineScope()
    val tabsWidthCache by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var scrollContainerWidth by remember { mutableStateOf(0) }
    val averageTabWidth = tabsWidthCache.values.average()
        .let { if (it.isFinite()) it else .0 }
        .fastRoundToInt()
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
    ) {
        LazyRow(
            modifier = Modifier
                .pointerEventWrapper(PointerEventType.Scroll) {
                    it.changes.firstOrNull()?.let { change ->
                        // TODO: Ensure that there's only one simultaneous scrolling/launch happening?
                        coroutineScope.launch {
                            scrollableState.animateScrollBy(change.scrollDelta.y * density * 16)
                        }
                    }
                }
                .onSizeChanged { scrollContainerWidth = it.width },
            state = scrollableState,
            userScrollEnabled = true,
            content = {
                items(count = tabsCount, key = { it }) { tabIndex ->
                    val isSelected = tabIndex == selectedTabIndex
                    Box(Modifier
                        .clickable {
                            // TODO: Ensure that there's only one simultaneous scrolling/launch happening?
                            coroutineScope.launch {
                                scrollableState.animateScrollToItem(
                                    tabIndex,
                                    (scrollContainerWidth - tabsWidthCache
                                        .getOrElse(tabIndex) { averageTabWidth }) / -2,
                                )
                            }
                            onTabClick(tabIndex)
                        }
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                val indicatorHeight = 8.dp.toPx()
                                if (isSelected) drawRect(
                                    color = selectionIndicatorColor,
                                    topLeft = Offset(0f, size.height - indicatorHeight),
                                    size = Size(size.width, indicatorHeight)
                                )
                            }
                        }
                        .background(
                            if (tabIndex % 2 == 0) containerColor
                            else contentColor.copy(alpha = .05f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .onSizeChanged { tabsWidthCache[tabIndex] = it.width }
                                .minimumInteractiveComponentSize(),
                            content = { onTabContent(tabIndex, isSelected) },
                        )
                    }
                }
            }
        )
    }
}
