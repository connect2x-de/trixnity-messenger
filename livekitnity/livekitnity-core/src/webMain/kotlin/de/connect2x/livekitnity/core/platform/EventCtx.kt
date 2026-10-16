package de.connect2x.livekitnity.core.platform

import livekit.EventEmitter

internal interface EventCtx<E : EventEmitter, T> {
  val eventEmitter: E

  fun onEvent(event: T)
}
