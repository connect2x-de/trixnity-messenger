package de.connect2x.livekitnity.core.platform

import java.lang.ref.WeakReference
import kotlin.reflect.cast

internal class WeakIdentityCache {
  private class Entry<V : Any>(key: CacheKey<V>, value: V) {
    val identity = WeakReference(key.identity)
    val clazz = key.clazz
    val value = WeakReference(value)
  }

  private val entries = mutableListOf<Entry<*>>()

  fun <V : Any> getOrCreate(key: CacheKey<V>, create: () -> V): V {
    val iterator = entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      val identity = entry.identity.get()
      val value = entry.value.get()
      if (identity == null || value == null) {
        iterator.remove()
      } else if (identity === key.identity && entry.clazz == key.clazz) {
        return key.clazz.cast(value)
      }
    }
    val value = create()
    entries.add(Entry(key, value))
    return value
  }

  fun clear() {
    entries.clear()
  }
}
