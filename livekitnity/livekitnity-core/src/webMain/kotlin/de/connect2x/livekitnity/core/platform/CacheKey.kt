package de.connect2x.livekitnity.core.platform

import kotlin.reflect.KClass

internal class CacheKey<out V : Any>(
    val identity: Any,
    val clazz: KClass<out V>,
)
