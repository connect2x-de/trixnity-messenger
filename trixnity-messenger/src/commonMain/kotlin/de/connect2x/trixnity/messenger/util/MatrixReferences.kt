package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.util.html.MatrixMentionVisitor
import net.folivo.trixnity.core.util.Reference

internal object MatrixReferences {
    fun findInText(text: String): Map<IntRange, Reference> = Reference.findReferences(text)

    fun findInHtml(content: HtmlNode): Map<String, Reference> =
        MatrixMentionVisitor().process(content)
}
