package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce

@Composable
internal fun <T : R, R> StateFlow<T>.collectAsStateDebounced(
    context: CoroutineContext = EmptyCoroutineContext,
    timeout: (T) -> Duration,
): State<R> {
    val debouncedFlow = remember(this@collectAsStateDebounced) { @OptIn(FlowPreview::class) debounce(timeout) }

    return debouncedFlow.collectAsState(value, context)
}
