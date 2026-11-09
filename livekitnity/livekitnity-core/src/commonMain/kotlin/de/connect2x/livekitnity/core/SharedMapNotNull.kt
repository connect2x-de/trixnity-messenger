package de.connect2x.livekitnity.core

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class SharedMapNotNull<T, R : Any>(
    private val source: SharedFlow<T>,
    private val mapper: (T) -> R?,
) : SharedFlow<R> {
  override val replayCache: List<R>
    get() = source.replayCache.mapNotNull(mapper)

  override suspend fun collect(collector: FlowCollector<R>): Nothing {
    source.collect { value -> mapper(value)?.let { collector.emit(it) } }
  }
}

internal fun <T, R : Any> SharedFlow<T>.mapSharedNotNull(
    mapper: (T) -> R?,
): SharedFlow<R> = SharedMapNotNull(this, mapper)
