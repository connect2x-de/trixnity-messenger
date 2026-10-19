package de.connect2x.livekitnity.core.platform

internal actual fun <T : Any> WeakReference(value: T): WeakReference<T> {
  return WasmWeakReference(value)
}

private class WasmWeakReference<T : Any>(value: T) : WeakReference<T> {
  private val reference = JsWeakRef(value.toJsReference())

  override fun get(): T? {
    return reference.deref()?.get()
  }
}

@JsName("WeakRef")
private external class JsWeakRef<T : JsAny>(target: T) {
  fun deref(): T?
}
