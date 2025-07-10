package de.connect2x.trixnity.messenger.util.html

import de.connect2x.messenger.compose.view.richtext.html.Patterns
import net.folivo.trixnity.core.MatrixRegex

class LinkMatcher(private val content: String) {
    private var url: Match? = null
    private var userId: Match? = null
    private var roomAlias: Match? = null

    data class Match(
        val type: MatchType,
        val result: MatchResult,
    )

    enum class MatchType {
        URL,
        USER_ID,
        ROOM_ALIAS,
    }

    fun next(index: Int): Match? {
        if (url == null || url!!.result.range.start < index) {
            url = Patterns.AUTOLINK_WEB_URL.matchAt(content, index)
                ?.let { Match(MatchType.URL, it) }
        }
        if (userId == null || userId!!.result.range.start < index) {
            userId = MatrixRegex.userId.matchAt(content, index)
                ?.let { Match(MatchType.USER_ID, it) }
        }
        if (roomAlias == null || roomAlias!!.result.range.start < index) {
            roomAlias = MatrixRegex.roomAlias.matchAt(content, index)
                ?.let { Match(MatchType.ROOM_ALIAS, it) }
        }
        return listOfNotNull(url, userId, roomAlias)
            .minByOrNull { it.result.range.start }
    }
}
