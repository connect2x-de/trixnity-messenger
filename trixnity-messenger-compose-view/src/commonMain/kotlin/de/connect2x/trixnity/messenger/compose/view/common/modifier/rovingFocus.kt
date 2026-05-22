package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import kotlin.reflect.KClass
import kotlin.reflect.cast

enum class RovingFocusDirection(internal val directions: List<FocusDirection>) {
    Vertical(listOf(FocusDirection.Up, FocusDirection.Down)),
    Horizontal(listOf(FocusDirection.Left, FocusDirection.Right)),
    Grid(listOf(FocusDirection.Up, FocusDirection.Down, FocusDirection.Left, FocusDirection.Right)),
}

@PublishedApi
@Composable
internal fun <T : Any> Modifier.rovingFocusContainer(
    direction: RovingFocusDirection = RovingFocusDirection.Vertical,
    listState: LazyListState?,
    focusedItem: MutableState<T?>?,
    focusedItemClass: KClass<T>?,
    ignoredKeys: List<Any>,
): Modifier {
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val moveFocus =
        remember(focusManager, inputModeManager) {
            { focusDirection: FocusDirection ->
                inputModeManager.requestInputMode(InputMode.Keyboard)
                focusManager.moveFocus(focusDirection)
            }
        }
    var isInternalFocus by remember { mutableStateOf(false) }

    return this.moveFocusOnDirection(moveFocus, direction.directions).let { modifier ->
        if (listState != null && focusedItem != null && focusedItemClass != null) {
            modifier
                .onFocusChanged { isInternalFocus = it.hasFocus }
                .focusProperties {
                    onEnter = {
                        if (requestedFocusDirection.isTab() && !isInternalFocus) {
                            val filteredKeys =
                                listState.layoutInfo.visibleItemsInfo.map { itemInfo -> itemInfo.key } - ignoredKeys
                            if (filteredKeys.none { key -> key == focusedItem.value }) {
                                if (filteredKeys.isEmpty()) {
                                    focusedItem.value = null
                                } else {
                                    val key =
                                        if (
                                            (requestedFocusDirection == FocusDirection.Previous) xor
                                                listState.layoutInfo.reverseLayout
                                        ) {
                                            filteredKeys.last()
                                        } else {
                                            filteredKeys.first()
                                        }
                                    require(key::class == focusedItemClass) {
                                        "The class of the LazyList's item key was not equal to the class of focusedItem in rovingFocusContainer."
                                    }
                                    focusedItem.value = focusedItemClass.cast(key)
                                }
                            }
                            isInternalFocus = true
                        }
                    }
                }
        } else {
            modifier
        }
    }
}

/**
 * This should be used for containers that stops loading items if they are scrolled out of the viewport (Using the
 * mouse), such as LazyColumns. In that case this will either set listState.layoutInfo.visibleItemsInfo.last().key or
 * listState.layoutInfo.visibleItemsInfo.first().key as the new focusedItem depending on the scroll direction
 */
@Composable
inline fun <reified T : Any> Modifier.rovingFocusContainer(
    direction: RovingFocusDirection = RovingFocusDirection.Vertical,
    listState: LazyListState? = null,
    focusedItem: MutableState<T?>,
    ignoredKeys: List<Any> = emptyList(),
): Modifier =
    rovingFocusContainer(
        direction = direction,
        listState = listState,
        focusedItem = focusedItem,
        focusedItemClass = T::class,
        ignoredKeys = ignoredKeys,
    )

/*
 * When using rovingFocusContainer on lazy loading containers a lazyListState and the current focused Item
 * should additionally be passed as an argument to avoid the hole container being unfocusable,
 * when the focused item is scrolled out of view
 */
@Composable
fun Modifier.rovingFocusContainer(direction: RovingFocusDirection = RovingFocusDirection.Vertical): Modifier =
    rovingFocusContainer<Unit>(
        direction = direction,
        listState = null,
        focusedItem = null,
        focusedItemClass = null,
        ignoredKeys = emptyList(),
    )

fun Modifier.rovingFocusItem(isFocused: () -> Boolean, onFocus: () -> Unit): Modifier =
    this.focusProperties { onEnter = { if (!isFocused() && requestedFocusDirection.isTab()) cancelFocusChange() } }
        .focusGroup()
        .onFocusChanged { if (it.isFocused) onFocus() }

/**
 * When using rovingFocusContainer on a lazily loaded container, such as a LazyColumn, isFocused should be provided as a
 * Lambda. This ensures that updates to focusedItem inside rovingFocusContainer are visible to rovingFocusItem before
 * recomposition occurs, since recomposition only happens after the focusManager finishes changing focus.
 */
fun Modifier.rovingFocusItem(isFocused: Boolean, onFocus: () -> Unit): Modifier =
    this.focusProperties { onEnter = { if (!isFocused && requestedFocusDirection.isTab()) cancelFocusChange() } }
        .focusGroup()
        .onFocusChanged { if (it.isFocused) onFocus() }

private fun Modifier.moveFocusOnDirection(
    moveFocus: (FocusDirection) -> Boolean,
    directions: List<FocusDirection>,
): Modifier =
    this.onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val pressedDirection =
                when (keyEvent.key) {
                    Key.DirectionRight -> FocusDirection.Right
                    Key.DirectionLeft -> FocusDirection.Left
                    Key.DirectionDown -> FocusDirection.Down
                    Key.DirectionUp -> FocusDirection.Up
                    else -> return@onPreviewKeyEvent false
                }
            return@onPreviewKeyEvent directions.contains(pressedDirection) && moveFocus(pressedDirection)
        }
        .focusProperties { onExit = { if (directions.contains(requestedFocusDirection)) cancelFocusChange() } }
        .focusGroup()

private fun FocusDirection.isTab(): Boolean = this == FocusDirection.Next || this == FocusDirection.Previous
