package de.connect2x.livekitnity.core.platform

internal actual fun <T : Any> WeakReference(value: T): WeakReference<T> {
  return JsWeakReference(value)
}

private class JsWeakReference<T : Any>(value: T) : WeakReference<T> {
  private val reference = JsWeakRef(value)

  override fun get(): T? {
    return reference.deref()
  }
}

@JsName("WeakRef")
private external class JsWeakRef<T : Any>(target: T) {
  fun deref(): T?
}
