package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.util.ifNotNull
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration

@Composable
fun FileName(fileName: String) {
    Text(
        fileName,
        style = MaterialTheme.typography.bodySmall,
        overflow = TextOverflow.Ellipsis,
        maxLines = 3,
        modifier = Modifier.sizeIn(maxWidth = 200.dp),
    )
}

@Composable
fun FileInfo(element: RoomMessageTimelineElementViewModel.FileBased<*>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(element.name, style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis, maxLines = 1)
        val metadata =
            buildString {
                    if (element is RoomMessageTimelineElementViewModel.FileBased.Audio) {
                        append(element.duration.ifNotNull { formatDuration(it) })
                    }
                    append(element.size.orEmpty())
                }
                .trim()
        if (metadata.isNotEmpty()) {
            Text(
                metadata,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
