package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlin.reflect.KClass

@TrixnityMessengerPrivateApi
interface SortedMetadata<out T : Any> {
    val clazz: KClass<out T>
    val before: Set<KClass<out T>>
    val after: Set<KClass<out T>>
}

internal fun <T : Any> SortedMetadata(clazz: KClass<out T>, builder: SortableScope<T>.() -> Unit): SortedMetadata<T> {
    val before = mutableSetOf<KClass<out T>>()
    val after = mutableSetOf<KClass<out T>>()

    SortableScope(before = before, after = after).builder()

    return SortedMetadataImpl(clazz = clazz, before = before, after = after)
}

private class SortedMetadataImpl<out T : Any>(
    override val clazz: KClass<out T>,
    override val before: Set<KClass<out T>>,
    override val after: Set<KClass<out T>>,
) : SortedMetadata<T>
