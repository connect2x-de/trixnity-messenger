package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.metadata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.HeaderBackButtonType.BACK
import de.connect2x.trixnity.messenger.compose.view.common.HeaderBackButtonType.CLOSE
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.settings.ExtrasPaneHeader
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.compose.view.util.DevInfoButton
import de.connect2x.trixnity.messenger.compose.view.util.waitForElementWithTimeout
import de.connect2x.trixnity.messenger.internal.sort.getSorted
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface TimelineElementMetadataView {
    @Composable fun create(viewModel: TimelineElementMetadataViewModel, isBottomOfStack: Boolean, isSinglePane: Boolean)
}

@Composable
fun TimelineElementMetadata(
    viewModel: TimelineElementMetadataViewModel,
    isBottomOfStack: Boolean,
    isSinglePane: Boolean,
) {
    DI.get<TimelineElementMetadataView>().create(viewModel, isBottomOfStack, isSinglePane)
}

class TimelineElementMetadataViewImpl : TimelineElementMetadataView {
    @Composable
    override fun create(viewModel: TimelineElementMetadataViewModel, isBottomOfStack: Boolean, isSinglePane: Boolean) {
        val i18n = DI.get<I18nView>()

        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        var elementHistory by remember { mutableStateOf(listOf<TimelineElementHolderViewModel>()) }
        val firstElement = elementHistory.firstOrNull()
        var lastElement by remember { mutableStateOf<TimelineElementHolderViewModel?>(null) }
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            launch {
                viewModel.elementHistory.filterNotNull().collect { history ->
                    withContext(Dispatchers.Default) {
                        history.forEach { element ->
                            launch { waitForElementWithTimeout(timelineElementViewSelector, element) }
                        }
                    }
                    elementHistory = history
                }
            }
            viewModel.element.filterNotNull().collect { newElement ->
                waitForElementWithTimeout(timelineElementViewSelector, newElement)
                lastElement = newElement
            }
        }

        ExtrasPaneHeader(
            title = i18n.timelineElementMetadataTitle(),
            error = null,
            onBack = { viewModel.back() },
            backButtonType = if (isSinglePane || isBottomOfStack.not()) BACK else CLOSE,
            { DevInfoButton(viewModel::openDevInfo) },
        ) {
            if (lastElement == null || elementHistory.isEmpty()) {
                LoadingSpinner(Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        verticalArrangement = Arrangement.Top,
                        modifier =
                            Modifier.padding(PaddingValues(vertical = 0.dp, horizontal = 20.dp))
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                    ) {
                        DI.current.getSorted<TimelineElementMetadataListItemView>().forEach {
                            with(it) {
                                create(
                                    viewModel,
                                    elementHistory,
                                    firstElement,
                                    lastElement,
                                    scrollState,
                                    isBottomOfStack,
                                    isSinglePane,
                                )
                            }
                        }
                    }
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
                }
            }
        }
    }
}

@Composable
fun ColumnScope.SubHeading(heading: String) { // TODO re-use in other components
    MiddleSpacer()
    Text(text = heading, style = MaterialTheme.typography.titleMedium)
    SmallSpacer()
}

@Composable
fun UserInfo(
    userInfo: UserInfoElement,
    modifier: Modifier = Modifier,
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
    Tooltip({ Text(tooltipText) }) {
        ThemedListItemButton(
            style = MaterialTheme.components.settingsItem,
            leadingContent = { ThemedUserAvatar(userInfo.initials, image) },
            headlineContent = {
                Text(
                    userInfo.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.paddingFromBaseline(0.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            supportingContent =
                if (!hasReactions) null
                else {
                    {
                        Text(
                            compiledReactionsList,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                            modifier = Modifier.paddingFromBaseline(0.dp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                },
            modifier = modifier,
            onClick = { onOpenUserProfile(userInfo.userId) },
        )
    }
}
