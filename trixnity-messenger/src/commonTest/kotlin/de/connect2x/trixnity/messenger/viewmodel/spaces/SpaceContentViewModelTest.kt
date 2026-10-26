package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.settings.set
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceContentViewModelTest {
    private val alice = UserId("alice", "example.org")
    private val bob = UserId("bob", "example.org")
    private var item = SpaceBrowserItem(alice, RoomId("!space:example.org"), "Space", isSpace = true, joined = true)
    private val settings = createTestMatrixMessengerSettingsHolder()
    private val selected = mutableListOf<SpaceBrowserItem>()
    private var activeCollectors = 0
    private var fail = false
    private val aliceFixture = SpaceBrowserFixture(alice, RoomId("!parent:example.org"))
    private val bobFixture = SpaceBrowserFixture(bob, RoomId("!parent:example.org"))
    private val clients = MutableStateFlow(mapOf(alice to aliceFixture.client, bob to bobFixture.client))
    private val roomName =
        mock<RoomName>().also { names ->
            every { names.getRoomName(any<RoomId>(), any(), false) } calls
                {
                    flow {
                        activeCollectors++
                        try {
                            if (fail) error("offline")
                            emit(item.name)
                            awaitCancellation()
                        } finally {
                            activeCollectors--
                        }
                    }
                }
        }

    private fun TestScope.viewModel(
        userId: UserId? = null,
        spaceId: RoomId? = null,
        loadHierarchy: Boolean = true,
        observe: Boolean = true,
    ): SpaceContentViewModel {
        aliceFixture.content.value = SpaceContent(items = listOf(item))
        bobFixture.content.value = SpaceContent(items = listOf(item.copy(userId = bob)))
        val context = testSpaceBrowserContext(clients, roomName)
        context.getKoin().declare<MatrixMessengerSettingsHolder>(settings)
        return SpaceContentViewModelImpl(context, userId, spaceId, { selected += it }, {}, loadHierarchy).also { vm ->
            if (observe) backgroundScope.launch { vm.state.collect() }
        }
    }

    @Test
    fun `account colors follow settings and disappear with only one account`() = runTest {
        settings.create(alice, MatrixMessengerAccountSettingsBase(displayColor = 0xFF112233))
        val vm = viewModel()
        runCurrent()
        fun colors() =
            (vm.state.value as SpaceContentState.Ready).content.items.associate { it.userId to it.accountColor }
        colors().values.all { it == null } shouldBe true

        settings.create(bob, MatrixMessengerAccountSettingsBase(displayColor = 0xFF445566))
        runCurrent()
        colors() shouldBe mapOf(alice to 0xFF112233L, bob to 0xFF445566L)

        settings.update(alice) { set(MatrixMessengerAccountSettingsBase(displayColor = 0xFF778899)) }
        runCurrent()
        colors()[alice] shouldBe 0xFF778899L

        settings.delete(bob)
        runCurrent()
        colors().values.all { it == null } shouldBe true
    }

    @Test
    fun `removing the route account stops its discovery subscriptions`() = runTest {
        val vm = viewModel(alice, aliceFixture.spaceId)
        runCurrent()
        (activeCollectors > 0) shouldBe true
        (aliceFixture.parentCollectors > 0) shouldBe true
        clients.value = mapOf(bob to bobFixture.client)
        runCurrent()
        vm.state.value shouldBe SpaceContentState.Error
        activeCollectors shouldBe 0
        aliceFixture.parentCollectors shouldBe 0
        bobFixture.parentCollectors shouldBe 0
        clients.value = mapOf(alice to aliceFixture.client, bob to bobFixture.client)
        vm.refresh()
        runCurrent()
        (vm.state.value is SpaceContentState.Ready) shouldBe true
        (activeCollectors > 0) shouldBe true
        (aliceFixture.parentCollectors > 0) shouldBe true
    }

    @Test
    fun `hidden browsers release discovery and resume when shown`() = runTest {
        val vm = viewModel(observe = false)
        runCurrent()
        activeCollectors shouldBe 0
        aliceFixture.parentCollectors shouldBe 0
        bobFixture.parentCollectors shouldBe 0
        val visible = backgroundScope.launch { vm.state.collect() }
        runCurrent()
        (activeCollectors > 0) shouldBe true
        (aliceFixture.parentCollectors > 0) shouldBe true
        visible.cancel()
        runCurrent()
        activeCollectors shouldBe 0
        aliceFixture.parentCollectors shouldBe 0
        bobFixture.parentCollectors shouldBe 0
        backgroundScope.launch { vm.state.collect() }
        runCurrent()
        (activeCollectors > 0) shouldBe true
        (aliceFixture.parentCollectors > 0) shouldBe true
    }

    @Test
    fun `invitation selection preserves membership and uses updated state after joining`() = runTest {
        item = item.copy(joined = false, invited = true, canJoin = true)
        val vm = viewModel()
        runCurrent()
        val invitation = (vm.state.value as SpaceContentState.Ready).content.items.first()
        vm.select(invitation)
        selected.single().invited shouldBe true
        item = item.copy(joined = true, invited = false)
        aliceFixture.content.value = SpaceContent(items = listOf(item))
        runCurrent()
        vm.select(invitation)
        selected.last().joined shouldBe true
    }

    @Test
    fun `selection uses the latest item after its name or avatar changes`() = runTest {
        val vm = viewModel(alice, aliceFixture.spaceId)
        runCurrent()
        val displayed = item
        item = item.copy(name = "Renamed", avatarUrl = "mxc://example.org/new")
        aliceFixture.content.value = SpaceContent(items = listOf(item))
        vm.refresh()
        runCurrent()
        vm.select(displayed)
        selected.single().name shouldBe item.name
        selected.single().avatarUrl shouldBe item.avatarUrl
    }

    @Test
    fun `selection rejects a formerly joined item after membership changes`() = runTest {
        val vm = viewModel(alice, aliceFixture.spaceId)
        runCurrent()
        val displayed = item
        item = item.copy(joined = false)
        aliceFixture.content.value = SpaceContent(items = listOf(item))
        vm.refresh()
        runCurrent()
        vm.select(displayed)
        selected shouldBe emptyList()
    }

    @Test
    fun `sidebar collects local content and refresh stays local`() = runTest {
        val vm = viewModel(alice, aliceFixture.spaceId, loadHierarchy = false)
        runCurrent()
        aliceFixture.hierarchyRequests shouldBe 0
        (vm.state.value as SpaceContentState.Ready).content.loadingHierarchy shouldBe false
        aliceFixture.content.value = SpaceContent(items = listOf(item))
        vm.refresh()
        runCurrent()
        aliceFixture.hierarchyRequests shouldBe 0
    }

    @Test
    fun `changing accounts replaces the list and prevents selecting stale items`() = runTest {
        val vm = viewModel()
        runCurrent()
        settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = bob) }
        runCurrent()
        vm.select(item)
        selected shouldBe emptyList()
        val current = (vm.state.value as SpaceContentState.Ready).content.items.first()
        vm.select(current)
        selected.single().userId shouldBe bob
    }

    @Test
    fun `discovery overview keeps the account from its route`() = runTest {
        val vm = viewModel(alice)
        runCurrent()
        settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = bob) }
        runCurrent()
        vm.select((vm.state.value as SpaceContentState.Ready).content.items.first())
        selected.single().userId shouldBe alice
    }

    @Test
    fun `details keep their route account when selected account changes`() = runTest {
        val vm = viewModel(alice, aliceFixture.spaceId)
        runCurrent()
        settings.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = bob) }
        runCurrent()
        vm.select((vm.state.value as SpaceContentState.Ready).content.items.first())
        selected.single().userId shouldBe alice
    }

    @Test
    fun `failed load can be retried`() = runTest {
        fail = true
        val vm = viewModel()
        runCurrent()
        vm.state.value shouldBe SpaceContentState.Error
        fail = false
        aliceFixture.content.value = SpaceContent(items = listOf(item))
        vm.refresh()
        runCurrent()
        (vm.state.value as SpaceContentState.Ready).content.items.any { it.roomId == item.roomId } shouldBe true
    }
}
