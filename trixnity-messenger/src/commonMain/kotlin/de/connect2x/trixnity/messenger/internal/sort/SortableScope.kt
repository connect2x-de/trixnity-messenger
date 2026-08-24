package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import kotlin.reflect.KClass

@TrixnityMessengerPrivateApi
interface SortableScope<in T : Any> {

    fun <I : T> after(clazz: KClass<out I>)

    fun <I : T> before(clazz: KClass<out I>)
}

internal fun <T : Any> SortableScope(
    before: MutableSet<KClass<out T>>,
    after: MutableSet<KClass<out T>>,
): SortableScope<T> {
    return SortableScopeImpl(before = before, after = after)
}

private class SortableScopeImpl<T : Any>(val before: MutableSet<KClass<out T>>, val after: MutableSet<KClass<out T>>) :
    SortableScope<T> {

    override fun <I : T> before(clazz: KClass<out I>) {
        before.add(clazz)
    }

    override fun <I : T> after(clazz: KClass<out I>) {
        after.add(clazz)
    }
}
