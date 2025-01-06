package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


//private val log = KotlinLogging.logger {}

@Composable
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel) {

    val edits = viewModel.edits.collectAsState().value
    val readers = viewModel.readers.collectAsState().value
    val reactions = viewModel.reactions.collectAsState().value

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

            Column {

                (reactions.keys.map { it.userId } + (readers ?: setOf()).map { it.userId })
                    .distinct()
                    .mapNotNull { userId ->
                        readers?.find { it.userId == userId } ?: reactions.keys.find { it.userId == userId }
                    }
                    .forEach { user ->
                        val avatarImage = user.image?.collectAsState(null)?.value
                        Box(Modifier.padding(16.dp)) {
                            Row {
                                Avatar(avatarImage, user.initials ?: "?")
                                Spacer(Modifier.size(8.dp))
                                if (readers?.contains(user) == true) Text("read ")
                                Text(user.name)
                                Row {
                                    val userReactions = reactions.keys
                                        .find { it.userId == user.userId }
                                        ?.let { key -> reactions[key] } ?: setOf()
                                    userReactions.forEach { reactionKey ->
                                        Spacer(Modifier.size(8.dp))
                                        Text(reactionKey)
                                    }
                                }
                            }
                        }
                    }
            }

        }
    }
}
