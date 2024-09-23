package de.connect2x.messenger.compose.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat.getSystemService
import org.koin.core.Koin

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
) = Unit

@Composable
actual fun VerticalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
) = Unit

@Composable
actual fun HorizontalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
) = Unit

@Composable
actual fun HorizontalScrollbar(
    modifier: Modifier,
    lazyListState: LazyListState,
    reverseLayout: Boolean,
) = Unit

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    delayMillis: Int,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val isVisible = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    return Box(
        Modifier.combinedClickable(
            indication = ripple(),
            interactionSource = interactionSource,
            role = Role.Button,
            onClick = if (onClick != null) onClick else {
                {}
            },
            onLongClick = { isVisible.value = true }),
    ) {
        content()
        if (isVisible.value) {
            Popup(
                onDismissRequest = { isVisible.value = false }, // FIXME check if that works
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 500.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary
                ) {
                    tooltip()
                }
            }
        }
    }
}

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier = this // empty Modifier

actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier =
    this // empty Modifier

actual suspend fun copyToClipboard(value: String, di: Koin) {
    val clipboard: ClipboardManager? = getSystemService(di.get<Context>(), ClipboardManager::class.java)
    val clip = ClipData.newPlainText("copy", value)
    clipboard?.setPrimaryClip(clip)
}
