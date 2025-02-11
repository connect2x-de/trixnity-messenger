package de.connect2x.trixnity.messenger.viewmodel.util

import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformWhile
import kotlin.time.Duration

fun <T> Flow<T>.takeWhileInclusive(pred: suspend (T) -> Boolean): Flow<T> =
    transformWhile { value ->
        if (pred(value)) {
            emit(value)
            true
        } else {
            emit(value)
            false
        }
    }

fun <T> Flow<T>.throttleFirst(delay: Long): Flow<T> =
    transform {
        emit(it)
        delay(delay)
    }

fun <T> Flow<T>.throttleFirst(delay: Duration): Flow<T> =
    throttleFirst(delay.inWholeMilliseconds)

fun <T> Flow<T>.debounceAfterFirst(delay: Duration): Flow<T> =
    channelFlow {
        var didEmitFirst = false
        collectLatest {
            if (didEmitFirst) delay(delay)
            didEmitFirst = true
            send(it)
        }
    }

fun <T : Any> Value<T>.toFlow(): Flow<T> = callbackFlow {
    val cancelable = subscribe { trySend(it) }
    awaitClose {
        cancelable.cancel()
    }
}

fun <T : Any> List<T>.asReversedFlow(): Flow<T> = flow {
    for (index in lastIndex downTo 0) {
        emit(get(index))
    }
}

fun <T : Any> List<T>.asReversedIndexedFlow(): Flow<IndexedValue<T>> = flow {
    for (index in lastIndex downTo 0) {
        emit(IndexedValue(index, get(index)))
    }
}

internal fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    @Suppress("UNCHECKED_CAST")
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

internal fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
    @Suppress("UNCHECKED_CAST")
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
        args[6] as T7,
    )
}
