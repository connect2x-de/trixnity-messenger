package de.connect2x.trixnity.messenger.viewmodel.util

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T>.scopedMapLatest(block: suspend CoroutineScope.(T) -> R): Flow<R> {
    return this.transformLatest {
        coroutineScope {
            emit(block(it))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T>.scopedFlatMapLatest(block: suspend CoroutineScope.(T) -> Flow<R>): Flow<R> {
    return this.flatMapLatest {
        coroutineScope {
            block(it)
        }
    }
}

suspend fun <T> Flow<T>.scopedCollectLatest(block: suspend CoroutineScope.(T) -> Unit) {
    this.collectLatest {
        coroutineScope {
            block(it)
        }
    }
}

fun LifecycleOwner.coroutineScope(
    context: CoroutineContext,
    description: String? = null,
): CoroutineScope = coroutineScope(lifecycle, context, description)

fun coroutineScope(
    lifecycle: Lifecycle,
    context: CoroutineContext,
    description: String? = null,
): CoroutineScope {
    val handler = CoroutineExceptionHandler { _, exception ->
        log.error(exception) { "coroutine scope with lifecycle has been cancelled${if (description != null) "($description)" else ""}" }
        // TODO close app
    }
    val scope = CoroutineScope(
        context
            + SupervisorJob(context[Job])
            + handler
    )
    lifecycle.doOnDestroy(scope::cancel)
    return scope
}
