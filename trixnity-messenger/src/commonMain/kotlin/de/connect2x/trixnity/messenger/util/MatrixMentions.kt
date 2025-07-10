package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.html.HtmlNode
import de.connect2x.trixnity.messenger.util.html.MatrixMentionVisitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.Mention

object MatrixMentions {
    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun findInText(text: String): Map<IntRange, Mention> = MatrixRegex.findMentions(text)

    internal fun findInHtml(content: HtmlNode): Map<String, Mention>? =
        MatrixMentionVisitor().process(content)
}
