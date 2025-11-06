package de.connect2x.messenger.compose.view.richtext.html

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import de.connect2x.messenger.compose.view.richtext.BlockContent
import de.connect2x.messenger.compose.view.richtext.InlineContent
import de.connect2x.messenger.compose.view.richtext.RichText
import de.connect2x.messenger.compose.view.richtext.RichTextContext

@Composable
internal fun TableCell(node: RichText, context: RichTextContext) {
    when (node) {
        is RichText.Block -> TableCell(node, context)
        is RichText.InlineSpan -> Unit
    }
}

@Composable
internal fun TableCell(node: RichText.Block, context: RichTextContext) {
    when (node.tag) {
        "th" -> {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                )
            ) {
                Column {
                    for (index in node.children.indices) {
                        val child = node.children[index]
                        when (child) {
                            is RichText.Block -> BlockContent(child, context)
                            is RichText.InlineSpan -> InlineContent(child, context, first = index == 0)
                        }
                    }
                }
            }
        }

        "td" -> {
            Column {
                for (index in node.children.indices) {
                    val child = node.children[index]
                    when (child) {
                        is RichText.Block -> BlockContent(child, context)
                        is RichText.InlineSpan -> InlineContent(child, context, first = index == 0)
                    }
                }
            }
        }
    }
}
