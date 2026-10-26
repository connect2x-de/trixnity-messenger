package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.clientserverapi.model.room.GetHierarchy
import de.connect2x.trixnity.core.model.RoomId
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class SpaceHierarchyTest {
    private val room = hierarchyRoom(RoomId("!room:example.org"))

    @Test
    fun `pagination deduplicates rooms and finishes loading`() = runTest {
        val tokens = mutableListOf<String?>()
        val states =
            loadSpaceHierarchy { from ->
                    tokens += from
                    Result.success(
                        GetHierarchy.Response(nextBatch = if (from == null) "next" else null, rooms = listOf(room))
                    )
                }
                .toList()
        tokens shouldBe listOf(null, "next")
        states.first().loading shouldBe true
        states.last() shouldBe SpaceHierarchy(listOf(room), loading = false)
    }

    @Test
    fun `later page failure retains earlier results`() = runTest {
        val states =
            loadSpaceHierarchy { from ->
                    if (from == null) Result.success(GetHierarchy.Response(nextBatch = "next", rooms = listOf(room)))
                    else Result.failure(IllegalStateException("offline"))
                }
                .toList()
        states.last() shouldBe SpaceHierarchy(listOf(room), loading = false, failed = true)
    }

    @Test
    fun `repeated pagination token stops with an error`() = runTest {
        val states =
            loadSpaceHierarchy { Result.success(GetHierarchy.Response(nextBatch = "same", rooms = listOf(room))) }
                .toList()
        states.size shouldBe 3
        states.last().failed shouldBe true
    }

    @Test
    fun `cancellation is not converted to a load error`() = runTest {
        assertFailsWith<CancellationException> {
            loadSpaceHierarchy { Result.failure(CancellationException()) }.toList()
        }
    }
}
