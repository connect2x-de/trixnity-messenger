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
import de.connect2x.messenger.compose.view.theme.messengerIcons
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReportMessageRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map


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
        val isFocused = IsFocused.current
        // because layout is reversed
        val timelineElementHolderViewModels =
            timelineViewModel.timelineElementHolderViewModels.map { it.reversed() }
                .collectAsState(listOf()).value

        val error = timelineViewModel.error.collectAsState().value
        val draggedFile = timelineViewModel.draggedFile.collectAsState().value

        val focusManager = LocalFocusManager.current

        Surface(modifier = Modifier.weight(1.0f, fill = true)) {
            if (timelineElementHolderViewModels.isEmpty()) {
                Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            } else {
                val unreadMarkerOnFirstLoad = remember {
                    timelineElementHolderViewModels.indexOfLast {
                        it is TimelineElementHolderViewModel && it.shouldShowUnreadMarkerFlow.value
                    }
                }
                val listState =
                    rememberLazyListState(initialFirstVisibleItemIndex = if (unreadMarkerOnFirstLoad >= 0) unreadMarkerOnFirstLoad else 0)

                val (scrollTo, setScrollTo) = remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    timelineViewModel.scrollTo.collect {
                        setScrollTo(it)
                    }
                }

                LaunchedEffect(listState) {
                    snapshotFlow { // important performance consideration: use snapshotFlow to avoid recompositions!
                        listState.layoutInfo.visibleItemsInfo.firstOrNull {
                            // we want the last element in the timeline only if it is completely visible (compose considers even
                            // 1 pixel of an element as "in view" which is not what we want)
                            it.index == 0 && it.offset == 0 || it.index > 0
                        }?.let {
                            val key = it.key
                            if (key is String) key else null
                        }
                    }.collectLatest {
                        timelineViewModel.lastVisibleTimelineElement.value = it
                    }
                }
                LaunchedEffect(listState) {
                    snapshotFlow {
                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.let {
                            val key = it.key
                            if (key is String) key else null
                        }
                    }.collectLatest { firstVisible ->
                        if (firstVisible != null) {
                            timelineViewModel.firstVisibleTimelineElement.value = firstVisible
                        }
                    }
                }

                LaunchedEffect(scrollTo, timelineElementHolderViewModels) {
                    if (scrollTo != null) {
                        log.debug { "scrolling to $scrollTo (ids: ${timelineElementHolderViewModels.joinToString { it.key }})" }
                        val index = timelineElementHolderViewModels.indexOfFirst { it.key == scrollTo }
                        if (index >= 0) {
                            listState.animateScrollToItem(index)
                            setScrollTo(null)
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
                                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index != 0
                            }
                        }

                        timelineViewModel.windowIsFocused.value = isFocused

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
                                reverseLayout = true,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                log.trace { "rendering timeline elements" }
                                items(
                                    timelineElementHolderViewModels,
                                    key = { it.key }
                                ) { timelineElementHolderViewModel ->
                                    TimelineElement(
                                        timelineElementHolderViewModel,
                                        timelineViewModel,
                                    )
                                }
                            }
                            DateStickyHeader(timelineViewModel)
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
                            true,
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
