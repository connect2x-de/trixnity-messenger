package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.theme.dp

@Composable
fun TooltipText(text: String) = Text(text)

@Composable
fun TooltipText(text: suspend () -> String) {
    val cached = produceState("", Unit) {
        value = text()
    }
    if (cached.value.isBlank()) {
        CircularProgressIndicator(
            Modifier.size(MaterialTheme.typography.bodySmall.dp).padding(MaterialTheme.typography.bodySmall.dp / 2)
        )
    } else {
        Text(cached.value)
    }
}

@Composable
fun TooltipIconButton(
    text: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Tooltip(tooltip = { TooltipText(text) }) {
        IconButton(onClick) {
            content()
        }
    }
}
