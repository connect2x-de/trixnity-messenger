package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.files.SaveFileDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun FileBasedDetailsHeader(
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onClose: () -> Unit,
    additionalButtons: @Composable RowScope.() -> Unit = {},
) {
    val i18n = DI.get<I18nView>()
    val downloadProgress = element.downloadMediaProgress.collectAsState().value
    val error = element.downloadMediaError.collectAsState().value

    var saveDialogOpen by remember { mutableStateOf(false) }
    if (saveDialogOpen) SaveFileDialog(
        element.name,
        element.mimeType,
        error,
        element::downloadMedia,
    ) { saveDialogOpen = false }

    Row(
        Modifier
            .fillMaxWidth()
            .background(color = Color.Black)
            .padding(MaterialTheme.messengerDpConstants.small)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                    onClose()
                    true
                } else {
                    false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.messengerDpConstants.small)
    ) {
        FileBasedDetailsHeaderButton(Icons.Outlined.Close, i18n.commonClose(), onClose)

        Box(
            Modifier.weight(1f, fill = true).padding(vertical = MaterialTheme.messengerDpConstants.small),
            contentAlignment = Alignment.Center
        ) {
            Tooltip(tooltip = {
                TooltipText(element.name)
            }) {
                Text(
                    text = element.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        additionalButtons(this)

        if (downloadProgress == null) {
            FileBasedDetailsHeaderButton(Icons.Outlined.Download, i18n.downloadMessage()) { saveDialogOpen = true }
        } else {
            CircularProgressIndicator(
                progress = {
                    downloadProgress.percent
                },
                modifier = Modifier.size(40.dp),
                color = Color.LightGray,
            )
        }
    }
}

@Composable
fun FileBasedDetailsHeaderButton(
    icon: ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
) {
    Tooltip(tooltip = { TooltipText(actionDescription) }) {
        IconButton(
            onClick = onAction,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.DarkGray),
            modifier = Modifier.buttonPointerModifier(),
        ) {
            Icon(icon, contentDescription = actionDescription, tint = Color.LightGray)
        }
    }
}
