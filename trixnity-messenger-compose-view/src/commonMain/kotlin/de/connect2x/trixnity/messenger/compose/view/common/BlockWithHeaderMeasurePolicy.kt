package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints

object BlockWithHeaderMeasurePolicy : MeasurePolicy {
    object HeaderLayoutId

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val header = measurables.firstOrNull { it.layoutId == HeaderLayoutId }
        val content = measurables.first { it.layoutId != HeaderLayoutId }
        val contentResult = content.measure(constraints)
        val headerResult =
            header?.measure(constraints.copy(minWidth = contentResult.width, maxWidth = contentResult.width))
        return if (headerResult == null) {
            layout(contentResult.width, contentResult.height) { contentResult.place(0, 0) }
        } else {
            layout(contentResult.width, headerResult.height + contentResult.height) {
                headerResult.place(0, 0)
                contentResult.place(0, headerResult.height)
            }
        }
    }
}
