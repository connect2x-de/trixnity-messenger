package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SpaceChildOrderTest {
    @Test
    fun `valid ASCII orders precede unordered children with timestamp and room ID tiebreakers`() {
        val user = UserId("alice", "example.org")
        val definitions =
            listOf(
                Triple("a", "same", 3L),
                Triple("b", "same", 2L),
                Triple("c", "same", 2L),
                Triple("d", " ", 99L),
                Triple("e", "~", 1L),
                Triple("f", null, 5L),
                Triple("g", "a".repeat(51), 4L),
                Triple("h", "é", 3L),
                Triple("i", "\n", 2L),
                Triple("j", "", 100L),
                Triple("k", "a".repeat(50), 1L),
            )
        val events = definitions.associate { (id, order, timestamp) ->
            val roomId = RoomId("!$id:example.org")
            roomId.full to
                ClientEvent.StrippedStateEvent(
                    ChildEventContent(via = setOf("example.org"), order = order),
                    sender = user,
                    stateKey = roomId.full,
                    originTimestamp = timestamp,
                )
        }
        val items = events.keys.map { SpaceBrowserItem(user, RoomId(it), it, false, true) }
        items.sortedWith(spaceChildComparator(events)).map { it.roomId.full } shouldBe
            listOf("j", "d", "k", "b", "c", "a", "e", "i", "h", "g", "f").map { "!$it:example.org" }
    }
}
