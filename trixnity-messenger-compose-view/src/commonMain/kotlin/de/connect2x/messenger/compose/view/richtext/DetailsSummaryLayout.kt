package de.connect2x.messenger.compose.view.richtext

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.util.animateRotation

@Composable
fun DetailsSummaryLayout(
    summary: @Composable @UiComposable ColumnScope.() -> Unit,
    details: @Composable @UiComposable ColumnScope.() -> Unit
) {
    val expanded = remember { MutableTransitionState(false) }
    val transition = rememberTransition(expanded)
    val rotation = transition.animateFloat { if (it) 180f else 0f }

    Layout(
        measurePolicy = DetailsSummaryMeasurePolicy,
        content = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .layoutId(DetailsSummaryMeasurePolicy.LayoutId.SUMMARY)
                    .clickable { expanded.targetState = !expanded.targetState }
                    .buttonPointerModifier(),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    Modifier.height(48.dp).padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DisableSelection {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.titleSmall,
                        ) {
                            Column {
                                summary()
                            }
                        }
                    }
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).animateRotation(rotation),
                        )
                    }
                }
            }
            transition.AnimatedContent(
                Modifier.layoutId(DetailsSummaryMeasurePolicy.LayoutId.DETAILS),
                transitionSpec = {
                    fadeIn().plus(expandVertically())
                        .togetherWith(fadeOut().plus(shrinkVertically()))
                }
            ) { expanded ->
                Column(
                    Modifier.animatedDetailsModifier(expanded).padding(8.dp),
                    content = details
                )
            }
        }
    )
}

@Stable
private fun Modifier.animatedDetailsModifier(expanded: Boolean) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val width = placeable.width
    val height = if (expanded) placeable.height else 0
    layout(width, height) {
        if (expanded) placeable.place(0, 0)
    }
}
