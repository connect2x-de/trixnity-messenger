package de.connect2x.messenger.compose.view.richtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

// This must match exactly
internal const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"

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
    onMeasure: CompoundTextContext.(String, Constraints) -> Placeholder?,
    onRender: @Composable (String, Constraints) -> Unit,
) {
    val replaceables = remember(content) {
        content.getStringAnnotations(INLINE_CONTENT_TAG, 0, content.length)
    }

    val density = LocalDensity.current
    val textStyle = LocalTextStyle.current.copy(
        lineHeight = TextUnit.Unspecified,
        color = LocalContentColor.current,
    )
    val layoutDirection = LocalLayoutDirection.current
    val measurer = rememberTextMeasurer()

    val context = remember(density, textStyle, layoutDirection, measurer) {
        CompoundTextContext(density, textStyle, layoutDirection, measurer)
    }

    BoxWithConstraints {
        val containerConstraints = constraints

        val inlineContent = remember(replaceables, context, onMeasure, containerConstraints) {
            buildMap {
                for (replaceable in replaceables) {
                    val measurement = context.onMeasure(replaceable.item, containerConstraints)
                    if (measurement != null) {
                        put(replaceable.item, InlineTextContent(measurement, {}))
                    }
                }
            }
        }
        val placeholders = remember(inlineContent, content) {
            content.resolveInlineContent(inlineContent)
        }
        val policy = remember(content, placeholders, context) {
            MentionMeasurePolicy(content, placeholders, context)
        }
        if (content.isNotBlank()) {
            Box(modifier) {
                BasicText(
                    text = content,
                    inlineContent = inlineContent,
                    style = textStyle,
                    onTextLayout = null,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    minLines = 1,
                )
                if (placeholders.isNotEmpty()) {
                    Layout(
                        measurePolicy = policy,
                        content = {
                            for (index in placeholders.indices) {
                                onRender(placeholders[index].first, containerConstraints)
                            }
                        },
                    )
                }
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
