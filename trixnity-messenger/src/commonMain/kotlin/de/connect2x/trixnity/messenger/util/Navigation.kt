package de.connect2x.trixnity.messenger.util

import com.arkivanov.decompose.router.stack.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO use context receivers for CoroutineScope in future Kotlin version

private val log = KotlinLogging.logger { }

/**
 * @see [com.arkivanov.decompose.router.stack.replaceCurrent]
 */
suspend fun <C : Any> StackNavigator<C>.replaceCurrentSuspending(
    configuration: C,
    onComplete: () -> Unit = {},
) = withContext(Dispatchers.Main.immediate) {
    log.trace { "replace current with configuration $configuration" }
    replaceCurrent(configuration, onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.replaceCurrent]
 */
fun <C : Any> StackNavigator<C>.launchReplaceCurrent(
    scope: CoroutineScope,
    configuration: C,
    onComplete: () -> Unit = {},
) = scope.launch(Dispatchers.Main.immediate) {
    log.trace { "replace current with configuration $configuration" }
    replaceCurrent(configuration, onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.replaceAll]
 */
suspend fun <C : Any> StackNavigator<C>.replaceAllSuspending(
    configuration: C,
    onComplete: () -> Unit = {},
) = withContext(Dispatchers.Main.immediate) {
    log.trace { "replace all with configuration $configuration" }
    replaceAll(configuration, onComplete = onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.replaceCurrent]
 */
fun <C : Any> StackNavigator<C>.launchReplaceAll(
    scope: CoroutineScope,
    configuration: C,
    onComplete: () -> Unit = {},
) = scope.launch(Dispatchers.Main.immediate) {
    log.trace { "replace all with configuration $configuration" }
    replaceAll(configuration, onComplete = onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.push]
 */
suspend fun <C : Any> StackNavigator<C>.pushSuspending(
    configuration: C,
    onComplete: () -> Unit = {},
) = withContext(Dispatchers.Main.immediate) {
    log.trace { "push configuration $configuration" }
    push(configuration, onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.push]
 */
fun <C : Any> StackNavigator<C>.launchPush(
    scope: CoroutineScope,
    configuration: C,
    onComplete: () -> Unit = {},
) = scope.launch(Dispatchers.Main.immediate) {
    log.trace { "push configuration $configuration" }
    push(configuration, onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.pop]
 */
suspend fun <C : Any> StackNavigator<C>.popSuspending(
    onComplete: (isSuccess: Boolean) -> Unit = {}
) = withContext(Dispatchers.Main.immediate) {
    log.trace { "pop current configuration" }
    pop(onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.pop]
 */
fun <C : Any> StackNavigator<C>.launchPop(
    scope: CoroutineScope,
    onComplete: (isSuccess: Boolean) -> Unit = {}
) = scope.launch(Dispatchers.Main.immediate) {
    log.trace { "pop current configuration" }
    pop(onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.popWhile]
 */
suspend fun <C : Any> StackNavigator<C>.popWhileSuspending(
    predicate: (C) -> Boolean
) = withContext(Dispatchers.Main.immediate) {
    log.trace { "popWhile current configuration" }
    popWhile(predicate)
}

/**
 * @see [com.arkivanov.decompose.router.stack.popWhile]
 */
fun <C : Any> StackNavigator<C>.launchPopWhile(
    scope: CoroutineScope,
    predicate: (C) -> Boolean
) = scope.launch(Dispatchers.Main.immediate) {
    log.trace { "popWhile current configuration" }
    popWhile(predicate)
}

/**
 * @see [com.arkivanov.decompose.router.stack.popWhile]
 */
suspend fun <C : Any> StackNavigator<C>.popWhileSuspending(
    predicate: (C) -> Boolean,
    onComplete: (isSuccess: Boolean) -> Unit,
) = withContext(Dispatchers.Main.immediate) {
    log.trace { "popWhile current configuration" }
    popWhile(predicate, onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.popWhile]
 */
suspend fun <C : Any> StackNavigator<C>.launchPopWhile(
    scope: CoroutineScope,
    predicate: (C) -> Boolean,
    onComplete: (isSuccess: Boolean) -> Unit,
) = scope.launch(Dispatchers.Main.immediate) {
    log.trace { "popWhile current configuration" }
    popWhile(predicate, onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.bringToFront]
 */
suspend fun <C : Any> StackNavigator<C>.bringToFrontSuspending(
    configuration: C,
    onComplete: () -> Unit = {}
) = withContext(Dispatchers.Main.immediate) {
    log.trace { "bring to front $configuration" }
    bringToFront(configuration, onComplete)
}

/**
 * @see [com.arkivanov.decompose.router.stack.bringToFront]
 */
fun <C : Any> StackNavigator<C>.launchBringToFront(
    scope: CoroutineScope,
    configuration: C,
    onComplete: () -> Unit = {}
) = scope.launch(Dispatchers.Main.immediate) {
    log.trace { "bring to front $configuration" }
    bringToFront(configuration, onComplete)
}