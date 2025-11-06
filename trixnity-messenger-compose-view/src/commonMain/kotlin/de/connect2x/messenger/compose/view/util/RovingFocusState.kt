package de.connect2x.messenger.compose.view.util

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.collections.get

class RovingFocusState(
    val coroutineScope: CoroutineScope,
    val activeRef: MutableState<Any?> = mutableStateOf(null),
    val references: SnapshotStateMap<Any, FocusRequester> = mutableStateMapOf(),
) {
    var hasFocus: Boolean = false
    var isFocussing: Boolean = false
        private set

    fun selectItem(item: Any?, shouldFocus: Boolean = hasFocus, scroll: suspend CoroutineScope.() -> Unit = {}) {
        activeRef.value = item
        if (shouldFocus) {
            references[item]?.let {
                withFocus {
                    it.requestFocus(FocusDirection.Down)
                }
                coroutineScope.launch(block = scroll)
            } ?: coroutineScope.launch {
                scroll()
                withFocus {
                    references[item]?.requestFocus(FocusDirection.Down)
                }
            }
        }
    }

    private inline fun withFocus(crossinline handler: () -> Unit) {
        isFocussing = true
        try {
            handler()
        } finally {
            isFocussing = false
        }
    }
}

val LocalRovingFocus = staticCompositionLocalOf<RovingFocusState?> { null }

@Composable
fun RovingFocusContainer(content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    CompositionLocalProvider(
        LocalRovingFocus provides remember { RovingFocusState(coroutineScope) },
        content = content,
    )
}

data class RovingFocusItemScope(val key: Any, val default: Any?)

val LocalRovingFocusItem = staticCompositionLocalOf<RovingFocusItemScope?> { null }

@Composable
fun RovingFocusItem(
    key: Any,
    default: Any? = null,
    content: @Composable () -> Unit,
) {
    val scope = remember(key, default) { RovingFocusItemScope(key, default) }
    CompositionLocalProvider(
        LocalRovingFocusItem provides scope,
        content = content,
    )
}

@Composable
fun Modifier.rovingFocusItem(): Modifier {
    val focusRequester = remember { FocusRequester() }
    val rovingFocusState = LocalRovingFocus.current ?: return this
    val scope = LocalRovingFocusItem.current ?: return this

    DisposableEffect(scope.key) {
        rovingFocusState.references[scope.key] = focusRequester
        onDispose {
            rovingFocusState.references.remove(scope.key)
        }
    }

    return this then Modifier
        .focusProperties {
            val current = rovingFocusState.activeRef.value ?: scope.default
            val focusable = rovingFocusState.isFocussing || current == scope.key
            if (!focusable) {
                canFocus = false
            }
        }
        .focusRequester(focusRequester)
}

@Composable
fun Modifier.rovingFocusChild(): Modifier {
    val rovingFocusState = LocalRovingFocus.current ?: return this
    val scope = LocalRovingFocusItem.current ?: return this
    return this then Modifier
        .focusProperties {
            val current = rovingFocusState.activeRef.value ?: scope.default
            val focusable = rovingFocusState.isFocussing || current == scope.key
            if (!focusable) {
                canFocus = false
            }
        }
}

@Composable
fun Modifier.verticalRovingFocus(
    default: Any? = null,
    up: RovingFocusState.() -> Any?,
    down: RovingFocusState.() -> Any?,
    scroll: suspend CoroutineScope.(Any?) -> Unit = {},
): Modifier {
    val state = LocalRovingFocus.current ?: return this
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(state, default) {
        val item = state.activeRef.value ?: default
        state.selectItem(item) { scroll(item) }
    }

    return this then Modifier
        .onFocusEvent {
            state.hasFocus = it.hasFocus
            if (it.isFocused) {
                val item = state.activeRef.value ?: default
                state.selectItem(item) { scroll(item) }
            }
        }
        .focusProperties {
            val item = state.activeRef.value ?: default
            if (state.references[item] != null) {
                canFocus = false
            }
        }
        .focusable(interactionSource = interactionSource)
        .onKeyEvent { event ->
            println("Key Event: $event")
            when (event.key) {
                Key.DirectionUp -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.up()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                Key.DirectionDown -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.down()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                Key.DirectionLeft -> true
                Key.DirectionRight -> true

                else -> false
            }
        }
}

@Composable
fun Modifier.horizontalRovingFocus(
    default: Any? = null,
    left: RovingFocusState.() -> Any?,
    right: RovingFocusState.() -> Any?,
    scroll: suspend CoroutineScope.(Any?) -> Unit = {},
): Modifier {
    val state = LocalRovingFocus.current ?: return this
    val interactionSource = remember { MutableInteractionSource() }

    return this then Modifier
        .onFocusEvent {
            state.hasFocus = it.hasFocus
            if (it.isFocused) {
                val item = state.activeRef.value ?: default
                state.selectItem(item) { scroll(item) }
            }
        }
        .focusProperties {
            val item = state.activeRef.value ?: default
            if (state.references[item] != null) {
                canFocus = false
            }
        }
        .focusable(interactionSource = interactionSource)
        .onKeyEvent { event ->
            when (event.key) {
                Key.DirectionLeft -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.left()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                Key.DirectionRight -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.right()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                Key.DirectionUp -> true
                Key.DirectionDown -> true

                else -> false
            }
        }
}

@Composable
fun Modifier.rovingFocus2D(
    default: Any? = null,
    up: RovingFocusState.() -> Any?,
    down: RovingFocusState.() -> Any?,
    left: RovingFocusState.() -> Any?,
    right: RovingFocusState.() -> Any?,
    scroll: suspend CoroutineScope.(Any?) -> Unit = {},
): Modifier {
    val state = LocalRovingFocus.current ?: return this
    val interactionSource = remember { MutableInteractionSource() }

    return this then Modifier
        .onFocusEvent {
            state.hasFocus = it.hasFocus
            if (it.isFocused) {
                val item = state.activeRef.value ?: default
                state.selectItem(item) { scroll(item) }
            }
        }
        .focusProperties {
            val item = state.activeRef.value ?: default
            if (state.references[item] != null) {
                canFocus = false
            }
        }
        .focusable(interactionSource = interactionSource)
        .onKeyEvent { event ->
            when (event.key) {
                Key.DirectionUp -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.up()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                Key.DirectionDown -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.down()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                Key.DirectionLeft -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.left()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                Key.DirectionRight -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val item = state.right()
                        state.selectItem(item) { scroll(item) }
                    }
                    true
                }

                else -> false
            }
        }
}
