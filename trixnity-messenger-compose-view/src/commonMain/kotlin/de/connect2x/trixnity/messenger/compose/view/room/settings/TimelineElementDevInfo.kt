package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.settings.DevInfoCard
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementDevInfoViewModel

interface TimelineElementDevInfoView {
    @Composable
    fun create(timelineElementDevInfoViewModel: TimelineElementDevInfoViewModel)
}

@Composable
fun TimelineElementDevInfo(timelineElementDevInfoViewModel: TimelineElementDevInfoViewModel) {
    DI.get<TimelineElementDevInfoView>().create(timelineElementDevInfoViewModel)
}

class TimelineElementDevInfoViewImpl : TimelineElementDevInfoView {
    @Composable
    override fun create(timelineElementDevInfoViewModel: TimelineElementDevInfoViewModel) {
        val i18n = DI.get<I18nView>()

        val body = timelineElementDevInfoViewModel.body.collectAsState().value
        val formatedBody = timelineElementDevInfoViewModel.formatedBody.collectAsState().value

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Column{
                    Header(timelineElementDevInfoViewModel::back, i18n.devInfo())
                    SmallSpacer()
                    Column(Modifier.padding(start = 8.dp, end = 8.dp)){
                        body?.let { content ->
                            DevInfoCard(i18n.timelineElementMetadataBody(), Icons.Default.Code){
                                ThemedSelectableText(
                                    content,
                                    MaterialTheme.components.selectionOnSurface
                                )
                            }
                            SmallSpacer()
                        }
                        formatedBody?.let { content ->
                            DevInfoCard(i18n.timelineElementMetadataFormattedBody(), Icons.Default.Code){
                                ThemedSelectableText(
                                    content,
                                    MaterialTheme.components.selectionOnSurface
                                )
                            }
                            SmallSpacer()
                        }
                        timelineElementDevInfoViewModel.eventId.let { content ->
                            DevInfoCard(i18n.timelineElementMetadataEventId(), Icons.Default.Numbers){
                                CopyableUserId(UserId(content.full), MaterialTheme.typography.bodyLarge)
                            }
                            SmallSpacer()
                        }
                    }
                }
            }
        }
    }
}
