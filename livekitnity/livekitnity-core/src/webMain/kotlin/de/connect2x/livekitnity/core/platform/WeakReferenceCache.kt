package de.connect2x.livekitnity.core.platform

import kotlin.reflect.cast

internal class WeakIdentityCache {
  private class Entry<V : Any>(
      key: CacheKey<V>,
      value: V,
  ) {
    val identity = WeakReference(key.identity)
    val type = key.clazz
    val value = WeakReference(value)
  }

  private val entries = mutableListOf<Entry<*>>()

  fun <V : Any> getOrCreate(
      key: CacheKey<V>,
      create: () -> V,
  ): V {
    val iterator = entries.iterator()

    while (iterator.hasNext()) {
      val entry = iterator.next()
      val cachedIdentity = entry.identity.get()
      val cachedValue = entry.value.get()

      if (cachedIdentity == null || cachedValue == null) {
        iterator.remove()
      } else if (cachedIdentity === key.identity && entry.type == key.clazz) {
        return key.clazz.cast(cachedValue)
      }
    }

    return create().also {
      entries.add(Entry(key, it))
    }
  }

  fun clear() {
    entries.clear()
  }
}

internal interface WeakReference<T : Any> {
  fun get(): T?
}

internal expect fun <T : Any> WeakReference(value: T): WeakReference<T>
