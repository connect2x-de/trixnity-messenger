package de.connect2x.livekitnity.core.platform

import livekit.EventCallback0
import livekit.EventEmitter

internal class StateDescriptor<in E : EventEmitter, T>(
    val events: List<EventCallback0>,
    val getState: (E, Converter) -> T,
) {
  fun <R> map(map: (T) -> R): StateDescriptor<E, R> {
    return StateDescriptor(
        events = events,
        getState = { eventEmitter, converter -> map(getState(eventEmitter, converter)) },
    )
  }
}

internal fun <E : EventEmitter, T> StateDescriptor(
    events: List<EventCallback0>,
    getState: E.() -> T,
): StateDescriptor<E, T> {
  return StateDescriptor(
      events = events,
      getState = { eventListener, _ -> getState(eventListener) },
  )
}

internal fun <E : EventEmitter, T, R> StateDescriptor(
    events: List<EventCallback0>,
    getState: E.() -> T,
    mapState: (T) -> R,
): StateDescriptor<E, R> {
  return StateDescriptor(
      events = events,
      getState = { eventListener, _ -> mapState(getState(eventListener)) },
  )
}

internal fun <E : EventEmitter, T, R> StateDescriptor(
    events: List<EventCallback0>,
    getState: E.() -> T,
    mapState: Converter.(T) -> R,
): StateDescriptor<E, R> {
  return StateDescriptor(
      events = events,
      getState = { eventListener, converter -> mapState(converter, getState(eventListener)) },
  )
}
