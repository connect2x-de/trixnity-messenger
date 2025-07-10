package de.connect2x.messenger.compose.view.richtext

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import de.connect2x.trixnity.messenger.util.html.HtmlNode
import net.folivo.trixnity.core.model.Mention

data class RichTextColors(
    val linkColor: Color,
    val spoilerColor: Color,
) {
    companion object {
        @Composable
        fun default(
            linkColor: Color = MaterialTheme.colorScheme.secondary,
            spoilerColor: Color = MaterialTheme.colorScheme.onSurface,
        ) = RichTextColors(
            linkColor,
            spoilerColor,
        )
    }
}

@Composable
fun RichTextDisplay(
    document: HtmlNode.HtmlElement,
    modifier: Modifier = Modifier.Companion,
    colors: RichTextColors = RichTextColors.default(),
    onCopy: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    onMentionClick: (Mention) -> Unit = {},
) {
    val richText = rememberRichText(document)

    val textColor = LocalTextStyle.current.color

    val context = remember(colors.linkColor) {
        RichTextContext(
            onCopy = onCopy,
            onLinkClick = onLinkClick,
            onMentionClick = onMentionClick,
            textLinkStyles = TextLinkStyles(
                style = SpanStyle(color = colors.linkColor),
                hoveredStyle = SpanStyle(color = colors.linkColor, textDecoration = TextDecoration.Companion.Underline),
                focusedStyle = SpanStyle(color = colors.linkColor, textDecoration = TextDecoration.Companion.Underline),
                pressedStyle = SpanStyle(color = colors.linkColor, textDecoration = TextDecoration.Companion.Underline),
            ),
            spoilerStyles = TextLinkStyles(
                style = SpanStyle(color = colors.spoilerColor, background = colors.spoilerColor),
                hoveredStyle = SpanStyle(color = textColor, background = Color.Companion.Transparent),
                focusedStyle = SpanStyle(color = textColor, background = Color.Companion.Transparent),
                pressedStyle = SpanStyle(color = textColor, background = Color.Companion.Transparent),
            )
        )
    }

    SelectionContainer(modifier) {
        Column {
            BlockContent(richText, context)
        }
    }
}
