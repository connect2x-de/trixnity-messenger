package de.connect2x.livekitnity.core.platform

import livekit.EventCallback0
import livekit.EventCallback1
import livekit.EventCallback2
import livekit.EventCallback3
import livekit.EventEmitter

internal typealias Unsubscribe = () -> Unit

internal fun interface EventDescriptor<C> {
  fun register(context: C): Unsubscribe
}

internal fun <C> Iterable<EventDescriptor<C>>.register(
    context: C,
): Unsubscribe {
  val subscriptions = map { it.register(context) }
  return { subscriptions.forEach { it() } }
}

internal fun <C : EventCtx<E, T>, E : EventEmitter, T> EventDescriptor(
    event: EventCallback0,
    mapEvent: C.() -> T,
): EventDescriptor<C> {
  return EventDescriptor { context ->
    val listener = { context.onEvent(mapEvent(context)) }
    context.eventEmitter.on(event, listener)

    val unsubscribe: Unsubscribe = {
      context.eventEmitter.off(event, listener)
    }
    unsubscribe
  }
}

internal fun <Ctx : EventCtx<E, T>, E : EventEmitter, T, A : JsAny?> EventDescriptor(
    event: EventCallback1<A>,
    mapEvent: Ctx.(A) -> T,
): EventDescriptor<Ctx> {
  return EventDescriptor { context ->
    val listener = { a: A ->
      context.onEvent(mapEvent(context, a))
    }
    context.eventEmitter.on(event, listener)

    val unsubscribe: Unsubscribe = {
      context.eventEmitter.off(event, listener)
    }
    unsubscribe
  }
}

internal fun <
    Ctx : EventCtx<E, T>,
    E : EventEmitter,
    T,
    A : JsAny?,
    B : JsAny?,
> EventDescriptor(
    event: EventCallback2<A, B>,
    mapEvent: Ctx.(A, B) -> T,
): EventDescriptor<Ctx> {
  return EventDescriptor { context ->
    val listener = { a: A, b: B ->
      context.onEvent(mapEvent(context, a, b))
    }
    context.eventEmitter.on(event, listener)

    val unsubscribe: Unsubscribe = {
      context.eventEmitter.off(event, listener)
    }
    unsubscribe
  }
}

internal fun <
    Ctx : EventCtx<E, T>,
    E : EventEmitter,
    T,
    A : JsAny?,
    B : JsAny?,
    C : JsAny?,
> EventDescriptor(
    event: EventCallback3<A, B, C>,
    mapEvent: Ctx.(A, B, C) -> T,
): EventDescriptor<Ctx> {
  return EventDescriptor { context ->
    val listener = { a: A, b: B, c: C ->
      context.onEvent(mapEvent(context, a, b, c))
    }
    context.eventEmitter.on(event, listener)

    val unsubscribe: Unsubscribe = {
      context.eventEmitter.off(event, listener)
    }
    unsubscribe
  }
}
