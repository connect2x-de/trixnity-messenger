package de.connect2x.messenger.compose.view

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.core.content.ContextCompat.getSystemService
import de.connect2x.trixnity.messenger.util.ActivityGetter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.Koin

private val log = KotlinLogging.logger { }

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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun Tooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    delayMillis: Int,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                modifier = onClick?.let { modifier.clickable(onClick = it) } ?: modifier,
                caretSize = TooltipDefaults.caretSize,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                content = { tooltip() }
            )
        },
        state = rememberTooltipState(),
    ) {
        content()
    }
}

actual fun Modifier.buttonPointerModifier(enabled: Boolean): Modifier = this // empty Modifier

actual fun Modifier.pointerMoveFilter(onEnter: () -> Boolean, onExit: () -> Boolean): Modifier =
    this // empty Modifier

actual fun Modifier.pointerEventWrapper(
    eventType: PointerEventType,
    pass: PointerEventPass,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
) = this // Pointer events are not supported on Mobile.

actual suspend fun copyToClipboard(value: String, di: Koin) {
    val context = di.getOrNull<ActivityGetter>()?.invoke()?.applicationContext
    if (context != null) {
        val clipboard: ClipboardManager? = getSystemService(context, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("copy", value)
        clipboard?.setPrimaryClip(clip)
    }
}
