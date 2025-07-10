package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.html.MatrixMentionVisitor
import io.ktor.http.*
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class MatrixLinkTest {
    @Test
    fun ignoresRegularLinks() {
        assertEquals(
            expected = null,
            actual = MatrixMentionVisitor.parseLink("https://example.com"),
        )
    }

    @Test
    fun parsesMatrixToUserLinks() {
        assertEquals(
            expected = Mention.User(
                UserId("alice", "example.org"),
                match = "https://matrix.to/#/@alice:example.org",
                parameters = Parameters.Empty,
            ),
            actual = MatrixMentionVisitor.parseLink("https://matrix.to/#/@alice:example.org"),
        )
    }

    @Test
    fun parsesMatrixProtocolRoomLinks() {
        assertEquals(
            expected = Mention.RoomAlias(
                RoomAliasId("matrix-dev", "matrix.org"),
                match = "matrix:r/matrix-dev:matrix.org?action=join",
                parameters = parameters {
                    append("action", "join")
                }
            ),
            actual = MatrixMentionVisitor.parseLink("matrix:r/matrix-dev:matrix.org?action=join"),
        )
    }

    @Test
    fun parsesMatrixToRoomLinks() {
        assertEquals(
            expected = Mention.RoomAlias(
                RoomAliasId("roomalias", "somewhere.tld"),
                match = "https://matrix.to/#/#roomalias:somewhere.tld",
                parameters = Parameters.Empty
            ),
            actual = MatrixMentionVisitor.parseLink("https://matrix.to/#/#roomalias:somewhere.tld"),
        )
    }

    @Test
    fun parsesMatrixToRoomLinksWithRoomId() {
        assertEquals(
            expected = Mention.Room(
                RoomId("roomid", "somewhere.tld"),
                match = "https://matrix.to/#/!roomid:somewhere.tld?via=elsewhere.tld&via=another.tld",
                parameters = parameters {
                    append("via", "elsewhere.tld")
                    append("via", "another.tld")
                }
            ),
            actual = MatrixMentionVisitor.parseLink("https://matrix.to/#/!roomid:somewhere.tld?via=elsewhere.tld&via=another.tld"),
        )
    }

    @Test
    fun parsesMatrixProtocolUserLinks() {
        assertEquals(
            expected = Mention.User(
                UserId("username", "somewhere.tld"),
                match = "matrix:u/username:somewhere.tld?action=chat",
                parameters = parameters {
                    append("action", "chat")
                }
            ),
            actual = MatrixMentionVisitor.parseLink("matrix:u/username:somewhere.tld?action=chat"),
        )
    }

    @Test
    fun parsesMatrixProtocolRoomLinksWithRoomId() {
        assertEquals(
            expected = Mention.Room(
                RoomId("roomid", "somewhere.tld"),
                match = "matrix:roomid/roomid:somewhere.tld?via=elsewhere.tld&via=another.tld",
                parameters = parameters {
                    append("via", "elsewhere.tld")
                    append("via", "another.tld")
                }
            ),
            actual = MatrixMentionVisitor.parseLink("matrix:roomid/roomid:somewhere.tld?via=elsewhere.tld&via=another.tld"),
        )
    }

    @Test
    fun parsesMatrixProtocolRoomLinksWithEvent() {
        assertEquals(
            expected = Mention.Event(
                RoomId("somewhere", "example.org"),
                EventId("\$event"),
                match = "matrix:roomid/somewhere:example.org/e/event?via=elsewhere.ca",
                parameters = parameters {
                    append("via", "elsewhere.ca")
                }
            ),
            actual = MatrixMentionVisitor.parseLink("matrix:roomid/somewhere:example.org/e/event?via=elsewhere.ca"),
        )
    }

    @Test
    fun handlesInvalidOrMalformedLinks() {
        assertEquals(
            expected = null,
            actual = MatrixMentionVisitor.parseLink("matrix:revolution"),
        )
        assertEquals(
            expected = null,
            actual = MatrixMentionVisitor.parseLink("matrix:roomid/roomid/somewhere.tld/"),
        )
    }

    @Test
    fun handlesMatrixToLinksWithUnrecognizedFragments() {
        assertEquals(
            expected = null,
            actual = MatrixMentionVisitor.parseLink("https://matrix.to/#/@unknown"),
        )
    }
}
