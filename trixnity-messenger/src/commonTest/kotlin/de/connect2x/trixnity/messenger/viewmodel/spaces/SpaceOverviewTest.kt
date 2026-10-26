package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceOverviewTest {
    private val account = UserId("alice", "example.org")

    private fun space(name: String) = SpaceBrowserItem(account, RoomId("!$name:example.org"), name, true, true)

    private val parent = space("parent")
    private val child = space("child")

    @Test
    fun `overview shows invitations before joined spaces`() = runTest {
        val fixture = SpaceBrowserFixture(account, parent.roomId)
        val invitation = space("zzz").copy(joined = false, invited = true, canJoin = true)
        fixture.content.value = SpaceContent(items = listOf(child, invitation))
        val context = fixture.context(this)
        var overview = SpaceContent()
        val subscription = backgroundScope.launch { context.getSpaceOverview(account).collect { overview = it } }
        advanceTimeBy(1_000)
        runCurrent()
        overview.items.first().roomId shouldBe invitation.roomId
        overview.items.count { it.invited } shouldBe 1
        subscription.cancel()
    }
}
