package de.connect2x.messenger.compose.view.room.timeline.element.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.zIndex
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun FileBasedDetailsHeader(
    element: RoomMessageTimelineElementViewModel.FileBased<*>,
    onSave: () -> Unit,
    onClose: () -> Unit,
    additionalButtons: @Composable RowScope.() -> Unit = {},
) {
    val i18n = DI.get<I18nView>()
    val configuration = DI.current.get<MatrixMessengerConfiguration>()
    val downloadProgress = element.downloadMediaProgress.collectAsState().value

    Row(
        Modifier
            .zIndex(99.0f)
            .fillMaxWidth()
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
        FileBasedDetailsHeaderButton(Icons.Outlined.Close, i18n.commonClose(), onAction = onClose)

        Box(
            Modifier.weight(1f, fill = true).padding(vertical = MaterialTheme.messengerDpConstants.small),
            contentAlignment = Alignment.Center
        ) {
            Tooltip(tooltip = {
                Text(element.name)
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
            FileBasedDetailsHeaderButton(
                Icons.Outlined.Download,
                i18n.downloadMessage(),
                !configuration.forbidDownloads,
                onSave
            )
        } else {
            ThemedProgressIndicator(
                progress = {
                    downloadProgress.percent
                },
                style = MaterialTheme.components.circularProgressIndicator
            )
        }
    }
}

@Composable
fun FileBasedDetailsHeaderButton(
    icon: ImageVector,
    actionDescription: String,
    isEnabled: Boolean = true,
    onAction: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    Tooltip(
        tooltip = {
            if (isEnabled)
                Text(actionDescription)
            else
                Text(i18n.commonButtonDisabled())
        },
    ) {
        ThemedIconButton(
            enabled = isEnabled,
            style = MaterialTheme.components.fileViewerIconButton,
            onClick = onAction,
        ) {
            Icon(icon, contentDescription = actionDescription)
        }
    }
}
