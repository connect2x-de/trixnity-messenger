package de.connect2x.messenger.compose.view.common.modifier

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private val minHeaderHeight = MutableStateFlow(0.dp)

fun resetMinHeaderHeight() {
    minHeaderHeight.value = 0.dp
}

/** Enforces a global minimum header height that grows to match the tallest header. */
fun Modifier.minHeaderHeight(): Modifier = composed {
    val density = LocalDensity.current
    val minHeight by minHeaderHeight.collectAsState()
    this.defaultMinSize(minHeight = minHeight).onGloballyPositioned { coordinates ->
        val actualHeight = with(density) { coordinates.size.height.toDp() }
        minHeaderHeight.update { maxOf(minHeaderHeight.value, actualHeight) }
    }
}
