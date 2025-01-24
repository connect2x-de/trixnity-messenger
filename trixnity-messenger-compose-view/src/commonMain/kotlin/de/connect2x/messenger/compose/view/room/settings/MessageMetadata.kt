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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.pointerEventWrapper
import de.connect2x.messenger.compose.view.room.timeline.DateStickyHeader
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.room.timeline.element.util.Tooltip
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageUserInteraction
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


// TODO: Move to utils?
/**
 * Helper to measure the visible vertical boundaries of a compose element and
 * calculate relative offsets to other elements.
 */
private data class VerticalBounds(
    val height: Dp,
    val localOffset: Dp,
    private val scrollTrigger: Int, // This ensures updates to be triggered whenever the element is moved.
    private val density: Density,
    private val coordinates: LayoutCoordinates? = null,
) {
    fun offsetRelativeTo(bounds: VerticalBounds): Dp? {
        val thisOffset = this.coordinates?.let { if (it.isAttached) it else null }?.positionInWindow()?.y
        val thatOffset = bounds.coordinates?.let { if (it.isAttached) it else null }?.positionInWindow()?.y
        return if (thisOffset != null && thatOffset != null) {
            (thisOffset - thatOffset).let { (it / density.density).dp }
        } else null
    }

    companion object {
        fun make(density: Density, coordinates: LayoutCoordinates? = null): VerticalBounds =
            VerticalBounds(
                height = calcHeight(density, coordinates),
                localOffset = calcLocalOffset(density, coordinates),
                scrollTrigger = coordinates?.let { if (it.isAttached) it else null }
                    ?.positionInWindow()?.y?.fastRoundToInt() ?: 0,
                density, coordinates,
            )

        private fun calcHeight(
            density: Density,
            coordinates: LayoutCoordinates?,
        ) = coordinates?.size?.height
            ?.let { (it / density.density).dp } ?: 0.dp

        private fun calcLocalOffset(
            density: Density,
            coordinates: LayoutCoordinates?,
        ) = coordinates?.let { if (it.isAttached) it else null }?.positionInParent()?.y
            ?.let { (it / density.density).dp } ?: 0.dp
    }
}

// TODO: Move to utils?
private fun Density.verticalBounds(coordinates: LayoutCoordinates? = null): VerticalBounds {
    return VerticalBounds.make(this, coordinates)
}

// TODO: Move to utils or remove
private fun Modifier.drawLayoutRulers(): Modifier =
    this.drawWithContent {
        val strokeWidth = Dp.Hairline.toPx()

        fun DrawScope.debugBounds(inset: Dp, color: Color = Color.Red, dashed: Boolean = false) {
            drawPath(
                color = color,
                path = Path().apply {
                    inset.toPx().let { inset ->
                        // Keep starting new lines so dashes can be used for measuring.
                        moveTo(inset, inset)
                        lineTo(size.width - inset, inset)
                        moveTo(inset, inset)
                        lineTo(inset, size.height - inset)
                        moveTo(size.width - inset, inset)
                        lineTo(size.width - inset, size.height - inset)
                        moveTo(inset, size.height - inset)
                        lineTo(size.width - inset, size.height - inset)
                    }
                },
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(
                        floatArrayOf(inset.toPx(), inset.toPx()),
                    ) else null,
                ),
            )
        }

        fun DrawScope.debugHorizontalRulerMark(offset: Dp, color: Color = Color.Red) {
            drawPath(
                color = color,
                path = Path().apply {
                    offset.toPx().let { offset ->
                        moveTo(0f, offset)
                        lineTo(size.width, offset)
                    }
                },
                style = Stroke(width = strokeWidth),
            )
        }

        fun DrawScope.debugVerticalRulerMark(offset: Dp, color: Color = Color.Red) {
            drawPath(
                color = color,
                path = Path().apply {
                    offset.toPx().let { offset ->
                        moveTo(offset, 0f)
                        lineTo(offset, size.height)
                    }
                },
                style = Stroke(width = strokeWidth),
            )
        }

        drawContent()
        debugBounds(10.dp, dashed = true)
        debugBounds(0.dp)

        val rulerSpacing = 100.dp
        for (i in 1..(size.height / rulerSpacing.toPx()).fastRoundToInt())
            debugHorizontalRulerMark((i * rulerSpacing.toPx()).toDp())
        for (i in 1..(size.width / rulerSpacing.toPx()).fastRoundToInt())
            debugVerticalRulerMark((i * rulerSpacing.toPx()).toDp())
    }

// TODO: Move to utils?
/**
 * This works almost like writing and using `var value = remember { mutableStateOf(T) }`
 * but instead of invoking a UI redraw on each update, this util provides a mechanism
 * to throttle the update propagation and thus reduces unnecessary updates to the composable.
 * E.g. when a scrolling list also performs some form of more complex layouting.
 */
@OptIn(FlowPreview::class)
private class ThrottledMutableState<T> private constructor(value: MutableState<T>, pending: MutableState<T>) {
    private val _value: MutableState<T> = value
    private var _pending by pending

    /**
     * This field should be handled as the read-only accessor to the current value!
     * Attempts at altering it, e.g. performing mutable collection operations
     * may result in strange and difficult to debug behavior.
     */
    fun get(): T = _value.value

    /**
     * This method should be used if the intent is to update the value as a whole.
     */
    fun set(value: T) {
        _pending = value
    }

    /**
     * This method is intended for performing modifications on a complex object which then
     * gets propagated to the UI eventually. E.g. modifying a mutable collection
     * without needing to copy it.
     */
    fun modify(modifyFn: (T) -> Unit) = modifyFn(_pending)

    companion object {
        @Composable
        operator fun <T> invoke(startValue: () -> T): ThrottledMutableState<T> {
            val state = ThrottledMutableState(
                remember { mutableStateOf(startValue()) },
                remember { mutableStateOf(startValue()) },
            )
            LaunchedEffect(Unit) {
                snapshotFlow { state._pending }
                    .conflate().sample(33.milliseconds)
                    .collect { state._value.value = it }
            }
            return state
        }
    }
}

interface MessageMetadataView {
    @Composable
    fun create(
        viewModel: MessageMetadataViewModel,
        stackPosition: Int, isSinglePane: Boolean,
    )
}

@Composable
fun MessageMetadata(
    viewModel: MessageMetadataViewModel,
    stackPosition: Int, isSinglePane: Boolean,
) {
    DI.get<MessageMetadataView>().create(viewModel, stackPosition, isSinglePane)
}

private val userItemHeight = 48.dp
private val filterBarHeight = 48.dp

class MessageMetadataViewImpl : MessageMetadataView {
    @Composable
    override fun create(
        viewModel: MessageMetadataViewModel,
        stackPosition: Int, isSinglePane: Boolean,
    ) {
        val message = viewModel.messagePreview.collectAsState().value
        val reactionCounts = viewModel.reactionCounts.collectAsState().value
        val userInteractions = viewModel.userInteractions.collectAsState().value
        val error = viewModel.error.collectAsState().value

        val i18n = DI.get<I18nView>()
        val density = LocalDensity.current
        val smallSpacing = 10.dp
        val largeSpacing = 20.dp

        val scrollState = rememberScrollState()
        val paneBounds = ThrottledMutableState { density.verticalBounds() }
        val interactionsBounds = ThrottledMutableState { density.verticalBounds() }
        val interactionFilterByReaction = remember { mutableStateOf<ReactionKey?>(null) }
        val interactionsOffset = interactionsBounds.get().offsetRelativeTo(paneBounds.get()) ?: 0.dp

        val filterOffset = min(
            interactionsOffset + userItemHeight * userInteractions.size + smallSpacing,
            paneBounds.get().height - filterBarHeight,
        )
        val isInteractionsVisible = filterOffset >= interactionsOffset
        val isFilterVisible = isInteractionsVisible && reactionCounts.isNotEmpty()

        ExtrasPaneHeader(
            i18n.messageMetadataTitle(),
            error,
            { viewModel.back() },
            if (isSinglePane || stackPosition > 2) BACK else CLOSE,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { paneBounds.set(density.verticalBounds(it)) }
            ) {
                Box(
                    Modifier
                        .height(paneBounds.get().height - (if (isFilterVisible) filterBarHeight else 0.dp))
                ) {
                    Column(
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.size(largeSpacing))
                        Text(
                            text = i18n.messageMetadataSender(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        UserInfo(viewModel.senderInfo)
                        Spacer(Modifier.size(smallSpacing))
                        Text(
                            text = i18n.messageMetadataMessage(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.size(smallSpacing))
                        MessageContents(message)
                        Spacer(Modifier.size(smallSpacing))
                        HorizontalDivider()
                        Spacer(Modifier.size(largeSpacing))
                        Text(
                            text = i18n.messageMetadataReadersAndReactions(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.size(smallSpacing))
                        Box(
                            Modifier
                                .onGloballyPositioned { interactionsBounds.set(density.verticalBounds(it)) }
                        ) {
                            if (isInteractionsVisible) {
                                // TODO: Move this into the viewmodel?
                                val interactions = (interactionFilterByReaction.value
                                    ?.let { filter -> userInteractions.filter { it.reactions.contains(filter) } }
                                    ?: userInteractions).sortedByDescending { it.reactions.firstOrNull()?.hashCode() }
                                if (interactions.isEmpty() && interactionFilterByReaction.value != null) {
                                    // Reset the filter if it's set but yields no results.
                                    interactionFilterByReaction.value = null
                                    // TODO: Add state that will trigger the filter row to scroll back to its beginning.
                                }
                                UserInteractions(
                                    interactions = interactions,
                                    paneBounds = paneBounds.get(),
                                    visibleListOffset = interactionsOffset,
                                    visibleListHeight = filterOffset - interactionsOffset,
                                )
                            }
                            // Provide some space so the user interactions list
                            // can appear while scrolling down.
                            else Spacer(Modifier.size(500.dp))
                        }
                        Spacer(Modifier.size(smallSpacing))
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
    userInfoFlow: StateFlow<UserInfoElement?>,
    reactions: Set<ReactionKey> = setOf(),
) {
    val i18n = DI.get<I18nView>()
    val userInfo = userInfoFlow.collectAsState().value
        ?: return Box(
            Modifier
                .fillMaxWidth()
                .height(userItemHeight)
        )
    val compiledReactionsList: String = reactions.joinToString(" ")
    val hasReactions = compiledReactionsList.isNotEmpty()
    val tooltipText = buildString {
        append("${userInfo.name}: ${userInfo.userId.full}")
        if (hasReactions) {
            appendLine()
            append(i18n.messageMetadataUserInfoTooltipReactions(compiledReactionsList))
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
                    // Noop for hover effect.
                    // TODO: Open user profile.
                }
        ) {
            Box(
                Modifier
                    .align(CenterVertically)
                    .padding(start = 8.dp)
            ) {
                // Loading and processing the image data immediately makes the scrolling lag.
                // Thus it's delayed by just enough to not be bothersome but long enough that
                // the corresponding view might already have left the building where it nor any
                // of its coroutines care anymore about actually doing anything with the data.
                // Tl;dr: the list navigation is much smoother as a result.
                val imageFlow = userInfo.image?.onStart { delay((250..500).random().milliseconds) }
                Avatar(userInfo.imageUrl, imageFlow, userInfo.initials ?: "?")
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
private fun MessageContents(
    messageHolder: TimelineElementHolderViewModel?,
) {
    val i18n = DI.get<I18nView>()
    messageHolder?.let { holder ->
        DateStickyHeader(messageHolder.formattedDate)
        holder.element.collectAsState().value?.let { element ->
            Column(
                Modifier.padding(end = 8.dp),
            ) {
                with(DI.get<TimelineElementViewSelector>()) {
                    createAsMessagePreview(holder, element)
                }
                Spacer(Modifier.size(5.dp))
            }
            Spacer(Modifier.size(10.dp))
        }
    }
}

@Composable
private fun UserInteractions(
    modifier: Modifier = Modifier,
    interactions: List<MessageUserInteraction>,
    paneBounds: VerticalBounds,
    visibleListOffset: Dp,
    visibleListHeight: Dp,
) {
    val i18n = DI.get<I18nView>()
    val density = LocalDensity.current
    val itemBounds = ThrottledMutableState<MutableMap<Int, VerticalBounds>> { mutableMapOf() }
    Column(modifier) {
        if (interactions.isEmpty()) Text(
            i18n.messageMetadataReadersAndReactionsNone(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .height(userItemHeight * 2)
                .align(CenterHorizontally)
                .paddingFromBaseline(0.dp)
                .padding(start = 10.dp),
        )
        else interactions.forEachIndexed { index, interaction ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .height(userItemHeight)
                    .onGloballyPositioned {
                        itemBounds.modify { value -> value[index] = density.verticalBounds(it) }
                    }
            ) {
                // TODO: Since the item views are all the same height, the list and visibility filter can be optimized more.
                val showItem = itemBounds.get()[index]
                    ?.let {
                        val itemOffset = it.offsetRelativeTo(paneBounds)
                        val cullOffset = userItemHeight * 1.5f
                        visibleListHeight > 0.dp && it.height > 0.dp && itemOffset != null
                                && itemOffset < visibleListOffset + visibleListHeight + cullOffset
                                && itemOffset > -cullOffset
                    } == true
                if (showItem) UserInfo(interaction.userInfo, interaction.reactions)
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
            edgePadding = 20.dp,
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
