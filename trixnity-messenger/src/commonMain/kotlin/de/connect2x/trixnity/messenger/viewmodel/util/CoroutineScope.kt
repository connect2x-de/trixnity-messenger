package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transformLatest

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T>.scopedMapLatest(block: suspend CoroutineScope.(T) -> R): Flow<R> {
    return this.transformLatest { coroutineScope { emit(block(it)) } }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T>.scopedFlatMapLatest(block: suspend CoroutineScope.(T) -> Flow<R>): Flow<R> {
    return this.flatMapLatest { coroutineScope { block(it) } }
}

suspend fun <T> Flow<T>.scopedCollectLatest(block: suspend CoroutineScope.(T) -> Unit) {
    this.collectLatest { coroutineScope { block(it) } }
}
