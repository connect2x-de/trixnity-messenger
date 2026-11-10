package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.StateBaseEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.StrippedStateEvent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

class SpacePerformanceBenchmarkTest {
    @Test
    fun parentValidationScaling() {
        val user = UserId("alice", "example.org")
        val parent = RoomId("!parent:example.org")
        for (size in listOf(100, 1_000, 5_000)) {
            val candidates =
                (0 until size).map { index ->
                    SpaceParentCandidate(
                        RoomId("!child$index:example.org"),
                        StrippedStateEvent(
                            ParentEventContent(via = setOf("example.org")),
                            sender = user,
                            stateKey = parent.full,
                        ),
                    )
                }
            val state: List<StateBaseEvent<*>> =
                listOf(
                    StrippedStateEvent(
                        CreateEventContent(type = CreateEventContent.RoomType.Space),
                        sender = user,
                        stateKey = "",
                    )
                ) +
                    candidates.map { (child, _) ->
                        StrippedStateEvent(
                            ChildEventContent(via = setOf("example.org")),
                            sender = user,
                            stateKey = child.full,
                        )
                    }
            report("parent-index", size) { assertEquals(size, filterValidSpaceParents(candidates, state).size) }
            report("parent-list-scan", size) {
                assertEquals(size, candidates.count { (child, event) -> isValidSpaceParent(child, event, state) })
            }
        }
    }

    @Test
    fun hierarchyAssemblyScaling() {
        val user = UserId("alice", "example.org")
        val space = RoomId("!space:example.org")
        for (size in listOf(100, 1_000, 10_000)) {
            val hierarchy = (0 until size).map { hierarchyRoom(RoomId("!room$it:example.org")) }
            report("hierarchy", size) {
                assertEquals(
                    size,
                    buildSpaceHierarchyItems(user, space, emptyMap(), emptyMap(), hierarchy, emptyList()).size,
                )
            }
        }
    }

    private fun report(operation: String, size: Int, action: () -> Unit) {
        repeat(3) { action() }
        val samples = List(7) { measureTime(action).inWholeNanoseconds }.sorted()
        println("SPACE_BENCHMARK operation=$operation size=$size medianNs=${samples[samples.size / 2]}")
    }
}
