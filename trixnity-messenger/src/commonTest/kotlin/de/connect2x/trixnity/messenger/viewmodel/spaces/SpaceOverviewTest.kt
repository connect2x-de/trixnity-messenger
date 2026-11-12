package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.StrippedStateEvent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import dev.mokkery.answering.returns
import dev.mokkery.every
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceOverviewTest {
    private val account = UserId("alice", "example.org")

    private fun space(name: String) = SpaceBrowserItem(account, RoomId("!$name:example.org"), name, true, true)

    private fun childLink(id: RoomId) =
        StrippedStateEvent(ChildEventContent(via = setOf(account.domain)), sender = account, stateKey = id.full)

    private val parent = space("parent")
    private val child = space("child")

    @Test
    fun `overview shows invitations before joined spaces`() = runTest {
        val fixture = SpaceBrowserFixture(account, parent.roomId)
        val invitation = space("zzz").copy(joined = false, invited = true, canJoin = true)
        fixture.content.value = SpaceContent(items = listOf(child, invitation))
        every { fixture.rooms.getAllState(parent.roomId, ChildEventContent::class) } returns flowOf(emptyMap())
        val context = fixture.context(this)
        var overview = SpaceContent()
        val subscription = backgroundScope.launch { context.getSpaceOverview(account).collect { overview = it } }
        advanceTimeBy(1_000)
        runCurrent()
        overview.items.first().roomId shouldBe invitation.roomId
        overview.items.count { it.invited } shouldBe 1
        subscription.cancel()
    }

    @Test
    fun `overview and lean rail hide nested spaces and react to unlinking`() = runTest {
        for (loadDetails in listOf(false, true)) {
            val fixture = SpaceBrowserFixture(account, parent.roomId)
            fixture.content.value = SpaceContent(items = listOf(child))
            val link = MutableStateFlow(childLink(child.roomId))
            every { fixture.rooms.getAllState(parent.roomId, ChildEventContent::class) } returns
                flowOf(mapOf(child.roomId.full to link))
            var overview = SpaceContent()
            val context = fixture.context(this)
            val subscription = backgroundScope.launch {
                context.getSpaceOverview(account, loadDetails = loadDetails).collect { overview = it }
            }
            advanceTimeBy(1_000)
            runCurrent()
            overview.items.map { it.roomId } shouldBe listOf(parent.roomId)
            link.value = link.value.copy(content = ChildEventContent(via = emptySet()))
            runCurrent()
            overview.items.map { it.roomId }.toSet() shouldBe setOf(parent.roomId, child.roomId)
            subscription.cancel()
        }
    }

    @Test
    fun `overview and lean rail keep loops accessible and update when broken`() = runTest {
        for (loadDetails in listOf(false, true)) {
            val fixture = SpaceBrowserFixture(account, parent.roomId)
            fixture.content.value = SpaceContent(items = listOf(child))
            val link = MutableStateFlow(childLink(parent.roomId))
            every { fixture.rooms.getAllState(child.roomId, ChildEventContent::class) } returns
                flowOf(mapOf(parent.roomId.full to link))
            var overview = SpaceContent()
            val context = fixture.context(this)
            val subscription = backgroundScope.launch {
                context.getSpaceOverview(account, loadDetails = loadDetails).collect { overview = it }
            }
            advanceTimeBy(1_000)
            runCurrent()
            overview.items.map { it.roomId }.toSet() shouldBe setOf(parent.roomId, child.roomId)
            overview.items.all { it.isInLoop } shouldBe true
            link.value = link.value.copy(content = ChildEventContent(via = emptySet()))
            runCurrent()
            overview.items.map { it.roomId } shouldBe listOf(parent.roomId)
            overview.items.none { it.isInLoop } shouldBe true
            subscription.cancel()
        }
    }

    @Test
    fun `overview counts follow valid links and room types used for top level filtering`() = runTest {
        val fixture = SpaceBrowserFixture(account, parent.roomId)
        val room = space("room").copy(isSpace = false)
        val unknown = RoomId("!unknown:example.org")
        fixture.content.value = SpaceContent(items = listOf(child, room))
        val links =
            MutableStateFlow(
                listOf(child.roomId, room.roomId, unknown).associate { id -> id.full to flowOf(childLink(id)) } +
                    ("invalid" to flowOf(childLink(child.roomId).copy(stateKey = "invalid")))
            )
        every { fixture.rooms.getAllState(parent.roomId, ChildEventContent::class) } returns links
        val context = fixture.context(this)
        var overview = SpaceContent()
        backgroundScope.launch { context.getSpaceOverview(account).collect { overview = it } }
        advanceTimeBy(1_000)
        runCurrent()
        fun counts(): List<Int?> =
            overview.items
                .first { it.roomId == parent.roomId }
                .let { listOf(it.childCount, it.subspaceCount, it.unknownChildCount) }
        overview.items.map { it.roomId } shouldBe listOf(parent.roomId)
        counts() shouldBe listOf(3, 1, 1)
        fixture.content.value = SpaceContent(items = listOf(child, room.copy(isSpace = true)))
        runCurrent()
        counts() shouldBe listOf(3, 2, 1)
        links.value = links.value - child.roomId.full
        advanceTimeBy(1_000)
        runCurrent()
        overview.items.map { it.roomId }.toSet() shouldBe setOf(parent.roomId, child.roomId)
        counts() shouldBe listOf(2, 1, 1)
    }
}
