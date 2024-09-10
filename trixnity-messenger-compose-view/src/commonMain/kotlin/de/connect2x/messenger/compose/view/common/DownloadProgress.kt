package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.util.FileTransferProgressElement

@Composable
fun BoxScope.DownloadProgress(
    progressElement: FileTransferProgressElement,
    cancel: (() -> Unit)? = null,
    color: Color = Color.LightGray
) {
    val i18n = DI.current.get<I18nView>()
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(color = backgroundColor(color), modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progressElement.percent },
                    modifier = Modifier.padding(start = 10.dp),
                )
                if (cancel != null)
                    IconButton(cancel, Modifier.buttonPointerModifier()) {
                        Icon(Icons.Default.Cancel, i18n.commonCancel().capitalize(Locale.current), tint = Color.Gray)
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

