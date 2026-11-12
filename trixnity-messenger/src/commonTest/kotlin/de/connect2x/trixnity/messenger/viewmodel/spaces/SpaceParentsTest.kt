package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.StateEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceParentsTest {
    private val alice = UserId("alice", "example.org")
    private val bob = UserId("bob", "example.org")
    private val child = RoomId("!child:example.org")
    private val parent = RoomId("!parent:example.org")

    private fun <C : StateEventContent> event(content: C, key: String = "", sender: UserId = alice) =
        ClientEvent.StrippedStateEvent(content, sender = sender, stateKey = key)

    private val parentEvent = event(ParentEventContent(via = setOf("example.org")), parent.full, bob)
    private val create = event(CreateEventContent(type = CreateEventContent.RoomType.Space))
    private val member = event(MemberEventContent(membership = Membership.JOIN), bob.full)
    private val power = event(PowerLevelsEventContent(users = mapOf(bob to 50)))

    @Test
    fun `parent validation does not collect unrelated members in a large space`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(mapOf(parent.full to flowOf(parentEvent)))
        every { service.getById(parent) } returns flowOf(Room(parent))
        every { service.getState(parent, CreateEventContent::class, "") } returns flowOf(create)
        every { service.getState(parent, PowerLevelsEventContent::class, "") } returns flowOf(power)
        every { service.getState(parent, ChildEventContent::class, child.full) } returns flowOf(null)
        var unrelatedCollections = 0
        var senderCollections = 0
        val sender = flowOf(member).onStart { senderCollections++ }
        every { service.getState(parent, MemberEventContent::class, bob.full) } returns sender
        every { service.getAllState(parent, MemberEventContent::class) } returns
            flowOf(
                (0 until 10_000).associate { index ->
                    val key = "@unrelated$index:example.org"
                    key to flowOf(member.copy(stateKey = key)).onStart { unrelatedCollections++ }
                } + (bob.full to sender)
            )

        getSpaceParents(client, parent).first().single().children shouldBe setOf(child)
        unrelatedCollections shouldBe 0
        senderCollections shouldBe 1
    }

    @Test
    fun `missing sender membership is loaded before validating a parent claim`() = runTest {
        for ((loadedMembership, reciprocal) in
            listOf(Membership.JOIN to false, Membership.LEAVE to false, null to false, null to true)) {
            val service = mock<RoomService>()
            val users = mock<UserService>()
            val client = mock<MatrixClient>()
            every { client.di } returns
                koinApplication {
                        modules(
                            module {
                                single { service }
                                single { users }
                            }
                        )
                    }
                    .koin
            every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
            every { service.getAllState(child, ParentEventContent::class) } returns
                flowOf(mapOf(parent.full to flowOf(parentEvent)))
            every { service.getById(parent) } returns flowOf(Room(parent))
            every { service.getState(parent, CreateEventContent::class, "") } returns flowOf(create)
            every { service.getState(parent, PowerLevelsEventContent::class, "") } returns flowOf(power)
            every { service.getState(parent, ChildEventContent::class, child.full) } returns
                flowOf(if (reciprocal) event(ChildEventContent(via = setOf("example.org")), child.full) else null)
            val membership = MutableStateFlow<ClientEvent.StateBaseEvent<MemberEventContent>?>(null)
            every { service.getState(parent, MemberEventContent::class, bob.full) } returns membership
            var loads = 0
            everySuspend { users.loadMembers(parent, false) } calls
                {
                    loads++
                    membership.value = loadedMembership?.let {
                        member.copy(content = MemberEventContent(membership = it))
                    }
                }
            var result = emptyList<SpaceParent>()
            val subscription = backgroundScope.launch { getSpaceParents(client, parent).collect { result = it } }
            runCurrent()
            result.isNotEmpty() shouldBe (reciprocal || loadedMembership == Membership.JOIN)
            loads shouldBe if (reciprocal) 0 else 1
            subscription.cancel()
        }
    }

    @Test
    fun `pending membership does not block cached parent links and updates when loaded`() = runTest {
        val service = mock<RoomService>()
        val users = mock<UserService>()
        val client = mock<MatrixClient>()
        every { client.di } returns
            koinApplication {
                    modules(
                        module {
                            single { service }
                            single { users }
                        }
                    )
                }
                .koin
        val cachedParent = RoomId("!cached:example.org")
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(
                mapOf(
                    parent.full to flowOf(parentEvent),
                    cachedParent.full to flowOf(parentEvent.copy(stateKey = cachedParent.full)),
                )
            )
        every { service.getById(any()) } calls { args -> flowOf(Room(args.arg<RoomId>(0))) }
        every { service.getState(any(), CreateEventContent::class, "") } returns flowOf(create)
        every { service.getState(any(), PowerLevelsEventContent::class, "") } returns flowOf(power)
        every { service.getState(parent, ChildEventContent::class, child.full) } returns flowOf(null)
        every { service.getState(cachedParent, ChildEventContent::class, child.full) } returns
            flowOf(event(ChildEventContent(via = setOf("example.org")), child.full))
        every { service.getState(cachedParent, MemberEventContent::class, bob.full) } returns flowOf(null)
        val membership = MutableStateFlow<ClientEvent.StateBaseEvent<MemberEventContent>?>(null)
        every { service.getState(parent, MemberEventContent::class, bob.full) } returns membership
        val release = CompletableDeferred<Unit>()
        var requests = 0
        everySuspend { users.loadMembers(parent, false) } calls
            {
                requests++
                backgroundScope.launch {
                    release.await()
                    membership.value = member
                }
                Unit
            }
        var result: List<SpaceParent>? = null
        backgroundScope.launch { getSpaceParents(client).collect { result = it } }
        runCurrent()
        result?.map { it.roomId } shouldBe listOf(cachedParent)
        requests shouldBe 1
        release.complete(Unit)
        runCurrent()
        result?.map { it.roomId }?.toSet() shouldBe setOf(cachedParent, parent)
        membership.value = member.copy(content = MemberEventContent(membership = Membership.LEAVE))
        runCurrent()
        result?.map { it.roomId } shouldBe listOf(cachedParent)
    }

    @Test
    fun `parent lookup and overview ignore unjoined parents without fetching them`() = runTest {
        val fixture = SpaceBrowserFixture(alice, child)
        every { fixture.rooms.getAllState(child, ParentEventContent::class) } returns
            flowOf(mapOf(parent.full to flowOf(parentEvent)))
        var requests = 0
        everySuspend { fixture.roomApi.getState(parent) } calls
            {
                requests++
                Result.failure(IllegalStateException("offline"))
            }
        var parents = emptyList<SpaceParent>()
        var overview = SpaceContent()
        val context = fixture.context(this)
        backgroundScope.launch { getSpaceParents(fixture.client).collect { parents = it } }
        backgroundScope.launch { context.getSpaceOverview(alice).collect { overview = it } }
        runCurrent()
        parents shouldBe emptyList()
        overview.items.map { it.roomId } shouldBe listOf(child)
        requests shouldBe 0
    }

    @Test
    fun `overview and rail follow joined parent claims without fetching departed parents`() = runTest {
        val fixture = SpaceBrowserFixture(alice, parent)
        fixture.space.value = fixture.space.value!!.copy(membership = Membership.LEAVE)
        fixture.content.value =
            SpaceContent(items = listOf(SpaceBrowserItem(alice, child, "Child", isSpace = true, joined = true)))
        every { fixture.rooms.getAllState(child, ParentEventContent::class) } returns
            flowOf(mapOf(parent.full to flowOf(parentEvent)))
        val reciprocal = MutableStateFlow<ClientEvent.StateBaseEvent<ChildEventContent>?>(null)
        every { fixture.rooms.getAllState(parent, ChildEventContent::class) } returns
            flowOf(mapOf(child.full to reciprocal))
        every { fixture.rooms.getState(parent, CreateEventContent::class, "") } returns flowOf(create)
        val powers = MutableStateFlow(power)
        every { fixture.rooms.getState(parent, PowerLevelsEventContent::class, "") } returns powers
        every { fixture.rooms.getState(parent, ChildEventContent::class, child.full) } returns reciprocal
        every { fixture.rooms.getState(parent, MemberEventContent::class, bob.full) } returns flowOf(member)
        var requests = 0
        everySuspend { fixture.roomApi.getState(parent) } calls
            {
                requests++
                Result.failure(IllegalStateException("offline"))
            }
        var overview = SpaceContent()
        val context = fixture.context(this)
        val rail = SpaceNavigationRailViewModelFactory.create(context) {}
        backgroundScope.launch { rail.state.collect {} }
        backgroundScope.launch { context.getSpaceOverview(alice).collect { overview = it } }
        fun railItems() = (rail.state.value as SpaceContentState.Ready).content.items.map { it.roomId }
        runCurrent()
        overview.items.map { it.roomId } shouldBe listOf(child)
        railItems() shouldBe listOf(child)
        fixture.space.value = fixture.space.value!!.copy(membership = Membership.JOIN)
        runCurrent()
        overview.items.map { it.roomId } shouldBe listOf(parent)
        overview.items.single().childCount shouldBe 1
        railItems() shouldBe listOf(parent)
        powers.value = power.copy(content = PowerLevelsEventContent(users = mapOf(bob to 49)))
        runCurrent()
        overview.items.map { it.roomId }.toSet() shouldBe setOf(parent, child)
        railItems().toSet() shouldBe setOf(parent, child)
        powers.value = power
        runCurrent()
        overview.items.map { it.roomId } shouldBe listOf(parent)
        overview.items.single().childCount shouldBe 1
        railItems() shouldBe listOf(parent)
        reciprocal.value = event(ChildEventContent(via = setOf(alice.domain)), child.full)
        runCurrent()
        overview.items.single().childCount shouldBe 1
        railItems() shouldBe listOf(parent)
        fixture.space.value = fixture.space.value!!.copy(membership = Membership.LEAVE)
        runCurrent()
        overview.items.map { it.roomId } shouldBe listOf(child)
        railItems() shouldBe listOf(child)
        requests shouldBe 0
    }

    @Test
    fun `parent claims require reciprocal child or joined sender with sufficient power`() {
        isValidSpaceParent(child, parentEvent, listOf(create, member, power)) shouldBe true
        isValidSpaceParent(child, parentEvent, listOf(create, member)) shouldBe false
        isValidSpaceParent(child, parentEvent, listOf(create, power)) shouldBe false
        isValidSpaceParent(
            child,
            parentEvent,
            listOf(create, power, member.copy(content = MemberEventContent(membership = Membership.LEAVE))),
        ) shouldBe false
        isValidSpaceParent(
            child,
            parentEvent,
            listOf(create, event(ChildEventContent(via = setOf("example.org")), child.full)),
        ) shouldBe true
        isValidSpaceParent(child, parentEvent, emptyList()) shouldBe false
        isValidSpaceParent(
            child,
            parentEvent.copy(content = ParentEventContent(via = emptySet())),
            listOf(create, member, power),
        ) shouldBe false
        isValidSpaceParent(parent, parentEvent, listOf(create, member, power)) shouldBe false
        isValidSpaceParent(
            child,
            parentEvent,
            listOf(create.copy(content = CreateEventContent()), member, power),
        ) shouldBe false
    }

    @Test
    fun `room v12 creators retain permission regardless of numeric state default`() {
        val creator = create.copy(content = create.content.copy(roomVersion = "12", additionalCreators = setOf(bob)))
        isValidSpaceParent(
            child,
            parentEvent,
            listOf(creator, member, power.copy(content = PowerLevelsEventContent(stateDefault = Long.MAX_VALUE))),
        ) shouldBe true
    }

    @Test
    fun `parent links react to demotion departure and link removal`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        val parentLink = MutableStateFlow(parentEvent)
        every { service.getAllState(child, ParentEventContent::class) } returns flowOf(mapOf(parent.full to parentLink))
        every { service.getById(parent) } returns flowOf(Room(parent))
        every { service.getState(parent, CreateEventContent::class, "") } returns flowOf(create)
        val powers = MutableStateFlow(power)
        val membership = MutableStateFlow(member)
        every { service.getState(parent, PowerLevelsEventContent::class, "") } returns powers
        every { service.getState(parent, ChildEventContent::class, child.full) } returns flowOf(null)
        every { service.getState(parent, MemberEventContent::class, bob.full) } returns membership
        var latest = emptyList<SpaceParent>()
        backgroundScope.launch { getSpaceParents(client, parent).collect { latest = it } }
        runCurrent()
        latest.single().children shouldBe setOf(child)
        powers.value = power.copy(content = PowerLevelsEventContent(users = mapOf(bob to 49)))
        runCurrent()
        latest shouldBe emptyList()
        powers.value = power
        runCurrent()
        latest.size shouldBe 1
        membership.value = member.copy(content = MemberEventContent(membership = Membership.LEAVE))
        runCurrent()
        latest shouldBe emptyList()
        membership.value = member
        runCurrent()
        parentLink.value = parentEvent.copy(content = ParentEventContent(via = emptySet()))
        runCurrent()
        latest shouldBe emptyList()
    }

    @Test
    fun `parent discovery accepts legacy and serverless room ids and rejects malformed ids`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        val validParents = listOf(parent, RoomId("!serverless"))
        val keys =
            validParents.map { it.full } + listOf("invalid", "!parent:example.org/invalid", "!" + "a".repeat(255))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(keys.associateWith { flowOf(parentEvent.copy(stateKey = it)) })
        validParents.forEach { roomId ->
            every { service.getById(roomId) } returns flowOf(Room(roomId))
            every { service.getState(roomId, CreateEventContent::class, "") } returns flowOf(create)
            every { service.getState(roomId, PowerLevelsEventContent::class, "") } returns flowOf(power)
            every { service.getState(roomId, ChildEventContent::class, child.full) } returns flowOf(null)
            every { service.getState(roomId, MemberEventContent::class, bob.full) } returns flowOf(member)
        }
        getSpaceParents(client).first().map { it.roomId }.toSet() shouldBe validParents.toSet()
    }
}
