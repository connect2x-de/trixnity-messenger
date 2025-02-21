package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.common.LargeSpacer
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.pointerEventWrapper
import de.connect2x.messenger.compose.view.room.timeline.DateStickyHeader
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.room.timeline.element.util.Tooltip
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageUserInteraction
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration.Companion.milliseconds


private val log = KotlinLogging.logger {}

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

        val element = viewModel.element.collectAsState().value
        val sender = element?.sender?.collectAsState()?.value
        val reactions = element?.reactions?.collectAsState()?.value
        val readers = element?.readers?.collectAsState()?.value
        if (element == null || reactions == null || readers == null || sender == null) {
            LoadingSpinner(Modifier.fillMaxSize())
        } else {
            val scrollState = rememberScrollState()
            val interactionFilterByReaction = remember { mutableStateOf<String?>(null) }
            val hasReadersOrReactions = readers.isNotEmpty() || reactions.all.isNotEmpty()
            val isFilterVisible = reactions.byReaction.size > 1

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
                            .verticalScroll(scrollState)
                            .padding(PaddingValues(vertical = 0.dp, horizontal = 20.dp))
                    ) {
                        LargeSpacer()
                        Text(
                            text = i18n.timelineElementMetadataSender(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        UserInfo(
                            sender,
                            onOpenUserProfile = viewModel::openUserProfile,
                        )
                        SmallSpacer()
                        Text(
                            text = i18n.timelineElementMetadataMessage(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SmallSpacer()
                        MessageContent(element)
                        SmallSpacer()
                        HorizontalDivider()
                        LargeSpacer()
                        Text(
                            text = i18n.timelineElementMetadataReadersAndReactions(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SmallSpacer()
                        if (hasReadersOrReactions) {
                            // TODO: Move this into the viewmodel?
                            val interactions = interactionFilterByReaction.value.let { filter ->
                                if (filter != null) userInteractions.filter { it.reactions.contains(filter) }
                                else userInteractions
                            }.sortedByDescending { it.reactions.firstOrNull()?.hashCode() }
                            log.debug { "interactions:${userInteractions.size} filtered:${interactions.size}" }
                            if (interactions.isEmpty() && interactionFilterByReaction.value != null) {
                                // Reset the filter if it's set but yields no results.
                                interactionFilterByReaction.value = null
                                // TODO: Add state that will trigger the filter row to scroll back to its beginning.
                            }
                            key(interactions) {
                                UserInteractions(
                                    interactions = interactions,
                                    visibleListOffset = interactionsOffset,
                                    visibleListHeight = filterOffset - interactionsOffset,
                                    onOpenUserProfile = viewModel::openUserProfile,
                                )
                            }
                        } else {
                            Text(
                                text = i18n.timelineElementMetadataReadersAndReactionsNone(),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        SmallSpacer()
                    }
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
                }
                if (isInteractionsVisible) ReactionsFilter(
                    Modifier.offset(y = filterOffset),
                    reactionCounts,
                    interactionFilterByReaction,
                )
            }
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
                .height(userItemHeight)
                .clickable {
                    onOpenUserProfile(userInfo.userId)
                }
        ) {
            Box(
                Modifier
                    .align(CenterVertically)
                    .padding(start = 8.dp)
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
private fun MessageContent(
    messageHolder: TimelineElementHolderViewModel?,
) {
    messageHolder?.let { holder ->
        DateStickyHeader(messageHolder.formattedDate)
        holder.element.collectAsState().value?.let { element ->
            Column(
                Modifier.padding(end = 8.dp),
            ) {
                DI.get<TimelineElementViewSelector>().createAsPreview(holder, element)
            }
            SmallSpacer()
        }
    }
}

@Composable
private fun UserInteractions(
    modifier: Modifier = Modifier,
    interactions: List<MessageUserInteraction>,
    visibleListOffset: Dp,
    visibleListHeight: Dp,
    onOpenUserProfile: (UserId) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    Column(modifier) {
        if (interactions.isEmpty()) Text(
            i18n.timelineElementMetadataReadersAndReactionsNone(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .height(filterBarHeight + largeSpacing)
                .align(CenterHorizontally)
                .paddingFromBaseline(0.dp)
                .padding(start = smallSpacing),
        )
        else LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .height(userItemHeight * interactions.size),
            userScrollEnabled = false,
            content = {
                items(
                    count = interactions.size,
                    key = { interactions[it].userId },
                ) {
                    val interaction = interactions[it]
                    UserInfo(interaction.userInfo, interaction.reactions, onOpenUserProfile)
                }
            }
        )

        /**
         * Alternative implementation if the lazy column is rendering too many items since this one avoids drawing rows and loading profile images by visibility.
         * Both variants seem to work fine so pick the best.
         */
//        else FlatLazyColumn(
//            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
//            visibleListOffset = visibleListOffset,
//            visibleListHeight = visibleListHeight,
//            itemHeight = userItemHeight,
//            itemCount = interactions.size,
//            itemKey = { interactions[it].userId },
//            itemContent = {
//                val interaction = interactions[it]
//                UserInfo(interaction.userInfo, interaction.reactions, onOpenUserProfile)
//            }
//        )
    }
}

@Composable
private fun <K> FlatLazyColumn(
    modifier: Modifier = Modifier,
    visibleListOffset: Dp,
    visibleListHeight: Dp,
    itemHeight: Dp,
    itemCount: Int,
    itemKey: (index: Int) -> K,
    itemContent: @Composable BoxScope.(index: Int) -> Unit,
) {
    val cullBuffer = 1f
    val visibleItemIndexFirst = (-visibleListOffset.value / itemHeight.value - cullBuffer)
        .fastRoundToInt().coerceIn(0, itemCount - 1)
    val visibleItemIndexLast = (visibleItemIndexFirst +
            (visibleListHeight.value / itemHeight.value + cullBuffer)
                .fastRoundToInt()).coerceIn(0, itemCount - 1)
    val visibleItemsRange = (visibleItemIndexFirst..visibleItemIndexLast)
    Column(
        Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        repeat(itemCount) { index ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
            ) {
                val showItem = visibleItemsRange.contains(index)
                if (showItem) key(itemKey(index)) {
                    itemContent(index)
                }
            }
        }
    }
}

@Composable
private fun ReactionsFilter(
    modifier: Modifier = Modifier,
    reactionCounts: Map<ReactionKey, UInt>,
    interactionFilterByReaction: MutableState<ReactionKey?>,
) {
    if (reactionCounts.isEmpty()) return
    val i18n = DI.get<I18nView>()
    var selectedTabIndex = 0
    val buttonWidth = 64.dp
    val reactionList = reactionCounts.asSequence()
    // TODO: Move this into the viewmodel?
    val reactionListWithSum: List<Pair<String, UInt>> =
        listOf(i18n.commonAll() to reactionList.map { it.value }.sum()) +
                reactionList.mapIndexed { index, reactionCount ->
                    if (reactionCount.key == interactionFilterByReaction.value)
                        selectedTabIndex = index + 1
                    reactionCount.toPair()
                }
    val filterScrollState = rememberLazyListState()
    Box(modifier.height(filterBarHeight)) {
        TabsRow(
            tabsCount = reactionListWithSum.size,
            selectedTabIndex = selectedTabIndex,
            scrollableState = filterScrollState,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            selectionIndicatorColor = MaterialTheme.colorScheme.primary,
            edgePadding = contentPadding,
            onTabClick = {
                if (it > 0) interactionFilterByReaction.value =
                    reactionListWithSum[it].first
                else interactionFilterByReaction.value = null
            },
        ) { tabIndex, _ ->
            reactionListWithSum[tabIndex].let { (reaction, count) ->
                Row(
                    Modifier
                        .width(buttonWidth)
                        .fillMaxHeight()
                        .background(with(MaterialTheme.colorScheme) {
                            if (tabIndex % 2 == 0) surface
                            else onSurface.copy(alpha = 0.063f)
                                .compositeOver(surface)
                        })
                        .align(Center),
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        reaction,
                        style = MaterialTheme.typography.labelLarge.let {
                            if (tabIndex > 0) it.copy(fontSize = 16.sp) else it
                        }
                    )
                    Spacer(Modifier.size(2.dp))
                    Text("$count")
                }
            }
        }
        HorizontalDivider(Modifier.align(TopCenter))
        // TODO: Use disappearing scrollbar?
        HorizontalScrollbar(Modifier.align(BottomCenter), filterScrollState, false)
    }
}

// TODO: Move to separate file?
@Composable
private fun TabsRow(
    tabsCount: Int,
    selectedTabIndex: Int?,
    modifier: Modifier = Modifier,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    selectionIndicatorColor: Color = TabRowDefaults.secondaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
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
            contentPadding = PaddingValues(horizontal = edgePadding),
            content = {
                items(count = tabsCount, key = { it }) { tabIndex ->
                    val isSelected = tabIndex == selectedTabIndex
                    Box(
                        Modifier
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
                    ) {
                        Box(
                            modifier = Modifier
                                .align(TopCenter)
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
