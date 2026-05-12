package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.theme.dp

@Composable
fun NotificationAndUnreadMarker(
    count: String?,
    isUnread: Boolean? = null,
    size: Dp = MaterialTheme.typography.labelSmall.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(size)) {
        when {
            count != null -> {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier
                        .defaultMinSize(minWidth = size)
                        .height(size),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = count,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                    )
                }
            }

            isUnread == true -> {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(size / 4)
                        .size(size / 2),
                    color = MaterialTheme.colorScheme.primary,
                ) {}
            }
        }
    }
}
