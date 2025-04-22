package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.UnsignedRoomEventData
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

@Suppress("NonAsciiCharacters")
class MemberStateTimelineElementViewModelTest {

    val roomId = RoomId("room", "server")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

    init {
        resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        senderName.value = "Sender"
        every { userServiceMock.getById(roomId, sender) } returns senderName.map {
            RoomUser(
                roomId, sender, it, ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = eventId,
                    sender = sender,
                    roomId = roomId,
                    originTimestamp = 0L,
                    stateKey = "",
                )
            )
        }
        isDirect.value = false
        every { roomServiceMock.getById(roomId) } returns isDirect.map { Room(roomId, isDirect = it) }

        every {
            userServiceMock.getById(any(), eq(UserId("bob", "localhost")))
        } returns MutableStateFlow(
            RoomUser(
                roomId = RoomId("room1", "localhost"),
                userId = UserId("bob", "localhost"),
                name = "Bob",
                event = ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId(""),
                    sender = UserId(""),
                    roomId = RoomId(""),
                    originTimestamp = 0L,
                    stateKey = "",
                ),
            )
        )
        every {
            userServiceMock.getById(any(), eq(UserId("mallory", "localhost")))
        } returns MutableStateFlow(
            RoomUser(
                roomId = RoomId("room1", "localhost"),
                userId = UserId("mallory", "localhost"),
                name = "Mallory",
                event = ClientEvent.RoomEvent.StateEvent(
                    content = MemberEventContent(membership = Membership.JOIN),
                    id = EventId(""),
                    sender = UserId(""),
                    roomId = RoomId(""),
                    originTimestamp = 0L,
                    stateKey = "",
                ),
            )
        )
    }

    @Test
    fun `name change » should show an indicator for name changes`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                displayName = "I have changed!",
                previousMemberEventContent = memberEventContent(displayName = "I am the original"),
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "'I am the original' has changed their name to 'I have changed!'"
        }
    }

    @Test
    fun `avatar change » should show an indicator for avatar image changes`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                avatarUrl = "mxc://localhost/new_url",
                previousMemberEventContent = memberEventContent(avatarUrl = "mxc://localhost/boring_old_url"),
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }

        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Sender has changed the avatar image"
        }
    }

    @Test
    fun `joining user » should show an indicator for user joining a room`() = runTest {

        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has joined the group"
        }
    }

    @Test
    fun `leaving user » should show an indicator for user leaving a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.LEAVE,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has left the group"
        }
    }

    @Test
    fun `banned user » should show an indicator for user being banned from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.BAN,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@mallory:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Mallory has been removed by Sender from the group"
        }
    }

    @Test
    fun `banned user » should show an indicator with reason for user being banned from a room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.BAN,
                previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                stateKey = "@mallory:localhost",
                reason = "he spammed our chat :("
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Mallory has been removed by Sender from the group because \"he spammed our chat :(\""
        }
    }

    @Test
    fun `invited user » should show an indicator for an invitation of a user to the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.INVITE,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has been invited by Sender"
        }
    }

    @Test
    fun `invited user » should show an indicator with reason for an invitation of a user to the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.INVITE,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
                reason = "I want him to play Stardew Valley with us"
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has been invited by Sender because \"I want him to play Stardew Valley with us\""
        }
    }

    @Test
    fun `knocking user » should show an indicator for a user knocking at the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.KNOCK,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob requested to join the group. Check the room settings to manage the Request"
        }
    }

    @Test
    fun `knocking user » should show an indicator with the reason for a user knocking at the room`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.KNOCK,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
                reason = "he also likes treecake"
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob requested to join the group because \"he also likes treecake\". Check the room settings to manage the Request"
        }
    }

    @Test
    fun `changed name » should update indicator on username changes`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.INVITE,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has been invited by Sender"
        }

        senderName.value = "Sender2"
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has been invited by Sender2"
        }
    }

    @Test
    fun `changed room » update indicator on room changing 'direct' state`() = runTest {
        val cut = memberStatusViewModel(
            mockTimelineEvent(
                membership = Membership.JOIN,
                previousMemberEventContent = null,
                stateKey = "@bob:localhost",
            ),
        )
        backgroundScope.launch { cut.changeMessage.collect {} }
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has joined the group"
        }

        isDirect.value = true
        eventually(100.milliseconds) {
            cut.changeMessage.value shouldBe "Bob has joined the chat"
        }
    }

    private fun TestScope.memberStatusViewModel(
        timelineEvent: TimelineEvent,
    ): MemberStateTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(UserId("test", "server") to matrixClientMock)
                )
            )
        }.koin
        return MemberStateTimelineElementViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = UserId("test", "server"),
            ),
            content = timelineEvent.event.content as MemberEventContent,
            roomId = roomId,
            eventId = eventId,
        )
    }

    private fun memberEventContent(
        avatarUrl: String = "",
        displayName: String = "Bob",
        membership: Membership = Membership.JOIN,
        isDirect: Boolean = false,
    ) = MemberEventContent(
        avatarUrl = avatarUrl,
        displayName = displayName,
        membership = membership,
        isDirect = isDirect,
    )

    private fun mockTimelineEvent(
        avatarUrl: String = "",
        displayName: String = "Bob",
        membership: Membership = Membership.JOIN,
        isDirect: Boolean = false,
        stateKey: String = "",
        previousMemberEventContent: MemberEventContent? = null,
        reason: String? = null
    ): TimelineEvent {
        val timelineEvent = TimelineEvent(
            event = ClientEvent.RoomEvent.StateEvent(
                content = MemberEventContent(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    membership = membership,
                    isDirect = isDirect,
                    reason = reason,
                ),
                id = eventId,
                sender = sender,
                roomId = roomId,
                originTimestamp = 0L,
                unsigned = UnsignedRoomEventData.UnsignedStateEventData(
                    previousContent = previousMemberEventContent,
                ),
                stateKey = stateKey,
            ),
            content = null,
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
        every { roomServiceMock.getTimelineEvent(roomId, eventId) } returns flowOf(timelineEvent)
        return timelineEvent
    }
}

