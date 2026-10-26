package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
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
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
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
        every { service.getAllState(parent, ChildEventContent::class) } returns flowOf(emptyMap())
        every { service.getAllState(parent, MemberEventContent::class) } returns flowOf(mapOf(bob.full to membership))
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
    fun `each browser retries parent lookup when its discovery restarts`() = runTest {
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
        val source = testSpaceBrowserContext(MutableStateFlow(mapOf(alice to client)))
        var rail = SpaceContent()
        val railSubscription = backgroundScope.launch { source.getSpaceOverview(alice).collect { rail = it } }
        runCurrent()
        rail.items shouldBe emptyList()
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
        var latest = SpaceContent()
        backgroundScope.launch { source.getSpaceOverview(alice).collect { latest = it } }
        runCurrent()
        rail.items shouldBe emptyList()
        railSubscription.cancel()
        backgroundScope.launch { source.getSpaceOverview(alice).collect { rail = it } }
        runCurrent()
        rail.items shouldBe latest.items
        val item = latest.items.single()
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
            every { service.getAllState(roomId, ChildEventContent::class) } returns flowOf(emptyMap())
            every { service.getAllState(roomId, MemberEventContent::class) } returns
                flowOf(mapOf(bob.full to flowOf(member)))
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
