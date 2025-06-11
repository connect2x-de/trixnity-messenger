package org.example.project.richtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"

@Immutable
internal data class CompoundTextContext(
    val density: Density,
    val textStyle: TextStyle,
    val layoutDirection: LayoutDirection,
    val measurer: TextMeasurer,
) {
    @Stable
    fun measure(
        text: String,
        style: TextStyle = textStyle,
    ) = measurer.measure(
        text = text,
        style = style,
        layoutDirection = layoutDirection,
        density = density,
    )

    @Stable
    fun measure(
        text: AnnotatedString,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        constraints: Constraints,
    ) = measurer.measure(
        text = text,
        style = textStyle,
        placeholders = placeholders,
        constraints = constraints,
        layoutDirection = layoutDirection,
        density = density,
    )
}

@Composable
internal fun CompoundText(
    modifier: Modifier = Modifier,
    content: AnnotatedString,
    onMeasure: CompoundTextContext.(String) -> Placeholder,
    onRender: @Composable (String) -> Unit,
) {
    val replaceables = remember(content) {
        content.getStringAnnotations(INLINE_CONTENT_TAG, 0, content.length)
    }
    val density = LocalDensity.current
    val textStyle = LocalTextStyle.current
    val layoutDirection = LocalLayoutDirection.current
    val measurer = rememberTextMeasurer()
    val context = remember(density, textStyle, layoutDirection, measurer) {
        CompoundTextContext(density, textStyle, layoutDirection, measurer)
    }
    val inlineContent = remember(replaceables, context) {
        replaceables.associate {
            it.item to InlineTextContent(context.onMeasure(it.item), {})
        }
    }
    val placeholders = remember(inlineContent, content) {
        content.resolveInlineContent(inlineContent)
    }
    if (content.isNotBlank()) {
        Box(modifier) {
            Text(
                text = content,
                inlineContent = inlineContent,
            )
            if (placeholders.isNotEmpty()) {
                Layout(
                    measurePolicy = MentionMeasurePolicy(content, placeholders, context),
                    content = {
                        for (index in placeholders.indices) {
                            onRender(placeholders[index].first)
                        }
                    },
                )
            }
        }
    }
}

internal class MentionMeasurePolicy(
    private val text: AnnotatedString,
    private val placeholders: List<Pair<String, AnnotatedString.Range<Placeholder>>>,
    private val context: CompoundTextContext,
): MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val textLayout = context.measure(text, placeholders = placeholders.map { it.second }, constraints)
        return layout(textLayout.size.width, textLayout.size.height) {
            for (index in measurables.indices) {
                val placeholder = textLayout.placeholderRects[index]
                val measurable = measurables[index]
                if (placeholder != null) {
                    measurable.measure(Constraints(
                        minWidth = placeholder.width.roundToInt(),
                        maxWidth = placeholder.width.roundToInt(),
                        minHeight = placeholder.height.roundToInt(),
                        maxHeight = placeholder.height.roundToInt(),
                    )).place(placeholder.left.roundToInt(), placeholder.top.roundToInt())
                }
            }
        }
    }
}

internal fun AnnotatedString.resolveInlineContent(
    inlineContent: Map<String, InlineTextContent>,
): List<Pair<String, AnnotatedString.Range<Placeholder>>> =
    getStringAnnotations(INLINE_CONTENT_TAG, 0, text.length)
        .mapNotNull { annotation ->
            inlineContent[annotation.item]?.let { inlineTextContent ->
                Pair(annotation.item, AnnotatedString.Range(
                    inlineTextContent.placeholder,
                    annotation.start,
                    annotation.end
                ))
            }
        }
