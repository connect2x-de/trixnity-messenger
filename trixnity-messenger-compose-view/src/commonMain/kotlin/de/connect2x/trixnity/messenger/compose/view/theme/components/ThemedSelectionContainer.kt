package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

data class SelectionStyle(
    val handleColor: Color,
    val selectionColor: Color,
) {
    internal val colors = TextSelectionColors(handleColor, selectionColor)

    companion object {
        @Composable
        fun onSurface(
            handleColor: Color = MaterialTheme.colorScheme.primary,
            selectionColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        ) = SelectionStyle(handleColor, selectionColor)

        @Composable
        fun onPrimary(
            handleColor: Color = MaterialTheme.colorScheme.inversePrimary,
            selectionColor: Color = MaterialTheme.colorScheme.inversePrimary.copy(0.4f),
        ) = SelectionStyle(handleColor, selectionColor)
    }
}

@Composable
fun ThemedSelectionContainer(style: SelectionStyle, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextSelectionColors provides style.colors) {
        // This is a hack around the internal focus behaviour of SelectionContainer.
        // We cannot make it unfocusable since that would disable proper text selection, and we cannot simpy set focus
        // modifiers on the SelectionContainer because it internally overwrites some of them.
        // Additionally, if content contains no focusable elements (like a simple text message) the SelectionContainer
        // will grab focus itself unless skipped over.
        // We skip into it by using a FocusRequester to focus an inner element and skip out of it by moving focus twice.
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        val base = modifier
            .focusProperties {
                onEnter = {
                    if (requestedFocusDirection == FocusDirection.Next || requestedFocusDirection == FocusDirection.Previous) {
                        // Try to focus the box around content if it fails then there are no focusable elements inside so we
                        // don't need to focus the anything at all and can just skip over the SelectionContainer
                        if (!focusRequester.requestFocus()) cancelFocusChange()
                    }
                }
            }
            .focusGroup()

        val suffix = Modifier
            .focusProperties {
                onExit = {
                    // move twice to skip the SelectionContainer
                    if (requestedFocusDirection == FocusDirection.Previous) {
                        focusManager.moveFocus(requestedFocusDirection)
                        focusManager.moveFocus(requestedFocusDirection)
                    }
                }
            }

        SelectionContainer(modifier = SuffixModifier(base, suffix)) {
            Box(Modifier.focusRequester(focusRequester).focusGroup()) {
                content()
            }
        }
    }
}

@Composable
fun ThemedSelectableText(
    text: String,
    selectionStyle: SelectionStyle,
    selectionModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    ThemedSelectionContainer(selectionStyle, selectionModifier.semantics(mergeDescendants = true) {}) {
        Text(
            text,
            modifier,
            color,
            autoSize,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            letterSpacing,
            textDecoration,
            textAlign,
            lineHeight,
            overflow,
            softWrap,
            maxLines,
            minLines,
            onTextLayout,
            style,
        )
    }
}

/**
 * SuffixModifier makes sure that the given suffix is applied last.
 * This is useful to guarantee that certain modifiers are overwritten.
 */
private class SuffixModifier(
    val base: Modifier,
    val suffix: Modifier,
    private val full: Modifier = base.then(suffix), // this is the normal then
) : Modifier by full {
    override fun then(other: Modifier): Modifier = SuffixModifier(base.then(other), suffix)
}
