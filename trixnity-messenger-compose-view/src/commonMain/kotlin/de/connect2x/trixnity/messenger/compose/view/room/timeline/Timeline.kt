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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.room.timeline.TimelineKt")

interface TimelineView {
    @Composable
    fun ColumnScope.create(timelineViewModel: TimelineViewModel)
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
        var scrollTo by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            timelineViewModel.scrollTo.drop(1).collect { scrollTo = it }
        }

        val timelineViewElements = rememberTimelineViewElements(timelineViewModel)
        val isTimelineLoading = timelineViewElements.value.isEmpty()
        val error = timelineViewModel.error.collectAsState()
        val draggedFile = timelineViewModel.draggedFile.collectAsState()

        val focusManager = LocalFocusManager.current

        val showTypingIndicator =
            remember { timelineViewModel.canLoadAfter.throttleFirst(300.milliseconds) }
                .collectAsState(false).value == false

        val initialFirstVisibleItemIndex =
            getInitialFirstVisibleItemIndex(timelineViewModel, timelineViewElements.value, showTypingIndicator)
        Box(modifier = Modifier.weight(1.0f, fill = true)) {
            if (isTimelineLoading || initialFirstVisibleItemIndex == null) {
                Box(Modifier.fillMaxSize()) {
                    LoadingSpinner(Modifier.align(Alignment.Center))
                }
            } else {
                val listState =
                    rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex)

                LaunchedEffect(scrollTo, timelineViewElements.value, showTypingIndicator) {
                    if (scrollTo != null) {
                        val index = withTimeoutOrNull(5.seconds) {
                            timelineViewElements.value.indexOfFirst { it.key == scrollTo }
                        } ?: -1
                        if (index >= 0) {
                            log.debug { "scrolling to $scrollTo (index=$index)" }
                            listState.animateScrollToItem(
                                when {
                                    index == 0 && showTypingIndicator -> 0
                                    showTypingIndicator -> index + 1
                                    else -> index
                                }
                            )
                            scrollTo = null
                        }
                    }
                }

                val visibleItems = rememberVisibleItems(listState)
                updateVisibleItems(timelineViewModel, visibleItems, timelineViewElements)

                BoxWithConstraints(
                    // On touchscreen devices, tapping the timeline will close the keyboard.
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus(true)
                            })
                        }
                ) {
                    error.value?.let { error ->
                        ThemedModalDialog({ timelineViewModel.errorDismiss() }) {
                            ModalDialogHeader {
                                Text(i18n.anErrorHasOccurred())
                            }
                            ModalDialogContent {
                                Text(error)
                            }
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
                            val focusedItem = remember {
                                mutableStateOf(
                                    timelineViewElements.value.firstOrNull()?.key
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rovingFocusContainer(
                                        listState = listState,
                                        focusedItem = focusedItem
                                    ).semantics {
                                        collectionInfo = CollectionInfo(1, timelineViewElements.value.size)
                                        liveRegion = LiveRegionMode.Polite
                                    },
                                contentPadding = PaddingValues(
                                    top = 10.dp,
                                    bottom = 10.dp,
                                    start = if (this@BoxWithConstraints.maxWidth.value > 1000)
                                        (0.5 * (this@BoxWithConstraints.maxWidth.value - 1000) + 10).dp else 10.dp,
                                    end = if (this@BoxWithConstraints.maxWidth.value > 1000)
                                        (0.5 * (this@BoxWithConstraints.maxWidth.value - 1000) + (10 + additionalEndPadding)).dp else 18.dp // 10 + 8, since we cannot add a padding or Spacer at the end
                                ),
                                state = listState,
                                reverseLayout = true,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                log.trace { "rendering timeline elements" }
                                if (showTypingIndicator) {
                                    item(key = "typing", contentType = "typing") {
                                        TypingIndicator(timelineViewModel)
                                    }
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
                                        Modifier
                                            .rovingFocusItem(
                                                isFocused = { timelineViewElement.key == focusedItem.value },
                                                onFocus = { focusedItem.value = timelineViewElement.key }
                                            )
                                            .animateItem()
                                            .animateContentSize()
                                    ) {
                                        when (timelineViewElement) {
                                            is TimelineViewElement.Date -> {
                                                DateStickyHeader(
                                                    date = timelineViewElement.formattedDate,
                                                    focusable = true
                                                )
                                            }

                                            is TimelineViewElement.Element -> {
                                                val viewModel = timelineViewElement.viewModel
                                                // if an empty timeline-event is marked as the focusedItem we cannot tab into the
                                                // timeline due to it not being focusable so we initially skip all empties
                                                if (focusedItem.value == timelineViewElement.key && viewModel.element.value is TimelineElementViewModel.Empty) {
                                                    focusedItem.value = timelineViewElements.value
                                                        .getOrNull(index + 1)?.key
                                                }

                                                TimelineElementHolder(viewModel, index)
                                            }
                                        }
                                    }
                                }
                            }
                            ListDateHeader(
                                visibleItems,
                                timelineViewElements,
                                show = listState.canScrollForward,
                            )
                            ScrollToEndButton(timelineViewModel, canScrollToEnd)
                            draggedFile.value?.let { draggedFile ->
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.Circle,
                                                "",
                                                Modifier.size(100.dp),
                                                tint = Color.Gray,
                                            )
                                            Icon(
                                                MaterialTheme.messengerIcons.attachFile,
                                                i18n.timelineSendFile(),
                                                Modifier.size(60.dp),
                                            )
                                        }
                                        Text(
                                            draggedFile.toString(),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }

                            ReportMessageSwitch(timelineViewModel)
                        }

                        VerticalScrollbar(
                            Modifier.align(Alignment.CenterEnd),
                            listState,
                            reverseLayout = true,
                        )
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
                    launch {
                        waitForElementWithTimeout(timelineElementViewSelector, element)
                    }
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
            }.asReversed()
        }
    }
}

@Composable
fun ReportMessageSwitch(timelineViewModel: TimelineViewModel) {
    Children(
        stack = timelineViewModel.reportMessageStack,
        animation = stackAnimation(fade()),
    ) {
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
                visibleItems.firstOrNull {
                    val key = it.key as? String
                    if (key == null || key.startsWith("date-") || key.startsWith("typing"))
                        return@firstOrNull false
                    // we want the last element in the timeline only if it is completely visible (compose considers even
                    // 1 pixel of an element as "in view" which is not what we want)
                    it.index == lastVisibleIndexOffset && (it.offset == 0 || lastIsTyping) ||
                            it.index > lastVisibleIndexOffset
                }?.key as? String
            val firstVisible =
                visibleItems.lastOrNull {
                    val key = it.key as? String
                    if (key == null || key.startsWith("date-") || key.startsWith("typing"))
                        return@lastOrNull false
                    true
                }?.key as? String
            if (firstVisible != null && lastVisible != null) {
                firstVisible to lastVisible
            } else null
        }
    }
}

@Composable
fun getInitialFirstVisibleItemIndex(
    timelineViewModel: TimelineViewModel,
    timelineViewElements: List<TimelineViewElement>,
    showTypingIndicator: Boolean
): Int? {
    var initialFirstVisibleItemIndex by remember { mutableStateOf<Int?>(null) }
    var initialScrollTo by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        initialScrollTo = timelineViewModel.scrollTo.first()
    }
    LaunchedEffect(initialScrollTo, timelineViewElements, showTypingIndicator) {
        if (initialFirstVisibleItemIndex != null || initialScrollTo == null) return@LaunchedEffect
        initialFirstVisibleItemIndex = timelineViewElements.indexOfLast { element ->
            element is TimelineViewElement.Element && element.viewModel.key == initialScrollTo
        }.takeIf { it >= 0 }
            ?.let { index ->
                when {
                    index == 0 && showTypingIndicator -> 0
                    showTypingIndicator -> index + 1
                    else -> index
                }
            }
    }
    return initialFirstVisibleItemIndex
}

@Composable
fun updateVisibleItems(
    timelineViewModel: TimelineViewModel,
    visibleItems: State<Pair<String, String>?>,
    timelineViewElements: State<List<TimelineViewElement>>,
) {
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val viewState = remember {
        derivedStateOf {
            val timelineViewElementsWithoutDate =
                timelineViewElements.value.filterIsInstance<TimelineViewElement.Element>()
            visibleItems.value?.let {
                TimelineViewModel.ViewState(
                    firstVisibleElement = it.first,
                    lastVisibleElement = it.second,
                    firstLoadedElement = timelineViewElementsWithoutDate.last().viewModel.key,
                    lastLoadedElement = timelineViewElementsWithoutDate.first().viewModel.key,
                    timelineIsFocused = lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED),
                )
            }
        }
    }
    LaunchedEffect(viewState.value) {
        log.trace { "viewState: ${viewState.value}" }
        timelineViewModel.viewState.value = viewState.value
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
                        ?.viewModel?.formattedDate
                }
            }
        }
        Box(Modifier.padding(end = additionalEndPadding.dp)) {
            timestamp.value?.let { DateStickyHeader(it, focusable = false) }
        }
    }
}
