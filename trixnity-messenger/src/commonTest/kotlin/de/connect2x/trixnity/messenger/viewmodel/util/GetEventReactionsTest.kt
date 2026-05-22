package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.TimelineEventRelation
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.RedactedEventContent
import de.connect2x.trixnity.core.model.events.m.ReactionEventContent
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.RelationType
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.testGraphemeIterableProvider
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class GetEventReactionsTest {

    private val user1 = UserId("user1", "localhost")
    private val user2 = UserId("user2", "localhost")
    private val user3 = UserId("user3", "localhost")
    private val roomId = RoomId("!roomId")
    private val eventId = EventId("1")
    private val annotation1 = EventId("annotation1")
    private val annotation2 = EventId("annotation2")
    private val annotation3 = EventId("annotation3")
    private val reaction1 = EventId("reaction1")
    private val reaction2 = EventId("reaction2")
    private val reaction3 = EventId("reaction3")

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()
    private val userServiceMock = mock<UserService>()

    @BeforeTest
    fun setup() {
        configureTestLogging()
        resetMocks(matrixClientMock, roomServiceMock, userServiceMock)

        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }
                .koin

        every { matrixClientMock.userId } returns user1
        every { userServiceMock.getById(roomId, user1) } returns
            MutableStateFlow(RoomUser(roomId, user1, "user 1", memberEvent()))
        every { userServiceMock.getById(roomId, user2) } returns
            MutableStateFlow(RoomUser(roomId, user2, "user 2", memberEvent()))
        every { userServiceMock.getById(roomId, user3) } returns
            MutableStateFlow(RoomUser(roomId, user3, "user 3", memberEvent()))
    }

    @Test
    fun `should return no reactions if there are none`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns MutableStateFlow(mapOf())
        getEventReactions() shouldBe EventReactions.Empty
    }

    @Test
    fun `should return no reactions if event is redacted`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RedactedEventContent("m.room.message")))
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns MutableStateFlow(mapOf())
        getEventReactions() shouldBe EventReactions.Empty
    }

    @Test
    fun `should return a single reaction for 1 user`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getTimelineEvent(any(), reaction1) } returns
            MutableStateFlow(reactionEvent(user2, reaction1, "🎉"))
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
            MutableStateFlow(
                mapOf(annotation1 to MutableStateFlow(timelineEventRelation(this@GetEventReactionsTest.reaction1)))
            )
        getEventReactions() shouldBe
            EventReactions(
                all =
                    setOf(
                        EventReaction(
                            value = "🎉",
                            sender = UserInfoElement(userId = user2, name = "user 2", initials = "U2"),
                            eventId = reaction1,
                            isByMe = false,
                        )
                    )
            )
    }

    @Test
    fun `should return multiple reactions for multiple users`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getTimelineEvent(any(), reaction1) } returns
            MutableStateFlow(reactionEvent(user2, reaction1, "🎉"))
        every { roomServiceMock.getTimelineEvent(any(), reaction2) } returns
            MutableStateFlow(reactionEvent(user2, reaction2, "🙈"))
        every { roomServiceMock.getTimelineEvent(any(), reaction3) } returns
            MutableStateFlow(reactionEvent(user3, reaction3, "🙈"))
        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
            MutableStateFlow(
                mapOf(
                    annotation1 to MutableStateFlow(timelineEventRelation(reaction1)),
                    annotation2 to MutableStateFlow(timelineEventRelation(reaction2)),
                    annotation3 to MutableStateFlow(timelineEventRelation(reaction3)),
                )
            )
        getEventReactions() shouldBe
            EventReactions(
                all =
                    setOf(
                        EventReaction(
                            value = "🎉",
                            sender = UserInfoElement(userId = user2, name = "user 2", initials = "U2"),
                            eventId = reaction1,
                            isByMe = false,
                        ),
                        EventReaction(
                            value = "🙈",
                            sender = UserInfoElement(userId = user2, name = "user 2", initials = "U2"),
                            eventId = reaction2,
                            isByMe = false,
                        ),
                        EventReaction(
                            value = "🙈",
                            sender = UserInfoElement(userId = user3, name = "user 3", initials = "U3"),
                            eventId = reaction3,
                            isByMe = false,
                        ),
                    )
            )
    }

    private suspend fun getEventReactions(): EventReactions =
        GetEventReactionsImpl()
            .invoke(
                matrixClientMock,
                roomId,
                eventId,
                InitialsImpl(testGraphemeIterableProvider()),
                avatarSize().toLong(),
            )
            .first()

    private fun reactionEvent(sender: UserId, eventId: EventId, reaction: String): TimelineEvent =
        timelineEvent(sender, eventId, ReactionEventContent(relatesTo = RelatesTo.Annotation(eventId, key = reaction)))

    private fun timelineEvent(sender: UserId, eventId: EventId, content: MessageEventContent): TimelineEvent =
        TimelineEvent(
            event =
                MessageEvent(content = content, id = eventId, sender = sender, roomId = roomId, originTimestamp = 0L),
            content = Result.success(content),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )

    private fun timelineEventRelation(eventId: EventId): TimelineEventRelation =
        TimelineEventRelation(roomId, eventId, relationType = RelationType.Annotation, relatedEventId = eventId)

    private fun memberEvent(): StateEvent<MemberEventContent> =
        StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = eventId,
            sender = user1,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = "",
        )
}
