package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        SelectionContainer(modifier) {
            content()
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
