package de.connect2x.messenger.compose.view.room.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
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
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


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
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ColumnScope.create(timelineViewModel: TimelineViewModel) {
        val i18n = DI.get<I18nView>()
        val isFocused = IsFocused.current
        var scrollTo by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            timelineViewModel.scrollTo.collect { scrollTo = it }
        }

        var timelineElementHolderViewModels by remember {
            mutableStateOf<List<BaseTimelineElementHolderViewModel>>(listOf())
        }
        val timelineElementViewModelGrouped by derivedStateOf {
            timelineElementHolderViewModels.groupBy { it.formattedDate }
        }

        LaunchedEffect(Unit) {
            timelineViewModel.elements.collect { elements ->
                log.trace { "wait for elements to be ready" }
                coroutineScope {
                    elements.forEach { element ->
                        launch { element.element.filterNotNull().first() }
                        launch { element.isFirstInUserSequence.filterNotNull().first() }
                        launch { element.showSender.filterNotNull().first() }
                        launch { element.showBigGapBefore.filterNotNull().first() }
                        when (element) {
                            is TimelineElementHolderViewModel -> {
                                launch { element.hasUnreadMarker.filterNotNull().first() }
                                launch { element.hasLoadingIndicatorBefore.filterNotNull().first() }
                                launch { element.hasLoadingIndicatorAfter.filterNotNull().first() }
                                launch { element.isRead.filterNotNull().first() }
                                launch { element.reactions.filterNotNull().first() }
                                launch { element.isReplaced.filterNotNull().first() }
                            }

                            is OutboxElementHolderViewModel -> {}
                        }
                    }
                }
                log.trace { "finished wait for elements to be ready" }
                timelineElementHolderViewModels = elements
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
                    timelineElementHolderViewModels.indexOfLast {
                        it is TimelineElementHolderViewModel && it.hasUnreadMarker.value
                    }
                }
                val listState =
                    rememberLazyListState(initialFirstVisibleItemIndex = if (unreadMarkerOnFirstLoad >= 0) unreadMarkerOnFirstLoad else 0)

                val uiState by remember {
                    derivedStateOf {
                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                        val firstVisible =
                            visibleItems.firstOrNull {
                                // we want the last element in the timeline only if it is completely visible (compose considers even
                                // 1 pixel of an element as "in view" which is not what we want)
                                (it.key as? String)?.startsWith('!') == true &&
                                        it.index == 0 && it.offset == 0 || it.index > 0
                            }?.let {
                                val key = it.key
                                key as? String
                            }
                        val lastVisible = visibleItems.lastOrNull { (it.key as? String)?.startsWith('!') == true }
                            ?.let {
                                val key = it.key
                                key as? String
                            }
                        if (firstVisible != null && lastVisible != null)
                            TimelineViewModel.ViewState(firstVisible, lastVisible, isFocused)
                        else null
                    }
                }
                LaunchedEffect(uiState) {
                    timelineViewModel.viewState.value = uiState
                }

                LaunchedEffect(scrollTo) {
                    if (scrollTo != null) {
                        val index = timelineElementHolderViewModels.indexOfFirst { it.key == scrollTo }
                        if (index >= 0) {
                            log.debug { "scrolling to $scrollTo (index=$index)" }
                            scrollTo = null
                            listState.animateScrollToItem(index)
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
                                val totalItemsCount = listState.layoutInfo.totalItemsCount
                                val index = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                index != null && index != (totalItemsCount - 1)
                            }
                        }

                        Box {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = 10.dp,
                                    bottom = 10.dp,
                                    start = if (this@BoxWithConstraints.maxWidth.value > 1000) 80.dp else 10.dp,
                                    end = if (this@BoxWithConstraints.maxWidth.value > 1000) 80.dp else 18.dp, // 10 + 8, since we cannot add a padding or Spacer at the end
                                ),
                                state = listState,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                log.trace { "rendering timeline elements" }
                                timelineElementViewModelGrouped.forEach { (date, viewModels) ->
                                    stickyHeader(date) {
                                        DateStickyHeader(date)
                                    }
                                    items(
                                        viewModels,
                                        key = { it.key }
                                    ) { viewModel ->
                                        TimelineElementHolder(
                                            viewModel,
                                        )
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
                            false,
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
