package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceJoinTest {
    private val account = UserId("second", "example.org")
    private val item = SpaceBrowserItem(account, RoomId("!room:example.org"), "Room", false, false, canJoin = true)
    private val fixture = SpaceBrowserFixture(account, RoomId("!space:example.org"))
    private val content = fixture.content.also { it.value = SpaceContent(items = listOf(item)) }
    private val response = CompletableDeferred<Unit>()
    private val calls = mutableListOf<SpaceBrowserItem>()
    private val selected = mutableListOf<SpaceBrowserItem>()
    private var fail = false

    init {
        everySuspend { fixture.roomApi.joinRoom(any<RoomId>(), any(), any(), any()) } calls
            { args ->
                calls +=
                    (content.value.items.firstOrNull { it.roomId == args.arg<RoomId>(0) }
                        ?: item.copy(roomId = args.arg(0)))
                if (fail) error("forbidden")
                response.await()
                Result.success(args.arg<RoomId>(0))
            }
        everySuspend { fixture.roomApi.leaveRoom(any(), any()) } calls
            { args ->
                calls +=
                    (content.value.items.firstOrNull { it.roomId == args.arg<RoomId>(0) }
                        ?: item.copy(roomId = args.arg(0)))
                if (fail) error("forbidden")
                response.await()
                Result.success(Unit)
            }
    }

    private fun TestScope.viewModel(observe: Boolean = true): SpaceContentViewModel {
        return SpaceContentViewModelImpl(
                fixture.context(this),
                account,
                RoomId("!space:example.org"),
                { selected += it },
                {},
            )
            .also { vm -> if (observe) backgroundScope.launch { vm.state.collect() } }
    }

    private fun SpaceContentViewModel.item() = (state.value as SpaceContentState.Ready).content.items.single()

    @Test
    fun `explore can accept its own invitation without waiting for a child item`() = runTest {
        fixture.space.value = fixture.space.value!!.copy(membership = Membership.INVITE)
        val vm =
            SpaceContentViewModelImpl(
                fixture.context(this),
                account,
                fixture.spaceId,
                { selected += it },
                {},
                loadHierarchy = true,
            )
        backgroundScope.launch { vm.state.collect() }
        runCurrent()
        val invitation = vm.item()
        vm.join(invitation)
        runCurrent()
        selected shouldBe emptyList()
        response.complete(Unit)
        fixture.space.value = fixture.space.value!!.copy(membership = Membership.JOIN)
        runCurrent()
        advanceTimeBy(31_000)
        runCurrent()
        (vm.state.value as SpaceContentState.Ready).content.invited shouldBe false
        calls.size shouldBe 1
        selected shouldBe listOf(invitation.copy(joined = true, invited = false, canJoin = false))
    }

    @Test
    fun `explore closes after declining its own invitation`() = runTest {
        fixture.space.value = fixture.space.value!!.copy(membership = Membership.INVITE)
        var closed = false
        val vm =
            SpaceContentViewModelImpl(
                fixture.context(this),
                account,
                fixture.spaceId,
                {},
                { closed = true },
                loadHierarchy = true,
            )
        backgroundScope.launch { vm.state.collect() }
        runCurrent()
        vm.reject(vm.item())
        runCurrent()
        response.complete(Unit)
        fixture.space.value = fixture.space.value!!.copy(membership = Membership.LEAVE)
        runCurrent()
        closed shouldBe true
        calls.size shouldBe 1
    }

    @Test
    fun `membership completes after the browser stops being displayed`() = runTest {
        val vm = viewModel(observe = false)
        val visible = backgroundScope.launch { vm.state.collect() }
        runCurrent()
        vm.join(item)
        runCurrent()
        visible.cancel()
        runCurrent()
        content.value = SpaceContent(items = listOf(item.copy(joined = true)))
        response.complete(Unit)
        runCurrent()
        backgroundScope.launch { vm.state.collect() }
        runCurrent()
        vm.item().joined shouldBe true
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Idle
        calls.size shouldBe 1
    }

    @Test
    fun `reject invitation prevents duplicate join and supports retry on failure`() = runTest {
        val invited = item.copy(isSpace = true, invited = true)
        content.value = SpaceContent(items = listOf(invited))
        val vm = viewModel()
        runCurrent()
        fail = true
        vm.reject(invited)
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Failed
        fail = false
        vm.reject(invited)
        vm.reject(invited)
        vm.join(invited)
        runCurrent()
        calls.size shouldBe 2
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Rejecting
        content.value = SpaceContent()
        vm.refresh()
        response.complete(Unit)
        runCurrent()
        (vm.state.value as SpaceContentState.Ready).content.items shouldBe emptyList()
    }

    @Test
    fun `joining an invitation prevents a competing rejection`() = runTest {
        val invited = item.copy(isSpace = true, invited = true)
        content.value = SpaceContent(items = listOf(invited))
        val vm = viewModel()
        runCurrent()
        vm.join(invited)
        vm.reject(invited)
        vm.join(invited)
        runCurrent()
        calls.size shouldBe 1
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Joining
    }

    @Test
    fun `different rooms can join concurrently and complete independently`() = runTest {
        val other = item.copy(roomId = RoomId("!other:example.org"))
        content.value = SpaceContent(items = listOf(item, other))
        val vm = viewModel()
        runCurrent()
        vm.join(item)
        vm.join(other)
        vm.join(item)
        vm.join(other)
        runCurrent()
        calls.map { it.roomId }.toSet() shouldBe setOf(item.roomId, other.roomId)
        calls.size shouldBe 2
        fun states() =
            (vm.state.value as SpaceContentState.Ready).content.items.associate {
                it.roomId to it.membershipActionState
            }
        states() shouldBe
            mapOf(item.roomId to SpaceMembershipActionState.Joining, other.roomId to SpaceMembershipActionState.Joining)
        response.complete(Unit)
        content.value = SpaceContent(items = listOf(item.copy(joined = true), other))
        runCurrent()
        states() shouldBe
            mapOf(item.roomId to SpaceMembershipActionState.Idle, other.roomId to SpaceMembershipActionState.Joining)
        vm.join(other)
        runCurrent()
        calls.size shouldBe 2
        content.value = SpaceContent(items = listOf(item.copy(joined = true), other.copy(joined = true)))
        runCurrent()
        states() shouldBe
            mapOf(item.roomId to SpaceMembershipActionState.Idle, other.roomId to SpaceMembershipActionState.Idle)
    }

    @Test
    fun `joined and non invited rooms cannot be rejected`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.reject(item)
        runCurrent()
        calls shouldBe emptyList()
    }

    @Test
    fun `stalled join times out and can be retried`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.join(item)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Failed
        vm.join(vm.item())
        runCurrent()
        calls.size shouldBe 2
        response.complete(Unit)
        content.value = SpaceContent(items = listOf(item.copy(joined = true)))
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Idle
    }

    @Test
    fun `removed child does not leave a join pending forever`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.join(item)
        response.complete(Unit)
        content.value = SpaceContent()
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        content.value = SpaceContent(items = listOf(item))
        vm.refresh()
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Failed
        vm.join(vm.item())
        runCurrent()
        calls.size shouldBe 2
    }

    @Test
    fun `join prevents duplicate requests and waits for membership sync before navigation`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.join(item)
        vm.join(item)
        runCurrent()
        calls.size shouldBe 1
        calls.single().userId shouldBe account
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Joining
        response.complete(Unit)
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Joining
        vm.select(vm.item())
        selected shouldBe emptyList()
        content.value = SpaceContent(items = listOf(item.copy(joined = true, canJoin = false)))
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Idle
        vm.select(vm.item())
        selected.single().userId shouldBe account
        vm.join(vm.item())
        runCurrent()
        calls.size shouldBe 1
    }

    @Test
    fun `joinable spaces open only after membership sync`() = runTest {
        val space = item.copy(isSpace = true)
        content.value = SpaceContent(items = listOf(space))
        val vm = viewModel()
        runCurrent()
        vm.select(vm.item())
        selected shouldBe emptyList()
        vm.join(vm.item())
        runCurrent()
        calls.single().roomId shouldBe space.roomId
        response.complete(Unit)
        runCurrent()
        vm.select(vm.item())
        selected shouldBe emptyList()
        content.value = SpaceContent(items = listOf(space.copy(joined = true, canJoin = false)))
        runCurrent()
        vm.select(vm.item())
        selected.single().roomId shouldBe space.roomId
        selected.single().joined shouldBe true
    }

    @Test
    fun `failure is shown and a retry can succeed`() = runTest {
        fail = true
        val vm = viewModel()
        runCurrent()
        vm.join(item)
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Failed
        fail = false
        vm.join(vm.item())
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Joining
        response.complete(Unit)
        content.value = SpaceContent(items = listOf(item.copy(joined = true)))
        runCurrent()
        vm.item().joined shouldBe true
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Idle
        calls.size shouldBe 2
    }

    @Test
    fun `unsupported and stale rooms cannot be joined`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.join(item.copy(roomId = RoomId("!other:example.org")))
        content.value = SpaceContent(items = listOf(item.copy(canJoin = false)))
        vm.refresh()
        runCurrent()
        vm.join(item)
        runCurrent()
        calls shouldBe emptyList()
    }

    @Test
    fun `public subspace can be joined retried and opened after sync`() = runTest {
        val subspace = item.copy(isSpace = true)
        content.value = SpaceContent(items = listOf(subspace))
        fail = true
        val vm = viewModel()
        runCurrent()
        vm.join(subspace)
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Failed
        fail = false
        vm.join(vm.item())
        runCurrent()
        response.complete(Unit)
        runCurrent()
        vm.item().membershipActionState shouldBe SpaceMembershipActionState.Joining
        vm.select(vm.item())
        selected shouldBe emptyList()
        content.value = SpaceContent(items = listOf(subspace.copy(joined = true, canJoin = false)))
        runCurrent()
        vm.select(vm.item())
        selected.single().isSpace shouldBe true
        selected.single().userId shouldBe account
        calls.size shouldBe 2
    }
}
