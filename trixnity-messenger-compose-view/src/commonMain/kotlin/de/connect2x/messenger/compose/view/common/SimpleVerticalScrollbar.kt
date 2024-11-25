import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// https://stackoverflow.com/questions/66341823/jetpack-compose-scrollbars
// https://stackoverflow.com/a/68056586
@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    color: Color,
    width: Dp = 8.dp,
    scrollBarState: MutableState<SimpleVerticalScrollbarState?>? = null,
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
    )

    return drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

        // Draw scrollbar if scrolling or if the animation is still running and lazy column has content
        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight
            val offset = Offset(this.size.width - width.toPx(), scrollbarOffsetY)
            val size = Size(width.toPx(), scrollbarHeight)
            if (scrollBarState != null) scrollBarState.value =
                SimpleVerticalScrollbarState(offset, size, alpha)
            drawRect(
                color = color,
                topLeft = offset,
                size = size,
                alpha = alpha,
            )
        }
    }
}

data class SimpleVerticalScrollbarState(
    val topLeft: Offset,
    val size: Size,
    val alpha: Float,
)
