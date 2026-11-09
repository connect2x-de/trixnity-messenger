package de.connect2x.livekitnity.core.platform

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import livekit.EventEmitter

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal interface EventSharedFlow<T> : SharedFlow<T>

internal fun <C : EventCtx<E, T>, E : EventEmitter, T> EventSharedFlow(
    makeCtx: (onEvent: (T) -> Unit) -> C,
    descriptors: List<EventDescriptor<C>>,
): EventSharedFlow<T> {
  return EventSharedFlowImpl { onEvent ->
    descriptors.register(makeCtx(onEvent))
  }
}

private class EventSharedFlowImpl<T>(
    private val subscribe: (onEvent: (T) -> Unit) -> () -> Unit,
) : EventSharedFlow<T> {

  private val sharedFlow = MutableSharedFlow<T>(extraBufferCapacity = Int.MAX_VALUE)

  override val replayCache: List<T>
    get() = sharedFlow.replayCache

  private var refCount = 0
  private var unsubscribe: (() -> Unit)? = null

  private val onEvent: (T) -> Unit = {
    check(sharedFlow.tryEmit(it)) {
      "Event buffer capacity exceeded"
    }
  }

  override suspend fun collect(collector: FlowCollector<T>): Nothing {
    try {
      registerIfNecessary()
      sharedFlow.collect(collector)
    } finally {
      unregisterIfNecessary()
    }
  }

  private fun registerIfNecessary() {
    if (refCount++ != 0) return

    unsubscribe = subscribe(onEvent)
  }

  private fun unregisterIfNecessary() {
    if (--refCount != 0) return

    val cleanup = unsubscribe
    unsubscribe = null
    cleanup?.invoke()
  }
}
