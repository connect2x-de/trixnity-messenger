package de.connect2x.messenger.compose.view.common

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints

object HorizontalScrollableMeasurePolicy: MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val content = measurables[0]
        val scrollbar = measurables[1]
        val contentResult = content.measure(constraints)
        if (constraints.maxWidth > contentResult.width) {
            return layout(contentResult.width, contentResult.height) {
                contentResult.place(0, 0)
            }
        } else {
            val scrollbarConstraints = constraints.copy(
                minWidth = contentResult.width,
                maxWidth = contentResult.width,
            )
            val scrollbarResult = scrollbar.measure(scrollbarConstraints)
            return layout(contentResult.width, contentResult.height) {
                contentResult.place(0, 0)
                scrollbarResult.place(0, contentResult.height - scrollbarResult.height)
            }
        }
    }
}
