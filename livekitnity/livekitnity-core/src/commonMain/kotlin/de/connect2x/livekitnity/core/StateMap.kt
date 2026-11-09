package de.connect2x.livekitnity.core

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class StateMap<T, R>(
    private val source: StateFlow<T>,
    private val mapper: (T) -> R,
) : StateFlow<R> {

  override val value: R
    get() = mapper(source.value)

  override val replayCache: List<R>
    get() = listOf(value)

  override suspend fun collect(collector: FlowCollector<R>): Nothing {
    source.collect { value ->
      collector.emit(mapper(value))
    }
  }
}

internal fun <T, R> StateFlow<T>.mapState(
    mapper: (T) -> R,
): StateFlow<R> = StateMap(this, mapper)
