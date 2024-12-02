package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MemberStateTimelineElementViewModelTest : ShouldSpec() {

    val roomId = RoomId("room", "server")
    val sender = UserId("user", "server")
    val eventId = EventId("event")
    val matrixClientMock = mock<MatrixClient>()
    val roomServiceMock = mock<RoomService>()
    val userServiceMock = mock<UserService>()

    val isDirect = MutableStateFlow(false)
    val senderName = MutableStateFlow("Sender")

    init {
        beforeTest {
            coroutineTestScope = true
            resetCalls(matrixClientMock, roomServiceMock, userServiceMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { userServiceMock }
                    }
                )
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

        should("show an indicator for name changes") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    displayName = "I have changed!",
                    previousMemberEventContent = memberEventContent(displayName = "I am the original"),
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "'I am the original' has changed their name to 'I have changed!'"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for avatar image changes") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    avatarUrl = "mxc://localhost/new_url",
                    previousMemberEventContent = memberEventContent(avatarUrl = "mxc://localhost/boring_old_url"),
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Bob has changed the avatar image"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for user joining a room") {

            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = Membership.JOIN,
                    previousMemberEventContent = null,
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Bob has joined the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for user leaving a room") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = Membership.LEAVE,
                    previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Bob has left the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for user being banned from a room") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = Membership.BAN,
                    previousMemberEventContent = memberEventContent(membership = Membership.JOIN),
                    stateKey = "@mallory:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Mallory has been removed by User1 from the group"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for an invitation of a user to the room") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = Membership.INVITE,
                    previousMemberEventContent = null,
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Bob has been invited by User1"
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("show an indicator for a user knocking at the room") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = Membership.KNOCK,
                    previousMemberEventContent = null,
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Bob wants to join the group"
            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("update indicator on username changes") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = Membership.INVITE,
                    previousMemberEventContent = null,
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()
            cut.changeMessage.value shouldBe "Bob has been invited by Sender"

            senderName.value = "Sender2"
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Bob has been invited by Sender2"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("update indicator on room changing 'direct' state") {
            val cut = memberStatusViewModel(
                mockTimelineEvent(
                    membership = Membership.JOIN,
                    previousMemberEventContent = null,
                    stateKey = "@bob:localhost",
                ), coroutineContext = coroutineContext
            )
            val subscriberJob = launch { cut.changeMessage.collect {} }
            advanceUntilIdle()
            cut.changeMessage.value shouldBe "Bob has joined the group"

            isDirect.value = true
            advanceUntilIdle()

            cut.changeMessage.value shouldBe "Bob has joined the chat"

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private fun memberStatusViewModel(
        timelineEvent: TimelineEvent,
        coroutineContext: CoroutineContext,
    ): MemberStateTimelineElementViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock))
            )
        }.koin
        return MemberStateTimelineElementViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = di,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext
            ),
            content = timelineEvent.content as MemberEventContent,
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
    ): TimelineEvent {
        val timelineEvent = TimelineEvent(
            event = ClientEvent.RoomEvent.StateEvent(
                content = MemberEventContent(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    membership = membership,
                    isDirect = isDirect,
                ),
                id = EventId(""),
                sender = UserId(""),
                roomId = RoomId(""),
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
