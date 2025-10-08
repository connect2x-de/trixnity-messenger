package de.connect2x.trixnity.messenger.compose.view.richtext.html

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.richtext.BlockContent
import de.connect2x.trixnity.messenger.compose.view.richtext.RichText
import de.connect2x.trixnity.messenger.compose.view.richtext.RichTextContext

@Composable
internal fun ColumnScope.TableCaption(node: RichText, context: RichTextContext) {
    when (node) {
        is RichText.Block -> TableCaption(node, context)
        is RichText.InlineSpan -> Unit
    }
}

@Composable
internal fun ColumnScope.TableCaption(node: RichText.Block, context: RichTextContext) {
    when (node.tag) {
        "thead", "tbody" -> {
            for (child in node.children) {
                TableCaption(child, context)
            }
        }
        "caption" -> {
            BlockContent(node, context)
        }
    }
}
