package de.connect2x.trixnity.messenger.uikit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal object Utilities {
    fun CoroutineScope.delegate(completionHandler: () -> Unit, action: suspend () -> Unit) {
        launch {
            action()
            completionHandler()
        }
    }

    fun <R> CoroutineScope.delegate(
        completionHandler: (R) -> Unit,
        default: () -> R,
        action: suspend () -> WithDefault<R>,
    ) {
        launch { completionHandler(action().valueOr(default)) }
    }

    fun <T> List<T>.delegate(action: T.() -> Unit) = delegate(combiner = {}, action = action)

    fun <T, R> List<T>.delegate(combiner: (List<R>) -> R, action: T.() -> R): R = combiner(map { it.action() })

    suspend fun <T> List<T>.delegateSuspend(action: suspend T.() -> Unit) =
        delegateSuspend(combiner = {}, action = action)

    suspend fun <T, R> List<T>.delegateSuspend(combiner: (List<R>) -> R, action: suspend T.() -> R): R =
        coroutineScope {
            combiner(map { async { action(it) } }.awaitAll())
        }

    fun <T> checkingCombiner(items: List<WithDefault<T>>): WithDefault<T> {
        var result: WithDefault<T> = WithDefault.Default

        for (item in items) {
            if (item is WithDefault.Value && result is WithDefault.Value) {
                error("Multiple non default values are not allowed")
            }

            if (item is WithDefault.Value) result = item
        }

        return result
    }

    inline fun <T, reified R> T.unsafeCast(): R = this as R
}
