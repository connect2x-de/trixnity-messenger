package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
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
import de.connect2x.messenger.compose.view.common.ErrorDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementHolder
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.messenger.compose.view.util.waitForElementWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
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

class TimelineViewImpl : TimelineView {
    @Composable
    override fun ColumnScope.create(timelineViewModel: TimelineViewModel) {
        val i18n = DI.get<I18nView>()
        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        val isFocused = IsFocused.current
        var scrollTo by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            timelineViewModel.scrollTo.collect { scrollTo = it }
        }

        var timelineElementHolderViewModels by remember {
            mutableStateOf<List<BaseTimelineElementHolderViewModel>>(listOf())
        }
        val timelineElementViewModelGrouped by derivedStateOf {
            val vms = timelineElementHolderViewModels
            buildList(vms.size) {
                var lastDate: String? = null
                for (index in vms.indices.reversed()) {
                    val vm = vms[index]
                    if (lastDate != vm.formattedDate) add(vm.formattedDate to vm)
                    else add(null to vm)
                    lastDate = vm.formattedDate
                }
            }.asReversed()
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
                timelineElementHolderViewModels = elements.asReversed()
            }
        }

        val error = timelineViewModel.error.collectAsState().value
        val draggedFile = timelineViewModel.draggedFile.collectAsState().value

        val focusManager = LocalFocusManager.current

        Surface(modifier = Modifier.weight(1.0f, fill = true)) {
            if (timelineElementHolderViewModels.isEmpty()) {
                Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            } else {
                val unreadMarkerOnFirstLoad = remember {
                    (timelineElementHolderViewModels.indexOfLast {
                        it is TimelineElementHolderViewModel && it.showUnreadMarker.value
                    })
                }

                val initialIndex = remember {
                    timelineViewModel.viewState.value?.lastVisibleElement
                        ?.let { key ->
                            var dateCount = 0
                            timelineElementViewModelGrouped.mapIndexedNotNull { index, elementPair ->
                                if (elementPair.second.key == key)
                                    return@mapIndexedNotNull index + dateCount
                                if (elementPair.first != null)
                                    dateCount++
                                return@mapIndexedNotNull null
                            }.firstOrNull()
                        }?: 0
                }

                val listState =
                    rememberLazyListState(initialFirstVisibleItemIndex =
                        if (unreadMarkerOnFirstLoad >= 0) unreadMarkerOnFirstLoad else initialIndex)


                val visible by remember {
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
                LaunchedEffect(visible, timelineElementHolderViewModels, isFocused) {
                    visible?.let {
                        timelineViewModel.viewState.value = TimelineViewModel.ViewState(
                            firstVisibleElement = it.first,
                            lastVisibleElement = it.second,
                            firstLoadedElement = timelineElementHolderViewModels.last().key,
                            lastLoadedElement = timelineElementHolderViewModels.first().key,
                            windowIsFocused = isFocused,
                        ).also {
                            log.debug { "viewState: $it" }
                        }
                    }
                }

                LaunchedEffect(scrollTo, timelineElementHolderViewModels) {
                    if (scrollTo != null) {
                        val index = withTimeoutOrNull(5.seconds) {
                            timelineElementHolderViewModels.indexOfFirst { it.key == scrollTo }
                        } ?: -1
                        if (index >= 0) {
                            log.debug { "scrolling to $scrollTo (index=$index)" }
                            listState.animateScrollToItem(index)
                            scrollTo = null
                        }
                    }
                }

                BoxWithConstraints(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus(true)
                            })
                        }
                ) {
                    if (error != null) {
                        ErrorDialog(
                            error,
                            { timelineViewModel.errorDismiss() },
                        )
                    }
                    Box(
                        Modifier
                            .padding(vertical = 2.dp)
                    ) {
                        val canScrollToEnd by remember {
                            derivedStateOf {
                                val index = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                                index != null && index != 0
                            }
                        }
                        val additionalEndPadding = 8
                        Box {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
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
                                timelineElementViewModelGrouped.forEach { (date, viewModel) ->
                                    item(viewModel.key) {
                                        TimelineElementHolder(viewModel)
                                    }
                                    if (date != null)
                                        item("date-$date-${viewModel.key}") {
                                            DateStickyHeader(date)
                                        }
                                }
                            }
                            Box(Modifier.padding(end = if (this@BoxWithConstraints.maxWidth.value > 1000) 0.dp else additionalEndPadding.dp)) {
                                listState.layoutInfo.visibleItemsInfo.lastOrNull { (it.key as? String)?.startsWith('!') == true }
                                    ?.let { layoutInfo ->
                                        timelineElementHolderViewModels.find { it.key == layoutInfo.key }?.let {
                                            DateStickyHeader(it.formattedDate)
                                        }
                                    }
                            }
                            ScrollToEndButton(timelineViewModel, canScrollToEnd)
                            if (draggedFile != null) {
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
