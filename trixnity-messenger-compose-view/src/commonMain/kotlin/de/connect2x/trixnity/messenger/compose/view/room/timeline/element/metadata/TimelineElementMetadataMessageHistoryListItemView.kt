package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.metadata

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.timeline.DateStickyHeader
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel

interface TimelineElementMetadataMessageHistoryListItemView : TimelineElementMetadataListItemView

class TimelineElementMetadataMessageHistoryListItemViewImpl : TimelineElementMetadataMessageHistoryListItemView {
    @Composable
    override fun ColumnScope.create(
        viewModel: TimelineElementMetadataViewModel,
        elementHistory: List<TimelineElementHolderViewModel>,
        firstElement: TimelineElementHolderViewModel?,
        lastElement: TimelineElementHolderViewModel?,
        scrollState: ScrollState,
        BottomOfStack: Boolean,
        isSinglePane: Boolean,
    ) {
        val i18n = DI.get<I18nView>()

        if (lastElement == null || elementHistory.isEmpty()) {
            LoadingSpinner()
        } else {
            SubHeading(i18n.timelineElementMetadataMessage())
            MessageContentHistorySwitch(lastElement, elementHistory)
            SmallSpacer()
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

        // The max height is required here due to this component being a nested scroll container; no max height results
        // in a crash
        Box(Modifier.heightIn(max = 400.dp)) {
            LazyColumn(Modifier.fillMaxWidth().padding(end = 10.dp), state = scrollState) {
                elementHistoryGrouped.forEach { (date, viewModel) ->
                    if (date != null) {
                        item("date-$date-${viewModel.key}") {
                            DateStickyHeader(date, focusable = true)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item(viewModel.key) { MessageContent(viewModel) }
                }
            }

            // If the scroll bar were always shown it would force the box to its maximum height, creating a lot of empty
            // space.
            if (canScroll) VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState, false)
        }
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
        element?.let { element -> timelineElementViewSelector.createAsPreview(messageHolder, element, index = 0) }
    }
}
