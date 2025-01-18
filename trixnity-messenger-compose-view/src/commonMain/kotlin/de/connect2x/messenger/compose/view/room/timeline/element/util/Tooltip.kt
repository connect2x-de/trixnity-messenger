package de.connect2x.messenger.compose.view.room.timeline.element.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.Tooltip
import kotlin.time.Duration


@Composable
fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    delay: Duration,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) = Tooltip(
    tooltip,
    modifier,
    delay.inWholeMilliseconds.toInt(),
    onClick,
    content,
)
