package de.connect2x.messenger.compose.view.richtext

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints

internal object DetailsSummaryMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val summary = measurables.first { it.layoutId == LayoutId.SUMMARY }
        val details = measurables.firstOrNull { it.layoutId == LayoutId.DETAILS }
        val detailsPlaceable = details?.measure(constraints)

        return if (detailsPlaceable == null) {
            val summaryPlaceable = summary.measure(constraints)
            layout(summaryPlaceable.width, summaryPlaceable.height) {
                summaryPlaceable.placeRelative(0, 0)
            }
        } else {
            val summaryPlaceable = summary.measure(constraints.copy(minWidth = detailsPlaceable.width))
            val width = maxOf(summaryPlaceable.width, detailsPlaceable.width)
            val height = summaryPlaceable.height + detailsPlaceable.height

            layout(width, height) {
                summaryPlaceable.placeRelative(0, 0)
                detailsPlaceable.placeRelative(0, summaryPlaceable.height)
            }
        }
    }

    internal enum class LayoutId {
        SUMMARY,
        DETAILS
    }
}
