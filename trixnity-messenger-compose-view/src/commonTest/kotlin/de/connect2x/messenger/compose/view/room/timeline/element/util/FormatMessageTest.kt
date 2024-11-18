package de.connect2x.messenger.compose.view.room.timeline.element.util

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
            listOf(Pair(6..19, TimelineElementMention.User(UserInfoElement("user", UserId("user", "acme.org")))))

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
            listOf(Pair(6..39, TimelineElementMention.User(UserInfoElement("user", UserId("user", "acme.org")))))

        assertEquals(
            "Hallo <a href=\"timmy-data:0\">user</a>! How are you?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    @Test
    fun shouldReplaceLinkUserMention() {
        val message = "Hallo https://matrix.to/#/%40alice%3Aexample.org! How are you?"
        val mention =
            listOf(Pair(6..47, TimelineElementMention.User(UserInfoElement("user", UserId("user", "acme.org")))))

        assertEquals(
            "Hallo <a href=\"timmy-data:0\">user</a>! How are you?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    @Test
    fun shouldReplaceMultipleMentions() {
        val message = "Hallo @user:acme.com! How are you? Want to meet up with @user2:acme.com?"
        val mention = listOf(
            56..70 to TimelineElementMention.User(UserInfoElement("user2", UserId("user2", "acme.org"))),
            6..19 to TimelineElementMention.User(UserInfoElement("user", UserId("user", "acme.org")))
        )

        assertEquals(
            "Hallo <a href=\"timmy-data:1\">user</a>! How are you? Want to meet up with <a href=\"timmy-data:0\">user2</a>?",
            message.formatMentions(mention) { "Message in #$it" }
        )
    }

    // Links
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
}
