package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.model.room.GetHierarchy
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.StateBaseEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.StrippedStateEvent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.component.get
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SpacePerformanceTest {
    @Test
    fun `rail respects an injected content factory`() = runTest {
        val fixture = SpaceBrowserFixture(UserId("alice", "example.org"), RoomId("!space:example.org"))
        val context = fixture.context(this)
        val expectedState = MutableStateFlow<SpaceContentState>(SpaceContentState.Error)
        val content = mock<SpaceContentViewModel>()
        every { content.state } returns expectedState
        val factory = mock<SpaceContentViewModelFactory>()
        every { factory.create(any(), any(), any(), any(), any(), any(), any()) } returns content
        context.getKoin().declare<SpaceContentViewModelFactory>(factory)
        val rail = SpaceNavigationRailViewModelFactory.create(context) {}
        assertSame(expectedState, rail.state)
    }

    @Test
    fun `joining a parent mid-batch does not refetch a completed parent`() = runTest {
        val user = UserId("alice", "example.org")
        val child = RoomId("!child:example.org")
        val a = RoomId("!a:example.org")
        val b = RoomId("!b:example.org")
        val membership = MutableStateFlow<Room?>(null)
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { client.api } returns api
        every { api.room } returns roomApi
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child)), a to membership))
        every { service.getAllState(a, ParentEventContent::class) } returns flowOf(emptyMap())
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(
                listOf(a, b).associate {
                    it.full to
                        flowOf(
                            ClientEvent.StrippedStateEvent(
                                ParentEventContent(via = setOf("example.org")),
                                sender = user,
                                stateKey = it.full,
                            )
                        )
                }
            )
        every { service.getById(a) } returns membership
        every { service.getById(b) } returns flowOf(null)
        val create =
            ClientEvent.StrippedStateEvent(
                CreateEventContent(type = CreateEventContent.RoomType.Space),
                sender = user,
                stateKey = "",
            )
        val power =
            ClientEvent.StrippedStateEvent(
                PowerLevelsEventContent(users = mapOf(user to 100)),
                sender = user,
                stateKey = "",
            )
        val member =
            ClientEvent.StrippedStateEvent(
                MemberEventContent(membership = Membership.JOIN),
                sender = user,
                stateKey = user.full,
            )
        every { service.getState(a, CreateEventContent::class, "") } returns flowOf(create)
        every { service.getState(a, PowerLevelsEventContent::class, "") } returns flowOf(power)
        every { service.getState(a, MemberEventContent::class, user.full) } returns flowOf(member)
        every { service.getState(a, ChildEventContent::class, child.full) } returns flowOf(null)
        val pendingParent = CompletableDeferred<Unit>()
        var otherRequests = 0
        everySuspend { roomApi.getState(any()) } calls
            { args ->
                val id = args.arg<RoomId>(0)
                if (id == b) otherRequests++
                if (id == a) pendingParent.await()
                Result.success(
                    listOf(create, power, member).map {
                        ClientEvent.RoomEvent.StateEvent(
                            it.content,
                            EventId("$" + it.stateKey),
                            it.sender,
                            id,
                            1L,
                            stateKey = it.stateKey,
                        )
                    }
                )
            }
        backgroundScope.launch { getSpaceParents(client).collect {} }
        runCurrent()
        assertEquals(1, otherRequests)
        membership.value = Room(a)
        runCurrent()
        assertEquals(1, otherRequests)
    }

    @Test
    fun `lean rail follows accounts membership and current avatar without discovery`() = runTest {
        val alice = UserId("alice", "example.org")
        val bob = UserId("bob", "example.org")
        val spaceId = RoomId("!shared:example.org")
        val first = SpaceBrowserFixture(alice, spaceId)
        val second = SpaceBrowserFixture(bob, spaceId)
        val context = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to first.client, bob to second.client)))
        val settings = context.get<MatrixMessengerSettingsHolder>()
        settings.create(alice, MatrixMessengerAccountSettingsBase(displayColor = 1))
        settings.create(bob, MatrixMessengerAccountSettingsBase(displayColor = 2))
        val selected = mutableListOf<SpaceBrowserItem>()
        val vm = SpaceNavigationRailViewModelFactory.create(context) { selected += it }
        val subscription = backgroundScope.launch { vm.state.collect() }
        runCurrent()
        fun items() = (vm.state.value as SpaceContentState.Ready).content.items
        items().map { it.userId to it.accountColor } shouldBe listOf(alice to 1L, bob to 2L)
        val stale = items().first()
        settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = bob) }
        runCurrent()
        items().map { it.userId } shouldBe listOf(bob)
        vm.select(stale)
        selected shouldBe emptyList()
        second.space.value = second.space.value!!.copy(avatarUrl = "mxc://example.org/new")
        runCurrent()
        vm.select(items().single())
        selected.single().avatarUrl shouldBe "mxc://example.org/new"
        second.space.value = second.space.value!!.copy(membership = Membership.LEAVE)
        runCurrent()
        items() shouldBe emptyList()
        vm.refresh()
        runCurrent()
        first.parentCollectors shouldBe 0
        second.parentCollectors shouldBe 0
        first.hierarchyRequests + second.hierarchyRequests shouldBe 0
        subscription.cancel()
    }

    @Test
    fun `large hierarchy visits each page once and retains every child`() = runTest {
        val pageSize = 100
        val pages = 100
        var requests = 0
        var result = SpaceHierarchy()
        loadSpaceHierarchy { token ->
                val page = token?.toInt() ?: 0
                requests++
                Result.success(
                    GetHierarchy.Response(
                        nextBatch = (page + 1).takeIf { it < pages }?.toString(),
                        rooms =
                            (0 until pageSize).map { index ->
                                hierarchyRoom(RoomId("!room${page * pageSize + index}:example.org"))
                            },
                    )
                )
            }
            .collect { result = it }
        requests shouldBe pages
        result.rooms.size shouldBe pageSize * pages
        result.loading shouldBe false
        result.failed shouldBe false
    }

    @Test
    fun `validating many parent links scans their state a bounded number of times`() {
        val user = UserId("alice", "example.org")
        val parent = RoomId("!parent:example.org")
        val candidates =
            (0 until 1_000).map { index ->
                SpaceParentCandidate(
                    RoomId("!child$index:example.org"),
                    StrippedStateEvent(
                        ParentEventContent(via = setOf("example.org")),
                        sender = user,
                        stateKey = parent.full,
                    ),
                )
            }
        val events: List<StateBaseEvent<*>> =
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
        var reads = 0
        val counted =
            object : AbstractList<StateBaseEvent<*>>() {
                override val size = events.size

                override fun get(index: Int): StateBaseEvent<*> {
                    reads++
                    return events[index]
                }
            }
        filterValidSpaceParents(candidates, counted) shouldBe candidates
        kotlin.test.assertTrue(reads <= events.size * 8, "Read $reads events for ${candidates.size} links")
    }

    @Test
    fun `rail in a large account collects rooms once and no discovery state`() = runTest {
        val user = UserId("alice", "example.org")
        val spaceId = RoomId("!space:example.org")
        val invitedId = RoomId("!invited:example.org")
        val type = CreateEventContent(type = CreateEventContent.RoomType.Space)
        val rooms =
            (0 until 2_000).map { Room(RoomId("!room$it:example.org")) } +
                Room(spaceId, createEventContent = type) +
                Room(invitedId, createEventContent = type, membership = Membership.INVITE)
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        var roomCollectors = 0
        var parentCollections = 0
        var childCollections = 0
        every { service.getAll() } returns
            flow {
                    emit(rooms.associate { it.roomId to flowOf(it) })
                    awaitCancellation()
                }
                .onStart { roomCollectors++ }
                .onCompletion { roomCollectors-- }
        every { service.getAllState(any(), ParentEventContent::class) } returns
            flowOf(emptyMap<String, Flow<StateBaseEvent<ParentEventContent>?>>()).onStart { parentCollections++ }
        every { service.getAllState(any(), ChildEventContent::class) } returns
            flowOf(emptyMap<String, Flow<StateBaseEvent<ChildEventContent>?>>()).onStart { childCollections++ }
        val vm =
            SpaceNavigationRailViewModelFactory.create(
                testSpaceBrowserContext(MutableStateFlow(mapOf(user to client))),
                onSelect = {},
            )
        runCurrent()
        roomCollectors shouldBe 0
        val subscription = backgroundScope.launch { vm.state.collect() }
        runCurrent()
        (vm.state.value as SpaceContentState.Ready).content.items.map { it.roomId }.toSet() shouldBe
            setOf(spaceId, invitedId)
        roomCollectors shouldBe 1
        parentCollections shouldBe 0
        childCollections shouldBe 1
        subscription.cancel()
        runCurrent()
        roomCollectors shouldBe 0
    }
}
