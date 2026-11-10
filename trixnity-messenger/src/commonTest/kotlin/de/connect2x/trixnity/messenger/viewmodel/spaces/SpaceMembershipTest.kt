package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceMembershipTest {
    private val account = UserId("second", "example.org")
    private val spaceId = RoomId("!space:example.org")
    private val childId = RoomId("!child:example.org")
    private val space = Room(spaceId, createEventContent = CreateEventContent(type = CreateEventContent.RoomType.Space))
    private val membership = MutableStateFlow<Room?>(space)
    private val client = mock<MatrixClient>()
    private val api = mock<MatrixClientServerApiClient>()
    private val roomApi = mock<RoomApiClient>()
    private val rooms = mock<RoomService>()
    private val TestScope.source
        get() =
            testSpaceBrowserContext(
                MutableStateFlow(mapOf(UserId("first", "example.org") to mock<MatrixClient>(), account to client))
            )

    init {
        every { client.api } returns api
        every { api.room } returns roomApi
        every { client.di } returns koinApplication { modules(module { single { rooms } }) }.koin
        every { rooms.getById(spaceId) } returns membership
        every {
            rooms.getAllState(any(), de.connect2x.trixnity.core.model.events.m.space.ChildEventContent::class)
        } returns flowOf(emptyMap())
        every { rooms.getAllState(any(), ParentEventContent::class) } returns flowOf(emptyMap())
        every { rooms.getAll() } returns flowOf(mapOf(spaceId to membership, childId to flowOf(Room(childId))))
    }

    @Test
    fun `rejecting a space invitation targets the account and waits for sync`() = runTest {
        val invited = space.copy(membership = Membership.INVITE)
        membership.value = invited
        everySuspend { roomApi.leaveRoom(spaceId, null) } returns Result.success(Unit)
        val item = invited.toBrowserItem(account)
        val pending = async { source.rejectSpaceInvite(item) }
        runCurrent()
        pending.isCompleted shouldBe false
        membership.value = invited.copy(membership = Membership.LEAVE)
        pending.await()
        assertFailsWith<IllegalStateException> { source.rejectSpaceInvite(item) }
        assertFailsWith<IllegalArgumentException> { source.rejectSpaceInvite(space.toBrowserItem(account)) }
    }

    @Test
    fun `public subspace join uses route account and via then waits for sync`() = runTest {
        membership.value = null
        val via = setOf("routing.example.org")
        everySuspend { roomApi.joinRoom(spaceId, via, null, null) } returns Result.success(spaceId)
        val pending = async {
            source.joinSpaceRoom(
                SpaceBrowserItem(
                    account,
                    spaceId,
                    "Subspace",
                    isSpace = true,
                    joined = false,
                    canJoin = true,
                    via = via,
                )
            )
        }
        runCurrent()
        pending.isCompleted shouldBe false
        membership.value = space
        runCurrent()
        pending.await()
    }
}
