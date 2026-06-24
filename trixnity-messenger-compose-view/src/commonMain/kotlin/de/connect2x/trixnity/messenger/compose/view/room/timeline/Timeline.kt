package de.connect2x.trixnity.messenger.compose.view.room.timeline

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementHolder
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.compose.view.util.waitForElementWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.throttleFirst
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.room.timeline.TimelineKt")

interface TimelineView {
    @Composable fun ColumnScope.create(timelineViewModel: TimelineViewModel)
}

@Composable
fun ColumnScope.Timeline(timelineViewModel: TimelineViewModel) {
    with(DI.get<TimelineView>()) { create(timelineViewModel) }
}

private const val additionalEndPadding = 8

sealed interface TimelineViewElement {
    val key: String

    data class Date(val viewModel: BaseTimelineElementHolderViewModel) : TimelineViewElement {
        val formattedDate = viewModel.formattedDate
        override val key = "date-${viewModel.key}"
    }

    data class Element(val viewModel: BaseTimelineElementHolderViewModel) : TimelineViewElement {
        override val key = viewModel.key
    }
}

class TimelineViewImpl : TimelineView {
    @Composable
    override fun ColumnScope.create(timelineViewModel: TimelineViewModel) {
        val i18n = DI.get<I18nView>()

        val timelineViewElements = rememberTimelineViewElements(timelineViewModel)
        val isTimelineLoading = timelineViewElements.value.isEmpty()
        val error = timelineViewModel.error.collectAsState()
        val draggedFile = timelineViewModel.draggedFile.collectAsState()

        val focusManager = LocalFocusManager.current

        val showTypingIndicator =
            remember { timelineViewModel.canLoadAfter.throttleFirst(300.milliseconds).map { it == false } }
                .collectAsState(false)

        val finishedScrollTo = remember { mutableStateOf<String?>(null) }
        val initialFirstVisibleItemIndex =
            getInitialFirstVisibleItemIndex(
                    timelineViewModel,
                    timelineViewElements,
                    showTypingIndicator,
                    finishedScrollTo,
                )
                .value
        Box(modifier = Modifier.weight(1.0f, fill = true)) {
            if (isTimelineLoading || initialFirstVisibleItemIndex == null) {
                Box(Modifier.fillMaxSize()) { LoadingSpinner(Modifier.align(Alignment.Center)) }
            } else {
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex)
                val scrollTo = remember { timelineViewModel.scrollTo.drop(1) }.collectAsState(null)
                LaunchedEffect(scrollTo.value, timelineViewElements.value) {
                    if (scrollTo.value != null) {
                        val currentShowTypingIndicator = showTypingIndicator.value
                        val index = timelineViewElements.value.indexOfFirst { it.key == scrollTo.value }
                        if (index >= 0) {
                            log.debug { "scrolling to $scrollTo (index=$index)" }
                            listState.animateScrollToItem(
                                when {
                                    index == 0 && currentShowTypingIndicator -> 0
                                    currentShowTypingIndicator -> index + 1
                                    else -> index
                                }
                            )
                        }
                        finishedScrollTo.value = scrollTo.value
                    }
                }

                val visibleItems = rememberVisibleItems(listState)
                updateVisibleItems(timelineViewModel, visibleItems, timelineViewElements, finishedScrollTo)

                BoxWithConstraints(
                    // On touchscreen devices, tapping the timeline will close the keyboard.
                    Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus(true) }) }
                ) {
                    error.value?.let { error ->
                        ThemedModalDialog({ timelineViewModel.errorDismiss() }) {
                            ModalDialogHeader { Text(i18n.anErrorHasOccurred()) }
                            ModalDialogContent { Text(error) }
                            ModalDialogFooter {
                                ThemedButton(
                                    style = MaterialTheme.components.primaryButton,
                                    onClick = { timelineViewModel.errorDismiss() },
                                ) {
                                    Text(i18n.actionOk())
                                }
                            }
                        }
                    }
                    Box(Modifier.padding(vertical = 2.dp)) {
                        val canScrollToEnd = remember {
                            derivedStateOf {
                                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                lastVisibleItem != null && !(lastVisibleItem.index == 0 && lastVisibleItem.offset == 0)
                            }
                        }
                        Box {
                            val focusedItem = remember { mutableStateOf(timelineViewElements.value.firstOrNull()?.key) }

                            LazyColumn(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .rovingFocusContainer(
                                            listState = listState,
                                            focusedItem = focusedItem,
                                            ignoredKeys = listOf("typing"),
                                        )
                                        .semantics {
                                            collectionInfo = CollectionInfo(1, timelineViewElements.value.size)
                                            liveRegion = LiveRegionMode.Polite
                                        },
                                contentPadding =
                                    PaddingValues(
                                        top = 10.dp,
                                        bottom = 10.dp,
                                        start =
                                            if (this@BoxWithConstraints.maxWidth.value > 1000)
                                                (0.5 * (this@BoxWithConstraints.maxWidth.value - 1000) + 10).dp
                                            else 10.dp,
                                        end =
                                            if (this@BoxWithConstraints.maxWidth.value > 1000)
                                                (0.5 * (this@BoxWithConstraints.maxWidth.value - 1000) +
                                                        (10 + additionalEndPadding))
                                                    .dp
                                            else 18.dp, // 10 + 8, since we cannot add a padding or Spacer at the end
                                    ),
                                state = listState,
                                reverseLayout = true,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                log.trace { "rendering timeline elements" }
                                if (showTypingIndicator.value) {
                                    item(key = "typing", contentType = "typing") { TypingIndicator(timelineViewModel) }
                                }
                                itemsIndexed(
                                    items = timelineViewElements.value,
                                    key = { _, timelineViewElement -> timelineViewElement.key },
                                    contentType = { _, timelineViewElement ->
                                        when (timelineViewElement) {
                                            is TimelineViewElement.Date -> "date"
                                            is TimelineViewElement.Element -> "element"
                                        }
                                    },
                                ) { index, timelineViewElement ->
                                    Box(
                                        Modifier.rovingFocusItem(
                                                isFocused = { timelineViewElement.key == focusedItem.value },
                                                onFocus = { focusedItem.value = timelineViewElement.key },
                                            )
                                            .animateItem()
                                            .animateContentSize()
                                    ) {
                                        when (timelineViewElement) {
                                            is TimelineViewElement.Date -> {
                                                DateStickyHeader(
                                                    date = timelineViewElement.formattedDate,
                                                    focusable = true,
                                                )
                                            }

                                            is TimelineViewElement.Element -> {
                                                val viewModel = timelineViewElement.viewModel
                                                // if an empty timeline-event is marked as the focusedItem we cannot tab
                                                // into the
                                                // timeline due to it not being focusable so we initially skip all
                                                // empties
                                                if (
                                                    focusedItem.value == timelineViewElement.key &&
                                                        viewModel.element.value is TimelineElementViewModel.Empty
                                                ) {
                                                    focusedItem.value =
                                                        timelineViewElements.value.getOrNull(index + 1)?.key
                                                }

                                                TimelineElementHolder(viewModel, index)
                                            }
                                        }
                                    }
                                }
                            }
                            ListDateHeader(visibleItems, timelineViewElements, show = listState.canScrollForward)
                            ScrollToEndButton(timelineViewModel, canScrollToEnd)
                            draggedFile.value?.let { draggedFile ->
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Circle, "", Modifier.size(100.dp), tint = Color.Gray)
                                            Icon(
                                                MaterialTheme.messengerIcons.attachFile,
                                                i18n.timelineSendFile(),
                                                Modifier.size(60.dp),
                                            )
                                        }
                                        Text(draggedFile.toString(), style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                            }

                            ReportMessageSwitch(timelineViewModel)
                        }

                        VerticalScrollbar(Modifier.align(Alignment.CenterEnd), listState, reverseLayout = true)
                    }
                }
            }
        }
    }
}

@Composable
fun rememberTimelineViewElements(timelineViewModel: TimelineViewModel): State<List<TimelineViewElement>> {
    val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
    val timelineElementHolderViewModels = remember {
        mutableStateOf<List<BaseTimelineElementHolderViewModel>>(listOf())
    }

    LaunchedEffect(Unit) {
        var elementsFromLastCollect = setOf<BaseTimelineElementHolderViewModel>()
        timelineViewModel.elements.collect { elements ->
            log.trace { "wait for elements to be ready" }
            withContext(Dispatchers.Default) {
                (elements - elementsFromLastCollect).forEach { element ->
                    launch { waitForElementWithTimeout(timelineElementViewSelector, element) }
                }
            }
            log.trace { "finished wait for elements to be ready" }
            elementsFromLastCollect = elements.toSet()
            timelineElementHolderViewModels.value = elements
        }
    }
    return remember {
        derivedStateOf {
            val vms = timelineElementHolderViewModels.value
            buildList(vms.size) {
                    var lastDate: String? = null
                    for (index in vms.indices) {
                        val vm = vms[index]
                        when {
                            lastDate == vm.formattedDate -> add(TimelineViewElement.Element(vm))
                            vm.element.value is TimelineElementViewModel.Empty -> add(TimelineViewElement.Element(vm))
                            else -> {
                                add(TimelineViewElement.Date(vm))
                                add(TimelineViewElement.Element(vm))
                                lastDate = vm.formattedDate
                            }
                        }
                    }
                }
                .asReversed()
        }
    }
}

@Composable
fun ReportMessageSwitch(timelineViewModel: TimelineViewModel) {
    Children(stack = timelineViewModel.reportMessageStack, animation = stackAnimation(fade())) {
        when (val child = it.instance) {
            ReportMessageRouter.Wrapper.None -> Unit
            is ReportMessageRouter.Wrapper.ReportMessageView -> {
                MessageReport(child.viewModel)
            }
        }.let {}
    }
}

@Composable
fun rememberVisibleItems(listState: LazyListState): State<Pair<String, String>?> {
    return remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo.sortedBy { it.index }
            val lastVisibleItem = visibleItems.firstOrNull()
            val lastIsTyping = (lastVisibleItem?.key as? String) == "typing"
            val lastVisibleIndexOffset = if (lastIsTyping) 1 else 0
            val lastVisible =
                visibleItems
                    .firstOrNull {
                        val key = it.key as? String
                        if (key == null || key.startsWith("date-") || key.startsWith("typing")) return@firstOrNull false
                        // we want the last element in the timeline only if it is completely visible (compose considers
                        // even
                        // 1 pixel of an element as "in view" which is not what we want)
                        it.index == lastVisibleIndexOffset && (it.offset == 0 || lastIsTyping) ||
                            it.index > lastVisibleIndexOffset
                    }
                    ?.key as? String
            val firstVisible =
                visibleItems
                    .lastOrNull {
                        val key = it.key as? String
                        if (key == null || key.startsWith("date-") || key.startsWith("typing")) return@lastOrNull false
                        true
                    }
                    ?.key as? String
            if (firstVisible != null && lastVisible != null) {
                firstVisible to lastVisible
            } else null
        }
    }
}

@Composable
fun getInitialFirstVisibleItemIndex(
    timelineViewModel: TimelineViewModel,
    timelineViewElements: State<List<TimelineViewElement>>,
    showTypingIndicator: State<Boolean>,
    finishedScrollTo: MutableState<String?>,
): State<Int?> {
    val initialFirstVisibleItemIndex = remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val scrollTo = timelineViewModel.scrollTo.first()
        val currentTimelineViewElements = snapshotFlow { timelineViewElements.value }.first { it.isNotEmpty() }
        val currentShowTypingIndicator = showTypingIndicator.value
        initialFirstVisibleItemIndex.value =
            currentTimelineViewElements
                .indexOfLast { element -> element is TimelineViewElement.Element && element.viewModel.key == scrollTo }
                .coerceAtLeast(0)
                .let { index ->
                    when {
                        index == 0 && currentShowTypingIndicator -> 0
                        currentShowTypingIndicator -> index + 1
                        else -> index
                    }
                }
        finishedScrollTo.value = scrollTo
    }

    return initialFirstVisibleItemIndex
}

@Composable
fun updateVisibleItems(
    timelineViewModel: TimelineViewModel,
    visibleItems: State<Pair<String, String>?>,
    timelineViewElements: State<List<TimelineViewElement>>,
    finishedScrollTo: MutableState<String?>,
) {
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val viewState = remember {
        derivedStateOf {
            val timelineViewElementsWithoutDate =
                timelineViewElements.value.filterIsInstance<TimelineViewElement.Element>()
            visibleItems.value?.let { visibleItemsValue ->
                TimelineViewModel.ViewState(
                    firstVisibleElement = visibleItemsValue.first,
                    lastVisibleElement = visibleItemsValue.second,
                    firstLoadedElement = timelineViewElementsWithoutDate.last().viewModel.key,
                    lastLoadedElement = timelineViewElementsWithoutDate.first().viewModel.key,
                    timelineIsFocused = lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED),
                    finishedScrollTo = finishedScrollTo.value,
                )
            }
        }
    }
    LaunchedEffect(viewState.value) {
        log.trace { "viewState: ${viewState.value}" }
        viewState.value?.let { timelineViewModel.setViewState(it) }
        finishedScrollTo.value = null
    }
}

@Composable
fun ListDateHeader(
    visible: State<Pair<String, String>?>,
    timelineViewElements: State<List<TimelineViewElement>>,
    show: Boolean,
) {
    if (show) {
        val timestamp = remember {
            derivedStateOf {
                visible.value?.first?.let { lastEventId ->
                    timelineViewElements.value
                        .filterIsInstance<TimelineViewElement.Element>()
                        .find { it.viewModel.key == lastEventId }
                        ?.viewModel
                        ?.formattedDate
                }
            }
        }
        Box(Modifier.padding(end = additionalEndPadding.dp)) {
            timestamp.value?.let { DateStickyHeader(it, focusable = false) }
        }
    }
}
