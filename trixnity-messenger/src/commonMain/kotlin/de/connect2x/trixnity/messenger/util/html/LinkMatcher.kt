package de.connect2x.trixnity.messenger.util.html

import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.Mention


class LinkMatcher(private val content: String) {
    sealed interface LinkMatch {
        val range: IntRange
        val content: String

        data class UrlMatch(override val range: IntRange, override val content: String): LinkMatch
        data class IdMentionMatch(override val range: IntRange, override val content: String, val mention: Mention): LinkMatch
        data class LinkMentionMatch(override val range: IntRange, override val content: String, val mention: Mention): LinkMatch
    }

    fun findAll(): List<LinkMatch> {
        val idMentions = MatrixRegex.findIdMentions(content)
            .map { (range, mention) -> LinkMatch.IdMentionMatch(range, content.substring(range), mention) }
        val linkMentions = MatrixRegex.findLinkMentions(content)
            .map { (range, mention) -> LinkMatch.LinkMentionMatch(range, content.substring(range), mention) }
        val mentions = idMentions.plus(linkMentions).sortedBy { it.range.start }
        return buildList {
            if (mentions.isEmpty()) {
                findUrls(0, content.length)
            } else if (mentions.size == 1) {
                val mention = mentions.single()
                findUrls(0, mention.range.first)
                add(mention)
                findUrls(mention.range.last + 1, content.length)
            } else {
                findUrls(0, mentions.first().range.first)
                for ((a, b) in mentions.windowed(2)) {
                    add(a)
                    findUrls(a.range.last + 1, a.range.first)
                    add(b)
                }
                findUrls(mentions.last().range.last + 1, content.length)
            }
        }
    }

    fun MutableList<LinkMatch>.findUrls(from: Int, to: Int) {
        for ((range, match) in MatrixRegex.findLinks(content, from, to)) {
            add(LinkMatch.UrlMatch(range, match))
        }
    }
}
