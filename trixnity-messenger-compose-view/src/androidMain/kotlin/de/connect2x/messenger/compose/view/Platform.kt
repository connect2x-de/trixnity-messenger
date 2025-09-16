package de.connect2x.messenger.compose.view

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.core.content.ContextCompat.getSystemService
import de.connect2x.trixnity.messenger.util.ActivityGetter
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

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier =
    this // Empty modifier.

actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier =
    this // Empty modifier.

actual fun Modifier.pointerEventWrapper(
    eventType: PointerEventType,
    pass: PointerEventPass,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit,
): Modifier = this // Pointer events are not supported on Mobile.

actual suspend fun copyToClipboard(value: String, di: Koin) {
    val context = di.getOrNull<ActivityGetter>()?.invoke()?.applicationContext
    if (context != null) {
        val clipboard: ClipboardManager? = getSystemService(context, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("copy", value)
        clipboard?.setPrimaryClip(clip)
    }
}
