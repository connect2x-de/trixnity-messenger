package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomDisplayName
import de.connect2x.trixnity.clientserverapi.model.sync.Sync.Response.Rooms.JoinedRoom.RoomSummary
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SpaceChildStatisticsTest {
    private val user = UserId("alice", "example.org")
    private val parent = RoomId("!parent:example.org")
    private val nested = RoomId("!nested:example.org")
    private val unknown = RoomId("!unknown:example.org")

    private fun link(id: RoomId) =
        ClientEvent.StrippedStateEvent(ChildEventContent(via = setOf("example.org")), sender = user, stateKey = id.full)

    @Test
    fun `joined children prefer synced member counts with hierarchy fallback`() {
        for ((membership, localCount, expected) in
            listOf(
                Triple(Membership.JOIN, 5L, 5L),
                Triple(Membership.JOIN, null, 2L),
                Triple(Membership.LEAVE, 5L, 2L),
            )) {
            val room =
                Room(
                    nested,
                    membership = membership,
                    name =
                        RoomDisplayName(
                            explicitName = "Child",
                            isEmpty = false,
                            otherUsersCount = 4,
                            heroes = emptyList(),
                            summary = RoomSummary(joinedMemberCount = localCount),
                        ),
                )
            val items =
                buildSpaceHierarchyItems(
                    user,
                    parent,
                    mapOf(nested to room),
                    emptyMap(),
                    listOf(hierarchyRoom(nested).copy(joinedMembersCount = 2)),
                    emptyList(),
                )
            items.single().memberCount shouldBe expected
        }
    }

    @Test
    fun `hierarchy counts remote subspaces without classifying unknown children as rooms`() {
        val items =
            buildSpaceHierarchyItems(
                user,
                RoomId("!root:example.org"),
                mapOf(nested to Room(nested)),
                emptyMap(),
                listOf(
                    hierarchyRoom(parent)
                        .copy(
                            roomType = CreateEventContent.RoomType.Space,
                            childrenState = setOf(link(nested), link(unknown)),
                        ),
                    hierarchyRoom(nested).copy(roomType = CreateEventContent.RoomType.Space),
                ),
                emptyList(),
            )
        val result = items.single { it.roomId == parent }
        result.childCount shouldBe 2
        result.subspaceCount shouldBe 1
        result.unknownChildCount shouldBe 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `nested stats react to child metadata without notification services`() = runTest {
        val client = mock<MatrixClient>()
        val rooms = mock<RoomService>()
        every { client.di } returns koinApplication { modules(module { single { rooms } }) }.koin
        val unknownRoom = MutableStateFlow<Room?>(null)
        every { rooms.getAll() } returns flowOf(mapOf(unknown to unknownRoom))
        every { rooms.getAllState(parent, ChildEventContent::class) } returns
            flowOf(mapOf(nested.full to flowOf(link(nested)), unknown.full to flowOf(link(unknown))))
        val item = SpaceBrowserItem(user, parent, "Parent", isSpace = true, joined = true)
        val remote = SpaceBrowserItem(user, nested, "Nested", isSpace = true, joined = false)
        var result = item
        backgroundScope.launch { spaceChildStatistics(listOf(item, remote), client).collect { result = it.first() } }
        runCurrent()
        result.subspaceCount shouldBe 1
        result.unknownChildCount shouldBe 1
        unknownRoom.value =
            Room(unknown, createEventContent = CreateEventContent(type = CreateEventContent.RoomType.Space))
        runCurrent()
        result.subspaceCount shouldBe 2
        result.unknownChildCount shouldBe 0
    }
}
