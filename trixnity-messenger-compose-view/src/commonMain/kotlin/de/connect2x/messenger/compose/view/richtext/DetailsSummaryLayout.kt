package de.connect2x.messenger.compose.view.richtext

import androidx.compose.runtime.Composable
import androidx.compose.ui.UiComposable
import androidx.compose.ui.layout.Layout

@Composable
fun DetailsSummaryLayout(content: @Composable @UiComposable () -> Unit) {
    Layout(
        content = content,
        measurePolicy = DetailsSummaryMeasurePolicy,
    )
}
