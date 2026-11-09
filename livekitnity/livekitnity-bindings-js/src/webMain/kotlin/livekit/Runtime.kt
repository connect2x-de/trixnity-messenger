@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress(
    // "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "RemoveRedundantBackticks",
    "RemoveExplicitTypeArguments",
    "RedundantUnitReturnType",
    "UnnecessaryOptInAnnotation",
    // "unused",
)

package livekit

import kotlin.js.*

internal fun <T : JsAny?> undefined(): T = js("undefined")

public external interface EventCallback0 : JsAny

internal fun eventCallback0(name: String): EventCallback0 {
  return unsafeCast<EventCallback0>(name)
}

public external interface EventCallback1<T0 : JsAny?> : EventCallback0

internal fun <T0 : JsAny?> eventCallback1(name: String): EventCallback1<T0> {
  return unsafeCast<EventCallback1<T0>>(name)
}

public external interface EventCallback2<T0 : JsAny?, T1 : JsAny?> : EventCallback0

internal fun <T0 : JsAny?, T1 : JsAny?> eventCallback2(name: String): EventCallback2<T0, T1> {
  return unsafeCast<EventCallback2<T0, T1>>(name)
}

public external interface EventCallback3<T0 : JsAny?, T1 : JsAny?, T2 : JsAny?> : EventCallback0

internal fun <T0 : JsAny?, T1 : JsAny?, T2 : JsAny?> eventCallback3(
    name: String,
): EventCallback3<T0, T1, T2> {
  return unsafeCast<EventCallback3<T0, T1, T2>>(name)
}

public external interface EventEmitter : JsAny {
  public fun on(event: EventCallback0, listener: () -> Unit)

  public fun off(event: EventCallback0, listener: () -> Unit)

  public fun <T0 : JsAny?> on(event: EventCallback1<T0>, listener: (T0) -> Unit)

  public fun <T0 : JsAny?> off(event: EventCallback1<T0>, listener: (T0) -> Unit)

  public fun <T0 : JsAny?, T1 : JsAny?> on(
      event: EventCallback2<T0, T1>,
      listener: (T0, T1) -> Unit,
  )

  public fun <T0 : JsAny?, T1 : JsAny?> off(
      event: EventCallback2<T0, T1>,
      listener: (T0, T1) -> Unit,
  )

  public fun <T0 : JsAny?, T1 : JsAny?, T2 : JsAny?> on(
      event: EventCallback3<T0, T1, T2>,
      listener: (T0, T1, T2) -> Unit,
  )

  public fun <T0 : JsAny?, T1 : JsAny?, T2 : JsAny?> off(
      event: EventCallback3<T0, T1, T2>,
      listener: (T0, T1, T2) -> Unit,
  )
}

internal fun <T : JsAny> construct(constructor: Constructor<T>, options: JsAny?): T =
    js("""new constructor(options)""")

internal external interface Constructor<T : JsAny> : JsAny

internal fun <T : JsAny> unsafeCast(value: Boolean): T = js("""value""")

internal fun <T : JsAny> unsafeCast(value: String): T = js("""value""")

internal fun <T : JsAny> unsafeCast(value: Double): T = js("""value""")

internal fun <T : JsAny> unsafeCast(value: JsAny): T = js("""value""")

internal fun <T : JsAny> createObject(): T = js("""({})""")

public external interface Date : JsAny

public external interface JsMap<K : JsAny?, V : JsAny?> : JsAny

public external interface JsRecord<V : JsAny?> : JsAny

public external interface CryptoKey : JsAny

public external interface JsError : JsAny
