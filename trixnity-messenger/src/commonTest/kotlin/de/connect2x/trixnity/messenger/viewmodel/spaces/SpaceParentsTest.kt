package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.StateEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.NameEventContent
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
    fun `remote parent discovery bounds requests and releases them when hidden`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { client.api } returns api
        every { api.room } returns roomApi
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(
                (0 until 20).associate { index ->
                    val id = RoomId("!parent$index:example.org")
                    id.full to flowOf(parentEvent.copy(stateKey = id.full))
                }
            )
        every { service.getById(any()) } returns flowOf(null)
        val release = CompletableDeferred<Unit>()
        var active = 0
        var peak = 0
        var requests = 0
        everySuspend { roomApi.getState(any()) } calls
            {
                requests++
                active++
                peak = maxOf(peak, active)
                try {
                    release.await()
                    Result.success(emptyList())
                } finally {
                    active--
                }
            }
        val subscription = backgroundScope.launch { getSpaceParents(client).collect {} }
        runCurrent()
        active shouldBe 4
        subscription.cancel()
        runCurrent()
        active shouldBe 0
        requests shouldBe 4
        release.complete(Unit)
        backgroundScope.launch { getSpaceParents(client).collect {} }
        runCurrent()
        requests shouldBe 24
        peak shouldBe 4
    }

    @Test
    fun `remote batches publish incrementally while joined parents stay reactive`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { client.api } returns api
        every { api.room } returns roomApi
        val remoteParents = (0 until 5).map { RoomId("!remote$it:example.org") }
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(
                (listOf(parent) + remoteParents).associate { it.full to flowOf(parentEvent.copy(stateKey = it.full)) }
            )
        every { service.getById(any()) } returns flowOf(null)
        every { service.getById(parent) } returns flowOf(Room(parent))
        every { service.getState(parent, CreateEventContent::class, "") } returns flowOf(create)
        val powers = MutableStateFlow(power)
        every { service.getState(parent, PowerLevelsEventContent::class, "") } returns powers
        every { service.getState(parent, ChildEventContent::class, child.full) } returns flowOf(null)
        every { service.getState(parent, MemberEventContent::class, bob.full) } returns flowOf(member)
        val gates = List(5) { CompletableDeferred<Unit>() }
        val requests = mutableListOf<RoomId>()
        everySuspend { roomApi.getState(any()) } calls
            { args ->
                val id = args.arg<RoomId>(0)
                val index = remoteParents.indexOf(id)
                requests += id
                gates[index].await()
                if (index == 1) Result.failure(IllegalStateException("unavailable"))
                else
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
        var latest = emptyList<SpaceParent>()
        backgroundScope.launch { getSpaceParents(client).collect { latest = it } }
        runCurrent()
        requests shouldBe remoteParents.take(4)
        latest.map { it.roomId } shouldBe listOf(parent)
        powers.value = power.copy(content = PowerLevelsEventContent(users = mapOf(bob to 49)))
        runCurrent()
        latest shouldBe emptyList()
        gates[0].complete(Unit)
        runCurrent()
        requests.size shouldBe 4
        latest.map { it.roomId } shouldBe listOf(remoteParents[0])
        (1..3).forEach { gates[it].complete(Unit) }
        runCurrent()
        requests shouldBe remoteParents
        latest.map { it.roomId }.toSet() shouldBe setOf(remoteParents[0], remoteParents[2], remoteParents[3])
        gates[4].complete(Unit)
        runCurrent()
        latest.map { it.roomId }.toSet() shouldBe (remoteParents - remoteParents[1]).toSet()
        powers.value = power
        runCurrent()
        latest.map { it.roomId }.toSet() shouldBe (remoteParents - remoteParents[1] + parent).toSet()
        requests.size shouldBe 5
    }

    @Test
    fun `unjoined parent retains only state needed for validation and display`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { client.api } returns api
        every { api.room } returns roomApi
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(mapOf(parent.full to flowOf(parentEvent)))
        every { service.getById(parent) } returns flowOf(null)
        val needed =
            listOf(
                create,
                power,
                member,
                event(NameEventContent("Discovered")),
                event(JoinRulesEventContent(JoinRulesEventContent.JoinRule.Public)),
            )
        val unrelated =
            (0 until 10_000).map { index ->
                event(MemberEventContent(membership = Membership.JOIN), "@other$index:example.org")
            }
        everySuspend { roomApi.getState(parent) } returns
            Result.success(
                (needed + unrelated).map {
                    ClientEvent.RoomEvent.StateEvent(
                        it.content,
                        EventId("$" + it.stateKey),
                        it.sender,
                        parent,
                        1L,
                        stateKey = it.stateKey,
                    )
                }
            )
        val result = getSpaceParents(client).first { it.isNotEmpty() }.single()
        result.state.map { it.content to it.stateKey } shouldBe needed.map { it.content to it.stateKey }
        result.children shouldBe setOf(child)
        result.toBrowserItem(alice, emptyMap()).name shouldBe "Discovered"
        result.toBrowserItem(alice, emptyMap()).canJoin shouldBe true
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
    fun `parent lookup retries when discovery restarts`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { client.api } returns api
        every { api.room } returns roomApi
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(mapOf(parent.full to flowOf(parentEvent)))
        every { service.getById(parent) } returns flowOf(null)
        val state =
            listOf(
                create,
                power,
                member,
                event(NameEventContent("Discovered")),
                event(JoinRulesEventContent(JoinRulesEventContent.JoinRule.Knock)),
            )
        everySuspend { roomApi.getState(parent) } returns Result.failure(IllegalStateException("offline"))
        var rail = emptyList<SpaceParent>()
        val railSubscription = backgroundScope.launch { getSpaceParents(client).collect { rail = it } }
        runCurrent()
        rail shouldBe emptyList()
        everySuspend { roomApi.getState(parent) } returns
            Result.success(
                state.map {
                    ClientEvent.RoomEvent.StateEvent(
                        it.content,
                        EventId("$" + it.stateKey),
                        it.sender,
                        parent,
                        1L,
                        stateKey = it.stateKey,
                    )
                }
            )
        var latest = emptyList<SpaceParent>()
        backgroundScope.launch { getSpaceParents(client).collect { latest = it } }
        runCurrent()
        rail shouldBe emptyList()
        railSubscription.cancel()
        backgroundScope.launch { getSpaceParents(client).collect { rail = it } }
        runCurrent()
        rail shouldBe latest
        val item = latest.single().toBrowserItem(alice, emptyMap())
        item.roomId shouldBe parent
        item.userId shouldBe alice
        item.name shouldBe "Discovered"
        item.via shouldBe setOf("example.org")
        item.canJoin shouldBe false
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

    @Test
    fun `inaccessible unjoined parents are ignored`() = runTest {
        val service = mock<RoomService>()
        val client = mock<MatrixClient>()
        val api = mock<MatrixClientServerApiClient>()
        val roomApi = mock<RoomApiClient>()
        every { client.di } returns koinApplication { modules(module { single { service } }) }.koin
        every { client.api } returns api
        every { api.room } returns roomApi
        every { service.getAll() } returns flowOf(mapOf(child to flowOf(Room(child))))
        every { service.getAllState(child, ParentEventContent::class) } returns
            flowOf(mapOf(parent.full to flowOf(parentEvent)))
        every { service.getById(parent) } returns flowOf(null)
        everySuspend { roomApi.getState(parent) } returns Result.failure(IllegalStateException("forbidden"))
        var latest = emptyList<SpaceParent>()
        backgroundScope.launch { getSpaceParents(client).collect { latest = it } }
        runCurrent()
        latest shouldBe emptyList()
    }
}
