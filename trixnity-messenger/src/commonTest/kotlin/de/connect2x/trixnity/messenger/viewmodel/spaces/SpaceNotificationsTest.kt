package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.notification.NotificationService
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.StrippedStateEvent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceNotificationsTest {
    @Test
    fun `counts only linked joined rooms and updates after reads and unlinking`() =
        checkChildNotifications(joined = true)

    @Test
    fun `counts linked invited rooms and updates after reads and unlinking`() = checkChildNotifications(joined = false)

    private fun checkChildNotifications(joined: Boolean) = runTest {
        val account = UserId("@alice:example.org")
        val parentId = RoomId("!parent:example.org")
        val nestedSpaceId = RoomId("!nested:example.org")
        val directRoomId = RoomId("!direct:example.org")
        val nestedRoomId = RoomId("!nested-room:example.org")
        val fixture = SpaceBrowserFixture(account, parentId)
        fixture.content.value =
            SpaceContent(
                items =
                    listOf(
                        SpaceBrowserItem(account, nestedSpaceId, "Nested", true, true),
                        SpaceBrowserItem(account, directRoomId, "Direct", false, joined, invited = !joined),
                        SpaceBrowserItem(account, nestedRoomId, "Nested room", false, true),
                    )
            )
        fun child(roomId: RoomId) =
            flowOf(
                StrippedStateEvent(
                    ChildEventContent(via = setOf(account.domain)),
                    sender = account,
                    stateKey = roomId.full,
                )
            )
        val directLinks =
            MutableStateFlow(
                mapOf(directRoomId.full to child(directRoomId), nestedSpaceId.full to child(nestedSpaceId))
            )
        every { fixture.rooms.getAllState(parentId, ChildEventContent::class) } returns directLinks
        every { fixture.rooms.getAllState(nestedSpaceId, ChildEventContent::class) } returns
            flowOf(mapOf(nestedRoomId.full to child(nestedRoomId)))
        val directCount = MutableStateFlow(3)
        val nestedCount = MutableStateFlow(8)
        val notifications = mock<NotificationService>()
        every { notifications.getCount(any()) } calls
            { args ->
                when (args.arg<RoomId>(0)) {
                    directRoomId -> directCount
                    nestedRoomId -> nestedCount
                    else -> flowOf(0)
                }
            }
        val clientDi = koinApplication {
            modules(
                module {
                    single { fixture.rooms }
                    single { notifications }
                }
            )
        }
        every { fixture.client.di } returns clientDi.koin
        try {
            val context = fixture.context(this)
            var overview = SpaceContent()
            val subscription = backgroundScope.launch { context.getSpaceOverview(account).collect { overview = it } }
            advanceTimeBy(1000)
            runCurrent()
            assertEquals("3", overview.items.single().notificationCount)
            nestedCount.value = 15
            runCurrent()
            assertEquals("3", overview.items.single().notificationCount)
            directCount.value = 0
            runCurrent()
            assertEquals(null, overview.items.single().notificationCount)
            directCount.value = 120
            runCurrent()
            assertEquals("99+", overview.items.single().notificationCount)
            directLinks.value = mapOf(nestedSpaceId.full to child(nestedSpaceId))
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(null, overview.items.single().notificationCount)
            subscription.cancel()
        } finally {
            clientDi.close()
        }
    }
}
