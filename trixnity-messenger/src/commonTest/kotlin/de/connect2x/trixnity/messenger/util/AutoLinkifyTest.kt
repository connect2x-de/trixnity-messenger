package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.util.html.MatrixMentionVisitor
import io.ktor.http.*
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoLinkifyTest {
    @Test
    fun ignoresRegularText() {
        assertEquals(
            expected = null,
            actual = MatrixMentionVisitor.parseLink("https://example.com"),
        )
    }

    @Test
    fun linkifiesRegularLinks() {
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
    fun linkifiesMatrixUserIds() {
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
    fun linkifiesMatrixUserAliases() {
        assertEquals(
            expected = Mention.RoomAlias(
                RoomAliasId("roomalias", "somewhere.tld"),
                match = "https://matrix.to/#/#roomalias:somewhere.tld",
                parameters = Parameters.Empty
            ),
            actual = MatrixMentionVisitor.parseLink("https://matrix.to/#/#roomalias:somewhere.tld"),
        )
    }
}
