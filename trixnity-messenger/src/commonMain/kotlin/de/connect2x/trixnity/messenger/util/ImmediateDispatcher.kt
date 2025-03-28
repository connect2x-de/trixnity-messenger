package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

val CoroutineContext.ImmediateDispatcher : CoroutineDispatcher
    get() = when (val element = this[ImmediateDispatcherElement]) {
        null -> Dispatchers.Main.immediate
        else -> element.dispatcher
    }

val CoroutineScope.ImmediateDispatcher
    get() = coroutineContext.ImmediateDispatcher

suspend fun currentImmediateDispatcher()
        = currentCoroutineContext().ImmediateDispatcher

data class ImmediateDispatcherElement(
    val dispatcher: CoroutineDispatcher
) : AbstractCoroutineContextElement(ImmediateDispatcherElement) {
    companion object Key : CoroutineContext.Key<ImmediateDispatcherElement>

    override fun toString(): String = "ImmediateDispatcher($dispatcher)"
}
