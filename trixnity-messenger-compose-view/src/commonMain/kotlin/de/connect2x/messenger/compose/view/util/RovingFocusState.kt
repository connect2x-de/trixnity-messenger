package de.connect2x.messenger.compose.view.util

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.center
import de.connect2x.messenger.compose.view.common.modifier.focusOnFirstRender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

/**
 * This is a wrapper around containers that contain multiple elements (like Column, Row, Grid, etc.) which should be
 * navigable with the arrow keys and be tabbed over if TAB is pressed after one element is focused. Contained
 * elements have to be wrapped in [RovingFocusItem] and get the [Modifier.rovingFocusItem].
 *
 * The TAB behavior on an item element can be changed if sub-elements of this item contain [rovingFocusChild]ren. Then,
 * pressing TAB will navigate to the first roving focus child and pressing TAB again will navigate to subsequent
 * children. If the end of all sub-children is reached, pressing TAB will step outside the [RovingFocusContainer].
 *
 * To activate keyboard navigation, the [Modifier.verticalRovingFocus] or its siblings (horizontal, 2D) have to be
 * applied to the container. Be sure to provide only navigable elements to it (disabled elements cannot be focused and
 * thus should not be part of the list). As a consequence of providing such a list of navigable elements, the
 * [RovingFocusContainer] can only be used on known lists of elements.
 *
 * Example:
 * ```kotlin
 * RovingFocusContainer {
 *   val listState = rememberLazyListState()
 *   val elements: List<Element> = ...
 *   val allItems = elements.filter { it.isEnabled() }.map { it.stringRepresentation() }
 *   val defaultItem = allItems.first()
 *   LazyColumn(
 *      modifier = Modifier
 *        .verticalRovingFocus(
 *          default = defaultItem,
 *          scroll = scroll(listState, allItems) { it },
 * .        up = { getPreviousItem(allItems, defaultItem) { it } },
 *          down = { getNextItem(allItems, defaultItem) { it } }
 *        ),
 *        // ...
 *   ) {
 *      // RovingFocusItems
 *   }
 * }
 * ```
 *
 */
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

/**
 * An item inside a container for multiple items (like Column, Row, Grid, etc.) that can be navigated to only with the
 * arrow keys and should be tabbed over if TAB is pressed while it or its siblings is being focused. Has to be used
 * inside [RovingFocusContainer].
 *
 * The element inside [RovingFocusItem] has to have the [Modifier.rovingFocusItem].
 *
 * Example
 * ```kotlin
 * RovingFocusContainer {
 *   LazyColumn {
 *      // ..
 *      items(allItems, key = { it }) { item ->
 *        RovingFocusItem(key = item) {
 *          MyElement(item, modifier = Modifier
 *            .rovingFocusItem()
 *          )
 *        }
 *      }
 *   }
 * }
 * ```
 */
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

/**
 * @see [RovingFocusItem]
 */
@Composable
fun Modifier.rovingFocusItem(focusOnFirstRender: Boolean = false): Modifier {
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
        .then(if (focusOnFirstRender) Modifier.focusOnFirstRender(focusRequester) else Modifier)
}

/**
 * Can be used to denote sub-elements of items inside a [RovingFocusContainer]. Pressing TAB on an item normally
 * would leave the [RovingFocusContainer], but with this Modifier, the children of the item will be focused instead.
 *
 * @see [RovingFocusContainer]
 */
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

/**
 * Navigate with arrow keys UP and DOWN inside a container that aligns its children vertically (like Column).
 *
 * @see [RovingFocusContainer]
 */
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
            when (event.key) {
                Key.DirectionUp -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val nextItem = state.up()
                        state.selectItem(nextItem) { scroll(nextItem) }
                    }
                    true
                }

                Key.DirectionDown -> {
                    if (event.type == KeyEventType.KeyDown) {
                        val nextItem = state.down()
                        state.selectItem(nextItem) { scroll(nextItem) }
                    }
                    true
                }

                Key.DirectionLeft -> true
                Key.DirectionRight -> true

                else -> false
            }
        }
}

fun <T> RovingFocusState.getPreviousItem(references: List<T>, defaultItem: Any?, key: (T) -> Any?): Any? {
    val currentItem = activeRef.value ?: defaultItem
    val currentIndex = references.indexOfFirst { key(it) == currentItem }
    val nextIndex = currentIndex.minus(1).coerceIn(references.indices)
    return references[nextIndex]?.let { key(it) }
}

fun <T> RovingFocusState.getNextItem(references: List<T>, defaultItem: Any?, key: (T) -> Any?): Any? {
    val currentItem = activeRef.value ?: defaultItem
    val currentIndex = references.indexOfFirst { key(it) == currentItem }
    val nextIndex = currentIndex.plus(1).coerceIn(references.indices)
    return references[nextIndex]?.let { key(it) }
}

fun <T> RovingFocusState.moveUp(
    defaultItem: Any?,
    references: List<T>,
    key: (T) -> Any,
    onMove: suspend CoroutineScope.(Any?) -> Unit = {},
) {
    val nextItem = getPreviousItem(references, defaultItem, key)
    selectItem(nextItem) { onMove(nextItem) }
}

fun <T> RovingFocusState.moveDown(
    defaultItem: Any?,
    references: List<T>,
    key: (T) -> Any,
    onMove: suspend CoroutineScope.(Any?) -> Unit = {},
) {
    val nextItem = getNextItem(references, defaultItem, key)
    selectItem(nextItem) { onMove(nextItem) }
}

fun <T> scroll(
    state: LazyListState,
    allItems: List<T>,
    horizontal: Boolean = false,
    key: (T) -> Any,
): suspend CoroutineScope.(Any?) -> Unit =
    { item ->
        val index = allItems.indexOfFirst { key(it) == item }
        val center = state.layoutInfo.viewportSize.center
        val halfItemSize = state.layoutInfo.visibleItemsInfo.find { it.key == item }?.let { it.size / 2 } ?: 0
        if (index != -1) {
            state.animateScrollToItem(index, if (horizontal) -(center.x - halfItemSize) else -(center.y - halfItemSize))
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
