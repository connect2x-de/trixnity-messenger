package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomDisplayName
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.model.room.GetHierarchy
import de.connect2x.trixnity.clientserverapi.model.sync.Sync.Response.Rooms.JoinedRoom.RoomSummary
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.TopicEventContent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SpaceContentTest {
    private val alice = UserId("alice", "example.org")
    private val bob = UserId("bob", "example.org")
    private val spaceId = RoomId("!space:example.org")
    private val roomId = RoomId("!room:example.org")
    private val space = Room(spaceId, createEventContent = CreateEventContent(type = CreateEventContent.RoomType.Space))

    private fun client(rooms: List<Room>, service: RoomService = mock()): MatrixClient {
        val client = mock<MatrixClient>()
        every { service.getAllState(any(), ChildEventContent::class) } returns flowOf(emptyMap())
        every { service.getState(any(), TopicEventContent::class, any()) } returns flowOf(null)
        every { service.getAllState(any(), ParentEventContent::class) } returns flowOf(emptyMap())
        every { service.getAll() } returns flowOf(rooms.associate { it.roomId to flowOf(it) })
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        return client
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `unrelated room updates do not restart space content subscriptions`() = runTest {
        for (explore in listOf(false, true)) {
            val service = mock<RoomService>()
            val client = client(emptyList(), service)
            val otherId = RoomId("!unrelated:example.org")
            val other = MutableStateFlow<Room?>(Room(otherId))
            every { service.getAll() } returns
                flowOf(mapOf(spaceId to flowOf(space), roomId to flowOf(Room(roomId)), otherId to other))
            every { service.getById(spaceId) } returns flowOf(space)
            every { service.getAllState(spaceId, ChildEventContent::class) } returns
                flowOf(
                    mapOf(
                        roomId.full to
                            flowOf(
                                ClientEvent.StrippedStateEvent(
                                    ChildEventContent(via = setOf("example.org")),
                                    sender = alice,
                                    stateKey = roomId.full,
                                )
                            )
                    )
                )
            val api = mock<MatrixClientServerApiClient>()
            val roomApi = mock<RoomApiClient>()
            every { client.api } returns api
            every { api.room } returns roomApi
            everySuspend { roomApi.getHierarchy(spaceId, from = any(), maxDepth = 1) } returns
                Result.success(GetHierarchy.Response(rooms = listOf(hierarchyRoom(roomId))))
            var nameCollections = 0
            val names = mock<RoomName>()
            every { names.getRoomName(any<RoomId>(), client, false) } returns
                flowOf("Room").onStart { nameCollections++ }
            val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client)), names)
            val content =
                if (explore) source.getSpaceExploreContent(alice, spaceId)
                else source.getJoinedSpaceContent(alice, spaceId)
            val subscription = backgroundScope.launch { content.collect() }
            runCurrent()
            val initialCollections = nameCollections
            repeat(20) { index ->
                other.value = other.value!!.copy(avatarUrl = "mxc://example.org/$index")
                runCurrent()
            }
            nameCollections shouldBe initialCollections
            subscription.cancel()
            runCurrent()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `explore keeps name and topic subscriptions when content changes`() = runTest {
        val fixture = SpaceBrowserFixture(alice, spaceId)
        val name = MutableStateFlow("Space")
        val topic = MutableStateFlow<ClientEvent.StateBaseEvent<TopicEventContent>?>(null)
        var nameCollections = 0
        var topicCollections = 0
        every { fixture.roomName.getRoomName(spaceId, fixture.client, false) } returns
            name.onStart { nameCollections++ }
        every { fixture.rooms.getState(spaceId, TopicEventContent::class, "") } returns
            topic.onStart { topicCollections++ }
        var latest = SpaceContent()
        backgroundScope.launch {
            fixture.context(this@runTest).getSpaceExploreContent(alice, spaceId).collect { latest = it }
        }
        runCurrent()
        fixture.content.value = SpaceContent(items = listOf(SpaceBrowserItem(alice, roomId, "Child", false, true)))
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        latest.items.single().roomId shouldBe roomId
        fixture.space.value = fixture.space.value!!.copy(avatarUrl = "mxc://example.org/avatar")
        runCurrent()
        latest.avatarUrl shouldBe "mxc://example.org/avatar"
        name.value = "Renamed"
        runCurrent()
        latest.name shouldBe "Renamed"
        nameCollections shouldBe 1
        topicCollections shouldBe 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `explore member count follows sync and falls back to hierarchy when unknown`() = runTest {
        val spaceState = MutableStateFlow(space)
        val source =
            detailSource(
                hierarchy =
                    Result.success(
                        GetHierarchy.Response(rooms = listOf(hierarchyRoom(spaceId).copy(joinedMembersCount = 4)))
                    ),
                spaceState = spaceState,
            )
        var latest = SpaceContent()
        backgroundScope.launch { source.getSpaceExploreContent(alice, spaceId).collect { latest = it } }
        runCurrent()
        latest.memberCount shouldBe 4
        spaceState.value =
            space.copy(
                name =
                    RoomDisplayName(
                        explicitName = "Space",
                        isEmpty = false,
                        otherUsersCount = 4,
                        heroes = emptyList(),
                        summary = RoomSummary(joinedMemberCount = 5),
                    )
            )
        advanceTimeBy(1_000)
        runCurrent()
        latest.memberCount shouldBe 5
        spaceState.value =
            space.copy(
                name =
                    RoomDisplayName(
                        explicitName = "Space",
                        isEmpty = false,
                        otherUsersCount = 2,
                        heroes = emptyList(),
                        summary = RoomSummary(joinedMemberCount = 3),
                    )
            )
        advanceTimeBy(1_000)
        runCurrent()
        latest.memberCount shouldBe 3
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `explore topic updates and disappears when cleared`() = runTest {
        val topic = MutableStateFlow<ClientEvent.StateBaseEvent<TopicEventContent>?>(null)
        val source = detailSource(topic = topic)
        var latest = SpaceContent()
        backgroundScope.launch { source.getSpaceExploreContent(alice, spaceId).collect { latest = it } }
        runCurrent()
        latest.topic shouldBe null
        topic.value = ClientEvent.StrippedStateEvent(TopicEventContent("Space topic"), sender = alice, stateKey = "")
        runCurrent()
        latest.topic shouldBe "Space topic"
        topic.value = ClientEvent.StrippedStateEvent(TopicEventContent("Changed topic"), sender = alice, stateKey = "")
        runCurrent()
        latest.topic shouldBe "Changed topic"
        topic.value = ClientEvent.StrippedStateEvent(TopicEventContent("  "), sender = alice, stateKey = "")
        runCurrent()
        latest.topic shouldBe null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sidebar uses local joined children without requesting hierarchy and reacts to membership changes`() = runTest {
        val service = mock<RoomService>()
        val joined = MutableStateFlow<Room?>(Room(roomId))
        val otherId = RoomId("!other:example.org")
        val other = MutableStateFlow<Room?>(Room(otherId, membership = Membership.LEAVE))
        val client = client(emptyList(), service)
        every { service.getAll() } returns flowOf(mapOf(spaceId to flowOf(space), roomId to joined, otherId to other))
        every { service.getById(spaceId) } returns flowOf(space)
        val child =
            MutableStateFlow(
                ClientEvent.StrippedStateEvent(
                    ChildEventContent(via = setOf("example.org")),
                    sender = alice,
                    stateKey = roomId.full,
                )
            )
        every { service.getAllState(spaceId, ChildEventContent::class) } returns
            flowOf(mapOf(roomId.full to child, otherId.full to flowOf(child.value.copy(stateKey = otherId.full))))
        val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client)))
        var latest = SpaceContent()
        backgroundScope.launch { source.getJoinedSpaceContent(alice, spaceId).collect { latest = it } }
        runCurrent()
        latest.items.map { it.roomId } shouldBe listOf(roomId)
        latest.loadingHierarchy shouldBe false
        latest.hierarchyFailed shouldBe false
        other.value = Room(otherId)
        runCurrent()
        latest.items.map { it.roomId }.toSet() shouldBe setOf(roomId, otherId)
        child.value = child.value.copy(content = ChildEventContent(via = emptySet()))
        runCurrent()
        latest.items.map { it.roomId } shouldBe listOf(otherId)
        other.value = other.value?.copy(membership = Membership.LEAVE)
        runCurrent()
        latest.items shouldBe emptyList()
    }

    @Test
    fun `list preserves account identity for a shared space and filters the selected account`() = runTest {
        val clients = MutableStateFlow(mapOf(alice to client(listOf(space)), bob to client(listOf(space))))
        val source = testSpaceBrowserContext(clients)
        source.getSpaceOverview(null).first().items.map { it.userId } shouldBe listOf(alice, bob)
        source.getSpaceOverview(bob).first().items.map { it.userId } shouldBe listOf(bob)
        clients.value = emptyMap()
        source.getSpaceOverview(null).first().items shouldBe emptyList()
    }

    @Test
    fun `list includes joined spaces and invitations but excludes rooms`() = runTest {
        val child = space.copy(roomId = RoomId("!child:example.org"))
        val invited = space.copy(roomId = RoomId("!invite:example.org"), membership = Membership.INVITE)
        val source =
            testSpaceBrowserContext(
                MutableStateFlow(mapOf(alice to client(listOf(space, child, invited, Room(roomId)))))
            )
        val items = source.getSpaceOverview(null).first().items
        source.getSpaceOverview(null, loadDetails = false).first().items.map { it.roomId } shouldBe
            items.map { it.roomId }
        items.map { it.roomId }.toSet() shouldBe setOf(spaceId, child.roomId, invited.roomId)
        items.single { it.roomId == invited.roomId }.let { it.invited && it.canJoin && !it.joined } shouldBe true
    }

    @Test
    fun `explore shows invitation without fetching hierarchy`() = runTest {
        val service = mock<RoomService>()
        val invited = space.copy(membership = Membership.INVITE)
        every { service.getById(spaceId) } returns flowOf(invited)
        val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client(listOf(invited), service))))
        val content = source.getSpaceExploreContent(alice, spaceId).first()
        content.invited shouldBe true
        content.items.single().let { it.invited && it.canJoin && !it.joined } shouldBe true
    }

    private fun TestScope.detailSource(
        children: Map<RoomId, Set<String>> = emptyMap(),
        hierarchy: Result<GetHierarchy.Response> = Result.success(GetHierarchy.Response(rooms = emptyList())),
        childMembership: Membership = Membership.JOIN,
        topic: StateFlow<ClientEvent.StateBaseEvent<TopicEventContent>?> = MutableStateFlow(null),
        spaceState: StateFlow<Room> = MutableStateFlow(space),
    ): ViewModelContext {
        val service = mock<RoomService>()
        val client = client(listOf(space, Room(roomId, membership = childMembership)), service)
        every { service.getState(spaceId, TopicEventContent::class, any()) } returns topic
        every { service.getById(spaceId) } returns spaceState
        every { service.getAll() } returns
            flowOf(mapOf(spaceId to spaceState, roomId to flowOf(Room(roomId, membership = childMembership))))
        every { service.getAllState(spaceId, ChildEventContent::class) } returns
            flowOf(
                children
                    .mapKeys { it.key.full }
                    .mapValues { (id, via) ->
                        flowOf(
                            ClientEvent.StrippedStateEvent(ChildEventContent(via = via), sender = alice, stateKey = id)
                        )
                    }
            )
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.api } returns api
        every { api.room } returns roomApi
        everySuspend { roomApi.getHierarchy(spaceId, any(), any(), 1, any()) } returns hierarchy
        return testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `new unjoined child triggers hierarchy reload`() = runTest {
        val service = mock<RoomService>()
        val client = client(listOf(space), service)
        every { service.getById(spaceId) } returns flowOf(space)
        val child =
            MutableStateFlow(
                ClientEvent.StrippedStateEvent(
                    ChildEventContent(via = emptySet()),
                    sender = alice,
                    stateKey = roomId.full,
                )
            )
        every { service.getAllState(spaceId, ChildEventContent::class) } returns flowOf(mapOf(roomId.full to child))
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.api } returns api
        every { api.room } returns roomApi
        var requests = 0
        everySuspend { roomApi.getHierarchy(spaceId, any(), any(), 1, any()) } calls
            {
                requests++
                Result.success(
                    GetHierarchy.Response(
                        rooms =
                            if (child.value.content.via.isEmpty()) emptyList()
                            else listOf(hierarchyRoom(roomId).copy(name = "New room", joinRule = JoinRule.Public))
                    )
                )
            }
        val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client)))
        var latest = SpaceContent()
        backgroundScope.launch { source.getSpaceExploreContent(alice, spaceId).collect { latest = it } }
        runCurrent()
        latest.items shouldBe emptyList()
        requests shouldBe 1
        child.value = child.value.copy(content = ChildEventContent(via = setOf("example.org")))
        runCurrent()
        requests shouldBe 2
        latest.items.single().name shouldBe "New room"
        latest.items.single().canJoin shouldBe true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `joined rooms use reactive room names`() = runTest {
        val client = client(listOf(space))
        val names = MutableStateFlow("Alice and Bob")
        val roomName = mock<RoomName>()
        every { roomName.getRoomName(spaceId, client, false) } returns names
        val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client)), roomName)
        var latest = SpaceContent()
        backgroundScope.launch { source.getSpaceOverview(alice).collect { latest = it } }
        runCurrent()
        latest.items.single().name shouldBe "Alice and Bob"
        names.value = "Alice and Charlie"
        runCurrent()
        latest.items.single().name shouldBe "Alice and Charlie"
    }

    @Test
    fun `unjoined rooms prefer current hierarchy names over local records`() = runTest {
        val source =
            detailSource(
                hierarchy =
                    Result.success(
                        GetHierarchy.Response(rooms = listOf(hierarchyRoom(roomId).copy(name = "Current name")))
                    ),
                childMembership = Membership.LEAVE,
            )
        source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }.items.single().name shouldBe
            "Current name"
    }

    @Test
    fun `empty space finishes loading`() = runTest {
        val content = detailSource().getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }
        content.items shouldBe emptyList()
        content.hierarchyFailed shouldBe false
    }

    @Test
    fun `hierarchy failure leaves joined children available`() = runTest {
        val source =
            detailSource(mapOf(roomId to setOf("example.org")), Result.failure(IllegalStateException("offline")))
        val content = source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }
        content.hierarchyFailed shouldBe true
        content.items.single().roomId shouldBe roomId
        content.items.single().userId shouldBe alice
        content.items.single().joined shouldBe true
    }

    @Test
    fun `removed child links are excluded even when hierarchy still returns them`() = runTest {
        val source =
            detailSource(
                mapOf(roomId to emptySet()),
                Result.success(GetHierarchy.Response(rooms = listOf(hierarchyRoom(roomId)))),
            )
        source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }.items shouldBe emptyList()
    }

    @Test
    fun `invites and eligible restricted memberships can join but bans cannot`() = runTest {
        for (rule in
            listOf(JoinRule.Invite, JoinRule.Restricted, JoinRule.KnockRestricted, JoinRule.Knock, JoinRule.Public)) {
            for (membership in listOf(Membership.INVITE, Membership.LEAVE, Membership.BAN, Membership.JOIN)) {
                for (allowed in listOf(setOf(spaceId), setOf(RoomId("!elsewhere:example.org")))) {
                    val source =
                        detailSource(
                            hierarchy =
                                Result.success(
                                    GetHierarchy.Response(
                                        rooms =
                                            listOf(
                                                hierarchyRoom(roomId).copy(joinRule = rule, allowedRoomIds = allowed)
                                            )
                                    )
                                ),
                            childMembership = membership,
                        )
                    val expected =
                        membership != Membership.BAN &&
                            membership != Membership.JOIN &&
                            (membership == Membership.INVITE ||
                                rule == JoinRule.Public ||
                                (rule in setOf(JoinRule.Restricted, JoinRule.KnockRestricted) && spaceId in allowed))
                    source
                        .getSpaceExploreContent(alice, spaceId)
                        .first { !it.loadingHierarchy }
                        .items
                        .single()
                        .canJoin shouldBe expected
                }
            }
        }
    }

    @Test
    fun `home preserves hierarchy child order after resolving room names`() = runTest {
        val early = RoomId("!early:example.org")
        val late = RoomId("!late:example.org")
        val parent =
            hierarchyRoom(spaceId)
                .copy(
                    childrenState =
                        setOf(
                            ClientEvent.StrippedStateEvent(
                                ChildEventContent(via = setOf("example.org"), order = "b"),
                                sender = alice,
                                stateKey = early.full,
                                originTimestamp = 1,
                            ),
                            ClientEvent.StrippedStateEvent(
                                ChildEventContent(via = setOf("example.org"), order = "a"),
                                sender = alice,
                                stateKey = late.full,
                                originTimestamp = 2,
                            ),
                        )
                )
        val source =
            detailSource(
                hierarchy =
                    Result.success(
                        GetHierarchy.Response(
                            rooms =
                                listOf(
                                    parent,
                                    hierarchyRoom(early).copy(name = "A"),
                                    hierarchyRoom(late).copy(name = "Z"),
                                )
                        )
                    )
            )
        source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }.items.map { it.roomId } shouldBe
            listOf(late, early)
    }

    @Test
    fun `without invites or allowed membership only public rooms and subspaces can be joined`() = runTest {
        for (rule in listOf(JoinRule.Public, JoinRule.Invite, JoinRule.Restricted, JoinRule.Knock)) {
            val source =
                detailSource(
                    mapOf(roomId to setOf("routing.example.org")),
                    Result.success(GetHierarchy.Response(rooms = listOf(hierarchyRoom(roomId).copy(joinRule = rule)))),
                    childMembership = Membership.LEAVE,
                )
            val item = source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }.items.single()
            item.canJoin shouldBe (rule == JoinRule.Public)
            item.via shouldBe setOf("routing.example.org")
        }
        val source =
            detailSource(
                hierarchy =
                    Result.success(
                        GetHierarchy.Response(
                            rooms = listOf(hierarchyRoom(roomId).copy(roomType = CreateEventContent.RoomType.Space))
                        )
                    ),
                childMembership = Membership.LEAVE,
            )
        source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }.items.single().canJoin shouldBe
            true
    }

    @Test
    fun `join sends account and routing servers then waits for synced membership`() = runTest {
        val service = mock<RoomService>()
        val client = client(emptyList(), service)
        val membership = MutableStateFlow<Room?>(null)
        every { service.getById(roomId) } returns membership
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.api } returns api
        every { api.room } returns roomApi
        val via = setOf("routing.example.org")
        everySuspend { roomApi.joinRoom(roomId, via, null, null) } returns Result.success(roomId)
        val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to mock<MatrixClient>(), bob to client)))
        val pending = async {
            source.joinSpaceRoom(SpaceBrowserItem(bob, roomId, "Room", false, false, canJoin = true, via = via))
        }
        runCurrent()
        pending.isCompleted shouldBe false
        membership.value = Room(roomId, membership = Membership.JOIN)
        runCurrent()
        pending.await()
    }

    @Test
    fun `list and detail header expose the spaces current avatar`() = runTest {
        val avatarUrl = "mxc://example.org/space-avatar"
        val service = mock<RoomService>()
        val client = client(listOf(space.copy(avatarUrl = avatarUrl)), service)
        val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client)))
        source.getSpaceOverview(alice).first().items.single().avatarUrl shouldBe avatarUrl
        every { service.getById(spaceId) } returns flowOf(space.copy(avatarUrl = avatarUrl))
        every { service.getAllState(spaceId, ChildEventContent::class) } returns flowOf(emptyMap())
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.api } returns api
        every { api.room } returns roomApi
        everySuspend { roomApi.getHierarchy(spaceId, any(), any(), 1, any()) } returns
            Result.success(GetHierarchy.Response(rooms = emptyList()))
        val content = source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }
        content.avatarUrl shouldBe avatarUrl
        content.userId shouldBe alice
    }

    @Test
    fun `unjoined children expose hierarchy avatars`() = runTest {
        val otherRoom = RoomId("!unjoined:example.org")
        val url = "mxc://example.org/child-avatar"
        val source =
            detailSource(
                hierarchy =
                    Result.success(
                        GetHierarchy.Response(rooms = listOf(hierarchyRoom(otherRoom).copy(avatarUrl = url)))
                    )
            )
        source.getSpaceExploreContent(alice, spaceId).first { !it.loadingHierarchy }.items.single().avatarUrl shouldBe
            url
    }
}

internal fun hierarchyRoom(roomId: RoomId) =
    GetHierarchy.Response.SpaceHierarchyRoomsChunk(
        roomId = roomId,
        childrenState = emptySet(),
        guestCanJoin = false,
        joinedMembersCount = 0,
        worldReadable = false,
    )

internal fun TestScope.testSpaceBrowserContext(
    clients: StateFlow<Map<UserId, MatrixClient>>,
    roomName: RoomName =
        mock<RoomName>().also { every { it.getRoomName(any<RoomId>(), any(), false) } returns flowOf("Room") },
): ViewModelContext {
    val registry = mock<MatrixClients>()
    every { registry.value } calls { clients.value }
    everySuspend { registry.collect(any()) } calls { clients.collect(it.arg(0)) }
    return testViewModelContext(
        koinApplication {
                modules(
                    module {
                        single { registry }
                        single<SpaceContentViewModelFactory> { SpaceContentViewModelFactory }
                        single { createTestMatrixMessengerSettingsHolder() }
                        single { roomName }
                        single<BackHandler> { BackHandlerImpl() }
                    }
                )
            }
            .koin
    )
}
