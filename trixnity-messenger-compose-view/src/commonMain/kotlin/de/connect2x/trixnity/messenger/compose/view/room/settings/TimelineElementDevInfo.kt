package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.settings.DevInfoCard
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.messenger.compose.view.util.waitForElementWithTimeout
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.coroutines.flow.filterNotNull
import net.folivo.trixnity.core.model.UserId

interface TimelineElementDevInfoView {
    @Composable
    fun create(timelineElementMetadataViewModel: TimelineElementMetadataViewModel)
}

@Composable
fun TimelineElementDevInfo(timelineElementMetadataViewModel: TimelineElementMetadataViewModel) {
    DI.get<TimelineElementDevInfoView>().create(timelineElementMetadataViewModel)
}

class TimelineElementDevInfoViewImpl : TimelineElementDevInfoView {
    @Composable
    override fun create(timelineElementMetadataViewModel: TimelineElementMetadataViewModel) {
        val i18n = DI.get<I18nView>()
        val timelineElementViewSelector = DI.get<TimelineElementViewSelector>()
        var lastElement by remember { mutableStateOf<TimelineElementHolderViewModel?>(null) }
        val messageElement =
            lastElement?.element?.collectAsState()?.value as? RoomMessageTimelineElementViewModel.TextBased<*>

        LaunchedEffect(Unit) {
            timelineElementMetadataViewModel.element.filterNotNull().collect { newElement ->
                waitForElementWithTimeout(timelineElementViewSelector, newElement)
                lastElement = newElement
            }
        }

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column{
                    Header(timelineElementMetadataViewModel::back, i18n.devInfo())
                    SmallSpacer()
                    Column(Modifier.padding(start = 8.dp, end = 8.dp)){
                        messageElement?.body?.let { content ->
                            DevInfoCard(i18n.timelineElementMetadataBody(), Icons.Default.Code){
                                ThemedSelectableText(
                                    content,
                                    MaterialTheme.components.selectionOnSurface
                                )
                            }
                            SmallSpacer()
                        }
                        messageElement?.formattedBody?.let { content ->
                            DevInfoCard(i18n.timelineElementMetadataFormattedBody(), Icons.Default.Code){
                                ThemedSelectableText(
                                    content,
                                    MaterialTheme.components.selectionOnSurface
                                )
                            }
                            SmallSpacer()
                        }
                        timelineElementMetadataViewModel.element.value?.eventId?.full?.let { content ->
                            DevInfoCard(i18n.timelineElementMetadataEventId(), Icons.Default.Numbers){
                                CopyableUserId(UserId(content), MaterialTheme.typography.bodyLarge)
                            }
                            SmallSpacer()
                        }
                    }
                }
            }
        }
    }
}
