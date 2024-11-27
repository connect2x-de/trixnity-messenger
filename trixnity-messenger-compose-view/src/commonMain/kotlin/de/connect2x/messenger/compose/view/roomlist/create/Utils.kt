package de.connect2x.messenger.compose.view.roomlist.create

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

@Composable
fun LimitedSizeStickyHeaderColumn(
    modifier: Modifier = Modifier,
    percentage: Float = 0.5f,
    header: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.(shouldScroll: Boolean) -> Unit,
) {
    BoxWithConstraints {
        val density = LocalDensity.current
        val availableHeight = maxHeight
        var headerHeight by remember { mutableStateOf((0.dp)) }
        val limitedSpace = headerHeight > percentage * availableHeight
        val scrollState = rememberScrollState()
        val maybeScrollModifier = remember(limitedSpace) {
            if (limitedSpace) {
                modifier.verticalScroll(scrollState)
            } else {
                modifier
            }
        }

        Column(maybeScrollModifier) {
            Column(Modifier.onGloballyPositioned { coordinates ->
                headerHeight = with(density) { coordinates.size.height.toDp() }
            }) {
                header()
            }
            body(!limitedSpace)
        }
    }
}
