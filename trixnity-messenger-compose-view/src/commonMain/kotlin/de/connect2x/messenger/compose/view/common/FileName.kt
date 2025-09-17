package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.util.ifNotNull
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.formatDuration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FileName(fileName: String) {
    Text(
        fileName,
        style = MaterialTheme.typography.bodySmall,
        overflow = TextOverflow.Ellipsis,
        maxLines = 3,
        modifier = Modifier.sizeIn(maxWidth = 200.dp)
    )
}

@Composable
fun FileInfo(element: RoomMessageTimelineElementViewModel.FileBased<*>, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append(element.name)
            pushStyle(SpanStyle(fontWeight = FontWeight.Light))
            when (element) {
                is RoomMessageTimelineElementViewModel.FileBased.File -> {
                    append(element.size)
                }

                is RoomMessageTimelineElementViewModel.FileBased.Audio -> {
                    append(element.duration.ifNotNull { formatDuration(it.milliseconds) })
                    append(element.size)
                }
            }
        },
        style = MaterialTheme.typography.bodySmall,
        overflow = TextOverflow.Ellipsis,
        maxLines = 3,
        modifier = modifier.sizeIn(maxWidth = 200.dp)
    )
}
