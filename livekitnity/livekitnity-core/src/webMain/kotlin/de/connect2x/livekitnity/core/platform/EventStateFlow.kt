package de.connect2x.livekitnity.core.platform

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import livekit.EventEmitter

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal interface EventStateFlow<T> : StateFlow<T>

internal fun <E : EventEmitter, T> EventStateFlow(
    eventEmitter: E,
    converter: Converter,
    descriptor: StateDescriptor<E, T>,
): EventStateFlow<T> {
  return EventStateFlowImpl(
      getValue = { descriptor.getState(eventEmitter, converter) },
      subscribe = subscribe@{ onEvent ->
            descriptor.events.forEach { event ->
              eventEmitter.on(event, onEvent)
            }
            return@subscribe {
              descriptor.events.forEach { event ->
                eventEmitter.off(event, onEvent)
              }
            }
          },
  )
}

private class EventStateFlowImpl<T>(
    private val getValue: () -> T,
    private val subscribe: (onEvent: () -> Unit) -> () -> Unit,
) : EventStateFlow<T> {

  override val value: T
    get() = getValue()

  override val replayCache: List<T>
    get() = listOf(value)

  private var refCount = 0
  private var unsubscribe: (() -> Unit)? = null
  private val stateFlow = MutableStateFlow(value)

  private val onEvent: () -> Unit = {
    queueMicrotask { stateFlow.value = getValue() }
  }

  override suspend fun collect(collector: FlowCollector<T>): Nothing {
    try {
      registerIfNecessary()
      stateFlow.collect(collector)
    } finally {
      unregisterIfNecessary()
    }
  }

  private fun registerIfNecessary() {
    if (refCount++ != 0) return

    unsubscribe = subscribe(onEvent)
    onEvent()
  }

  private fun unregisterIfNecessary() {
    if (--refCount != 0) return

    val cleanup = unsubscribe
    unsubscribe = null
    cleanup?.invoke()
  }
}

@JsFun("""task => queueMicrotask(task)""") private external fun queueMicrotask(task: () -> Unit)
