package de.connect2x.livekitnity.demo.shared.optimistic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
public fun <T> rememberOptimisticState(
    sourceValue: T,
    debounceDuration: Duration = 250.milliseconds,
    onError: (Exception) -> Unit = {},
    update: suspend (T) -> Unit,
): MutableState<T> {
  val source = rememberUpdatedState(sourceValue)

  return rememberOptimisticState(
      source = source,
      debounceDuration = debounceDuration,
      onError = onError,
      update = update,
  )
}

@Composable
public fun <T> rememberOptimisticState(
    source: State<T>,
    debounceDuration: Duration = 250.milliseconds,
    onError: (Exception) -> Unit = {},
    update: suspend (T) -> Unit,
): MutableState<T> {
  val latestUpdate = rememberUpdatedState(update)
  val latestOnError = rememberUpdatedState(onError)

  val state = remember { OptimisticState(source) }
  val request = state.pending

  LaunchedEffect(request, debounceDuration) {
    if (request == null) return@LaunchedEffect

    try {
      delay(debounceDuration)
      latestUpdate.value(request.value)

      snapshotFlow { source.value }.first { it == request.value }

      state.clear(request)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (state.clear(request)) {
        latestOnError.value(e)
      }
    }
  }

  return state
}
