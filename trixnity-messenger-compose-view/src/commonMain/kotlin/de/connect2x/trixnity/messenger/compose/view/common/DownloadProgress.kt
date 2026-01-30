package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement

@Composable
fun BoxScope.DownloadProgress(
    progressElement: FileTransferProgressElement,
    cancel: (() -> Unit)? = null,
    color: Color = Color.LightGray
) {
    val i18n = DI.get<I18nView>()
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = backgroundColor(color),
            modifier = Modifier.padding(horizontal = MaterialTheme.messengerDpConstants.small)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                progressElement.percent?.let {
                    ThemedProgressIndicator(
                        progress = { it },
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.components.linearProgressIndicator
                    )
                } ?: ThemedProgressIndicator(
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.components.linearProgressIndicator
                )

                if (cancel != null)
                    Tooltip({ Text(i18n.commonCancel()) }) {
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            onClick = cancel
                        ) {
                            Icon(Icons.Default.Cancel, i18n.commonCancel())
                        }
                    }
            }
        }
        Spacer(Modifier.size(10.dp))
        Surface(
            color = backgroundColor(color),
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Text(progressElement.formattedProgress, color = color, modifier = Modifier.padding(8.dp))
        }
    }
}

private fun backgroundColor(color: Color) =
    if (color == Color.LightGray) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)

