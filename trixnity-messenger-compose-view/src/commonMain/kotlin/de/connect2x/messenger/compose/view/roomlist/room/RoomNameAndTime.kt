package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.PlaceholderHighlight
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.common.fade
import de.connect2x.messenger.compose.view.common.icons.UnencryptedIcon
import de.connect2x.messenger.compose.view.common.placeholder
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun RoomName(roomName: String?, modifier: Modifier = Modifier) {
    Tooltip(
        { TooltipText(roomName ?: " ") },
        modifier,
        delayMillis = 1_000,
    ) {
        Text(
            text = roomName ?: " ",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RowScope.RoomTime(roomListElementViewModel: RoomListElementViewModel, modifier: Modifier = Modifier) {
    val time = roomListElementViewModel.time.collectAsState().value
    val isEncrypted = roomListElementViewModel.isEncrypted.collectAsState().value
    if (isEncrypted != null && isEncrypted.not()) {
        Box(Modifier.padding(end = 5.dp), contentAlignment = Alignment.Center) {
            UnencryptedIcon()
        }
    }
    Text(
        time ?: " ",
        modifier.then(Modifier.alignByBaseline()),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
    )
}

@Composable
fun RoomNameAndTime(roomListElementViewModel: RoomListElementViewModel) {
    val roomName = roomListElementViewModel.roomName.collectAsState().value
    Row(
        modifier = Modifier.placeholder(
            visible = roomName == null,
            color = Color.LightGray,
            shape = RoundedCornerShape(8.dp),
            highlight = PlaceholderHighlight.fade(highlightColor = Color(0xFFDDDDDD))
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.fillMaxWidth().weight(1.0f, false).alignByBaseline()
        ) {
            RoomName(roomName = roomName)
        }
        Spacer(Modifier.size(10.dp))
        RoomTime(roomListElementViewModel)
    }
}
