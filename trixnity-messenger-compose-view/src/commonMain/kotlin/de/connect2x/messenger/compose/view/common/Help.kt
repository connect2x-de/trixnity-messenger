package de.connect2x.messenger.compose.view.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.theme.dp

@Composable
fun TooltipText(text: String, softWrap: Boolean = true) {
    Text(
        text,
        Modifier.padding(MaterialTheme.typography.bodySmall.dp / 2),
        softWrap = softWrap,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onTertiary),
    )
}

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
        TooltipText(cached.value)
    }
}

@Composable
fun TooltipIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Tooltip(
            tooltip = { TooltipText(text) },
            onClick = onClick,
        ) {
            content()
        }
    }
}
