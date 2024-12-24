package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


private val log = KotlinLogging.logger {}

@Composable
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel) {

    val edits = viewModel.edits.collectAsState().value

    Box(Modifier.fillMaxSize()) {
        Column {
            Header(viewModel::back, "we need to go back")

            Text("metadata of ${viewModel.eventId}")

            Column {
                edits.sortedBy { it.originTimestamp }.reversed().forEach { item ->
                    val content = item.content?.fold({ it }, { null })
                    if (content is RoomMessageEventContent) {
                        Box(Modifier.padding(16.dp)) {
                            Text("${item.originTimestamp}: ${content.formattedBody}", Modifier.background(Color.Gray))
                        }
                    }
                }
            }

        }
    }
}
