package de.connect2x.messenger.compose.view.room.timeline

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
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.LoadingSpinner
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementHolder
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.messenger.compose.view.util.RovingFocusContainer
import de.connect2x.messenger.compose.view.util.RovingFocusItem
import de.connect2x.messenger.compose.view.util.scrollIntoView
import de.connect2x.messenger.compose.view.util.verticalRovingFocus
import de.connect2x.messenger.compose.view.util.waitForElementWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

interface TimelineView {
    @Composable
    fun ColumnScope.create(timelineViewModel: TimelineViewModel)
}

@Composable
fun ColumnScope.Timeline(timelineViewModel: TimelineViewModel) {
    with(DI.get<TimelineView>()) { create(timelineViewModel) }
}

private const val additionalEndPadding = 8

class TimelineViewImpl : TimelineView {
    @Composable
    override fun ColumnScope.create(timelineViewModel: TimelineViewModel) {
        val i18n = DI.get<I18nView>()
        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        var scrollTo by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            timelineViewModel.scrollTo.collect { scrollTo = it }
        }

        val timelineElementHolderViewModels = remember {
            mutableStateOf<List<BaseTimelineElementHolderViewModel>>(listOf())
        }
        val isTimelineLoading = remember {
            derivedStateOf {
                timelineElementHolderViewModels.value.isEmpty()
            }
        }
        val timelineElementViewModelGrouped = remember {
            derivedStateOf {
                val vms = timelineElementHolderViewModels.value
                buildList(vms.size) {
                    var lastDate: String? = null
                    for (index in vms.indices.reversed()) {
                        val vm = vms[index]
                        when {
                            lastDate == vm.formattedDate -> add(null to vm)
                            vm.element.value is TimelineElementViewModel.Empty -> add(null to vm)
                            else -> {
                                add(vm.formattedDate to vm)
                                lastDate = vm.formattedDate
                            }
                        }
                    }
                }.asReversed()
            }
        }
        val navigatableTimelineElements = remember {
            derivedStateOf {
                timelineElementHolderViewModels.value.filter {
                    val element = it.element.value
                    element != null && timelineElementViewSelector.isFocusable(element)
                }.map { it.key }
            }
        }
        val uiTimelineElements = remember {
            derivedStateOf {
                buildList {
                    for ((date, viewModel) in timelineElementViewModelGrouped.value) {
                        add(viewModel.key)
                        if (date != null) {
                            add("date-$date-${viewModel.key}")
                        }
                    }
                }
            }
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
                timelineElementHolderViewModels.value = elements.asReversed()
            }
        }

        val error = timelineViewModel.error.collectAsState()
        val draggedFile = timelineViewModel.draggedFile.collectAsState()

        val focusManager = LocalFocusManager.current

        Box(modifier = Modifier.weight(1.0f, fill = true)) {
            if (isTimelineLoading.value) {
                Box(Modifier.fillMaxSize()) {
                    LoadingSpinner(Modifier.align(Alignment.Center))
                }
            } else {
                val unreadMarkerOnFirstLoad = remember {
                    (timelineElementHolderViewModels.value.indexOfLast {
                        it is TimelineElementHolderViewModel && it.showUnreadMarker.value
                    })
                }

                val initialIndex = remember {
                    timelineViewModel.viewState.value?.lastVisibleElement
                        ?.let { key ->
                            var dateCount = 0
                            timelineElementViewModelGrouped.value.mapIndexedNotNull { index, elementPair ->
                                if (elementPair.second.key == key)
                                    return@mapIndexedNotNull index + dateCount
                                if (elementPair.first != null)
                                    dateCount++
                                return@mapIndexedNotNull null
                            }.firstOrNull()
                        } ?: 0
                }

                val initialFirstVisibleItemIndex =
                    if (unreadMarkerOnFirstLoad >= 0) unreadMarkerOnFirstLoad else initialIndex
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex)

                val lastItem = remember {
                    derivedStateOf {
                        navigatableTimelineElements.value.firstOrNull()
                    }
                }

                LaunchedEffect(scrollTo, timelineElementHolderViewModels.value) {
                    if (scrollTo != null) {
                        val index = withTimeoutOrNull(5.seconds) {
                            timelineElementHolderViewModels.value.indexOfFirst { it.key == scrollTo }
                        } ?: -1
                        if (index >= 0) {
                            log.debug { "scrolling to $scrollTo (index=$index)" }
                            listState.animateScrollToItem(index)
                            scrollTo = null
                        }
                    }
                }

                val visibleItems = rememberVisibleItems(listState)
                updateVisibleItems(timelineViewModel, visibleItems, timelineElementHolderViewModels)

                BoxWithConstraints(
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
                    Box(
                        Modifier
                            .padding(vertical = 2.dp)
                    ) {
                        val canScrollToEnd = remember {
                            derivedStateOf {
                                val index = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                                index != null && index != 0
                            }
                        }
                        Box {
                            RovingFocusContainer {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                        .verticalRovingFocus(
                                            default = lastItem.value,
                                            scroll = { item ->
                                                val index = uiTimelineElements.value.indexOf(item)
                                                if (index != -1) {
                                                    listState.scrollIntoView(index)
                                                }
                                            },
                                            up = {
                                                val currentItem = activeRef.value ?: lastItem.value
                                                val currentIndex =
                                                    navigatableTimelineElements.value.indexOf(currentItem)
                                                val nextIndex = currentIndex.plus(1)
                                                    .coerceIn(navigatableTimelineElements.value.indices)
                                                navigatableTimelineElements.value[nextIndex]
                                            },
                                            down = {
                                                val currentItem = activeRef.value ?: lastItem.value
                                                val currentIndex =
                                                    navigatableTimelineElements.value.indexOf(currentItem)
                                                val nextIndex = currentIndex.minus(1)
                                                    .coerceIn(navigatableTimelineElements.value.indices)
                                                navigatableTimelineElements.value[nextIndex]
                                            },
                                        ),
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
                                    timelineElementViewModelGrouped.value.forEach { (date, viewModel) ->
                                        item(viewModel.key) {
                                            RovingFocusItem(viewModel.key) {
                                                TimelineElementHolder(viewModel)
                                            }
                                        }
                                        if (date != null)
                                            item("date-$date-${viewModel.key}") {
                                                DateStickyHeader(date)
                                            }
                                    }
                                }
                            }
                            ListDateHeader(
                                visibleItems,
                                timelineElementHolderViewModels,
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
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val lastVisible =
                visibleItems.firstOrNull {
                    // we want the last element in the timeline only if it is completely visible (compose considers even
                    // 1 pixel of an element as "in view" which is not what we want)
                    (it.key as? String)?.startsWith('!') == true &&
                            it.index == 0 && it.offset == 0 || it.index > 0
                }?.let {
                    val key = it.key
                    key as? String
                }
            val firstVisible = visibleItems.lastOrNull { (it.key as? String)?.startsWith('!') == true }
                ?.let {
                    val key = it.key
                    key as? String
                }
            if (firstVisible != null && lastVisible != null) {
                firstVisible to lastVisible
            } else null
        }
    }
}

@Composable
fun updateVisibleItems(
    timelineViewModel: TimelineViewModel,
    visible: State<Pair<String, String>?>,
    timelineElementHolderViewModels: State<List<BaseTimelineElementHolderViewModel>>,
) {
    val isFocused = IsFocused.current
    val viewState = remember {
        derivedStateOf {
            visible.value?.let {
                TimelineViewModel.ViewState(
                    firstVisibleElement = it.first,
                    lastVisibleElement = it.second,
                    firstLoadedElement = timelineElementHolderViewModels.value.last().key,
                    lastLoadedElement = timelineElementHolderViewModels.value.first().key,
                    windowIsFocused = isFocused,
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
    timelineElementHolderViewModels: State<List<BaseTimelineElementHolderViewModel>>,
    show: Boolean,
) {
    if (show) {
        val timestamp = remember {
            derivedStateOf {
                visible.value?.first?.let { lastEventId ->
                    timelineElementHolderViewModels.value
                        .find { it.key == lastEventId }
                        ?.formattedDate
                }
            }
        }
        Box(Modifier.padding(end = additionalEndPadding.dp)) {
            timestamp.value?.let { DateStickyHeader(it) }
        }
    }
}
