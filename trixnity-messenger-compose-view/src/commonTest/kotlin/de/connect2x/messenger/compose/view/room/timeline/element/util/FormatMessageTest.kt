package de.connect2x.messenger.compose.view.room.timeline.element.util

import de.connect2x.messenger.compose.view.room.timeline.element.message.formatLinks
import de.connect2x.messenger.compose.view.room.timeline.element.message.formatMentions
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatMessageTest {
    // Mentions
    @Test
    fun shouldReplaceBasicUserMention() {
        val message = "Hallo @user:acme.com! How are you?"
        val mention =
            listOf(Pair(6..19, TimelineElementMention.User(UserInfoElement(UserId("user", "acme.org"), "user", "U"))))

        assertEquals(
            "Hallo <a href=\"timmy-data:0\">user</a>! How are you?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    @Test
    fun shouldReplaceBasicRoomIdMention() {
        val message = "Hallo !bike-production:acme.com! How are you?"
        val mention = listOf(
            Pair(
                6..30,
                TimelineElementMention.Room(
                    RoomInfoElement(
                        "bike-production",
                        RoomId("bike-production", "acme.org"),
                        "bp",
                        ByteArray(10)
                    )
                )
            )
        )

        assertEquals(
            "Hallo <a href=\"timmy-data:0\">bike-production</a>! How are you?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    @Test
    fun shouldReplaceBasicRoomAliasMention() {
        val message = "Hallo #awesome-room:acme.com! How are you?"
        val mention = listOf(
            Pair(
                6..27,
                TimelineElementMention.Room(
                    RoomInfoElement(
                        "awesome-room",
                        RoomId("awesome-room", "acme.org"),
                        "ar",
                        ByteArray(10)
                    )
                )
            )
        )

        assertEquals(
            "Hallo <a href=\"timmy-data:0\">awesome-room</a>! How are you?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    @Test
    fun shouldReplaceUriUserMention() {
        val message = "Hallo matrix:u/user:acme.com?action=chat! How are you?"
        val mention =
            listOf(Pair(6..39, TimelineElementMention.User(UserInfoElement(UserId("user", "acme.org"), "user", "U"))))

        assertEquals(
            "Hallo <a href=\"timmy-data:0\">user</a>! How are you?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    @Test
    fun shouldReplaceLinkUserMention() {
        val message = "Hallo https://matrix.to/#/%40alice%3Aexample.org! How are you?"
        val mention =
            listOf(Pair(6..47, TimelineElementMention.User(UserInfoElement(UserId("user", "acme.org"), "user", "U"))))

        assertEquals(
            "Hallo <a href=\"timmy-data:0\">user</a>! How are you?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    @Test
    fun shouldReplaceMultipleMentions() {
        val message = "Hallo @user:acme.com! How are you? Want to meet up with @user2:acme.com?"
        val mention = listOf(
            56..70 to TimelineElementMention.User(UserInfoElement(UserId("user2", "acme.org"), "user2", "U")),
            6..19 to TimelineElementMention.User(UserInfoElement(UserId("user", "acme.org"), "user", "U"))
        )

        assertEquals(
            "Hallo <a href=\"timmy-data:1\">user</a>! How are you? Want to meet up with <a href=\"timmy-data:0\">user2</a>?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    // Links
    @Test
    fun shouldFormatLinkExcludingWhitespaceInTheEnd() {
        listOf("\r", "\n", "\t", " `").forEach {
            assertEquals(
                "Hello, take a look at <a href=\"https://tammy.connect2x.de/en-us/\">https://tammy.connect2x.de/en-us/</a>${it}cool isn't it?",
                "Hello, take a look at https://tammy.connect2x.de/en-us/${it}cool isn't it?".formatLinks()
            )
        }
    }

    @Test
    fun shouldFormatRegularLink() {
        assertEquals(
            "<a href=\"https://matrix.org\">https://matrix.org</a>",
            "https://matrix.org".formatLinks()
        )
    }

    @Test
    fun shouldSkipAnchor() {
        assertEquals(
            "<a href=\"https://matrix.org\">Matrix Website</a>",
            "<a href=\"https://matrix.org\">Matrix Website</a>".formatLinks()
        )
    }

    @Test
    fun shouldFormatUrlWithEscapedAmpersand() {
        val link = "https://duckduckgo.com/?q=html+escaping+ampersand&amp;ia=web"
        val formattedLink = link.formatLinks()

        assertEquals(
            "<a href=\"${link}\">${link}</a>",
            formattedLink,
        )
    }

    @Test
    fun shouldFormatUrlWithSemicolon() {
        val link = "https://exampleformytest.com/bla;blubb"
        val formattedLink = link.formatLinks()

        assertEquals(
            "<a href=\"${link}\">${link}</a>",
            formattedLink,
        )
    }

    @Test
    fun shouldFormatUrlWithComma() {
        val link = "https://osmand.net/map/?pin=50.89774,13.69089#19/51.05483/13.74711"
        val formattedLink = link.formatLinks()

        assertEquals(
            "<a href=\"${link}\">${link}</a>",
            formattedLink,
        )
    }

    @Test
    fun shouldIgnoreExclamationMarkAtEndOfLink() {
        assertEquals(
            "I think you could really like https://graphemica.com/!".formatLinks(),
            "I think you could really like <a href=\"https://graphemica.com/\">https://graphemica.com/</a>!"
        )
    }

    @Test
    fun shouldIgnoreParenthesis() {
        assertEquals(
            "The website Graphemica (https://graphemica.com/) can display any sort of symbol.".formatLinks(),
            "The website Graphemica (<a href=\"https://graphemica.com/\">https://graphemica.com/</a>) can display any sort of symbol."
        )
    }

    @Test
    fun shouldIgnoreColonAtTheEndOfLink() {
        assertEquals(
            "The best thing about https://graphemica.com/: It has support for unicode characters.".formatLinks(),
            "The best thing about <a href=\"https://graphemica.com/\">https://graphemica.com/</a>: It has support for unicode characters."
        )
    }

    @Test
    fun shouldIgnoreQuestionMarkAtTheEndOfLink() {
        assertEquals(
            "Do you know https://graphemica.com/?".formatLinks(),
            "Do you know <a href=\"https://graphemica.com/\">https://graphemica.com/</a>?"
        )
    }

    @Test
    fun shouldIgnorePeriodAtTheEndOfLink() {
        assertEquals(
            "I thought about https://graphemica.com.".formatLinks(),
            "I thought about <a href=\"https://graphemica.com\">https://graphemica.com</a>."
        )
    }

    @Test
    fun shouldAllowUnicodeSymbols() {
        assertEquals(
            "https://graphemica.com/»".formatLinks(),
            "<a href=\"https://graphemica.com/»\">https://graphemica.com/»</a>"
        )
    }

    @Test
    fun shouldAllowSymbolsEvenWithPunctuationInsideUrl() {
        assertEquals(
            "https://duckduckgo.com/?q=!#$'()*+,-./:;=?@[]^_`{|}~".formatLinks(),
            "<a href=\"https://duckduckgo.com/?q=!#$'()*+,-./:;=?@[]^_`{|}~\">https://duckduckgo.com/?q=!#$'()*+,-./:;=?@[]^_`{|}~</a>",
        )
    }
}
