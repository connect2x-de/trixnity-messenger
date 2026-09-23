package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.RoomOutboxMessage
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
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.util.testGraphemeIterableProvider
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId.Companion.EventIdOrTransactionId
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
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

    private val i18n =
        object :
            I18n(
                DefaultLanguages,
                createTestMatrixMessengerSettingsHolder(),
                GetSystemLang { "en" },
                TimeZone.of("CET"),
            ) {}

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
        every { roomServiceMock.getOutbox(any()) } returns MutableStateFlow(listOf())
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
                            eventOrTransactionId = EventIdOrTransactionId(reaction1),
                            status = ReactionStatus.NotByMe,
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
                            eventOrTransactionId = EventIdOrTransactionId(reaction1),
                            status = ReactionStatus.NotByMe,
                        ),
                        EventReaction(
                            value = "🙈",
                            sender = UserInfoElement(userId = user2, name = "user 2", initials = "U2"),
                            eventOrTransactionId = EventIdOrTransactionId(reaction2),
                            status = ReactionStatus.NotByMe,
                        ),
                        EventReaction(
                            value = "🙈",
                            sender = UserInfoElement(userId = user3, name = "user 3", initials = "U3"),
                            eventOrTransactionId = EventIdOrTransactionId(reaction3),
                            status = ReactionStatus.NotByMe,
                        ),
                    )
            )
    }

    @Test
    fun `should return reactions in outbox`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getOutbox(any()) } returns
            MutableStateFlow(
                listOf(
                    MutableStateFlow(
                        RoomOutboxMessage(
                            roomId = roomId,
                            transactionId = "123",
                            content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                            createdAt = Instant.fromEpochSeconds(123, 0),
                            sentAt = null,
                            eventId = null,
                            sendError = null,
                        )
                    )
                )
            )

        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns MutableStateFlow(emptyMap())
        getEventReactions() shouldBe
            EventReactions(
                all =
                    setOf(
                        EventReaction(
                            value = "🎉",
                            sender = UserInfoElement(userId = user1, name = "user 1", initials = "U1"),
                            eventOrTransactionId = EventIdOrTransactionId("123"),
                            status = ReactionStatus.Pending,
                        )
                    )
            )
    }

    @Test
    fun `should return newest reactions in outbox per key`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getOutbox(any()) } returns
            MutableStateFlow(
                listOf(
                    MutableStateFlow(
                        RoomOutboxMessage(
                            roomId = roomId,
                            transactionId = "1",
                            content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                            createdAt = Instant.fromEpochSeconds(1, 0),
                            sentAt = null,
                            eventId = null,
                            sendError = null,
                        )
                    ),
                    MutableStateFlow(
                        RoomOutboxMessage(
                            roomId = roomId,
                            transactionId = "2",
                            content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                            createdAt = Instant.fromEpochSeconds(2, 0),
                            sentAt = null,
                            eventId = null,
                            sendError = null,
                        )
                    ),
                    MutableStateFlow(
                        RoomOutboxMessage(
                            roomId = roomId,
                            transactionId = "3",
                            content = ReactionEventContent(RelatesTo.Annotation(eventId, "👺")),
                            createdAt = Instant.fromEpochSeconds(2, 0),
                            sentAt = null,
                            eventId = null,
                            sendError = null,
                        )
                    ),
                )
            )

        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns MutableStateFlow(emptyMap())
        getEventReactions() shouldBe
            EventReactions(
                all =
                    setOf(
                        EventReaction(
                            value = "🎉",
                            sender = UserInfoElement(userId = user1, name = "user 1", initials = "U1"),
                            eventOrTransactionId = EventIdOrTransactionId("2"),
                            status = ReactionStatus.Pending,
                        ),
                        EventReaction(
                            value = "👺",
                            sender = UserInfoElement(userId = user1, name = "user 1", initials = "U1"),
                            eventOrTransactionId = EventIdOrTransactionId("3"),
                            status = ReactionStatus.Pending,
                        ),
                    )
            )
    }

    @Test
    fun `should return reactions in outbox with send error with send error status`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getOutbox(any()) } returns
            MutableStateFlow(
                listOf(
                    MutableStateFlow(
                        RoomOutboxMessage(
                            roomId = roomId,
                            transactionId = "123",
                            content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                            createdAt = Instant.fromEpochSeconds(123, 0),
                            sentAt = null,
                            eventId = null,
                            sendError = RoomOutboxMessage.SendError.NoEventPermission,
                        )
                    )
                )
            )

        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns MutableStateFlow(emptyMap())
        getEventReactions() shouldBe
            EventReactions(
                all =
                    setOf(
                        EventReaction(
                            value = "🎉",
                            sender = UserInfoElement(userId = user1, name = "user 1", initials = "U1"),
                            eventOrTransactionId = EventIdOrTransactionId("123"),
                            status = ReactionStatus.SentError(i18n.sendErrorEventPermission()),
                        )
                    )
            )
    }

    @Test
    fun `should only return reactions from outbox and not also from timeline`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getTimelineEvent(any(), reaction1) } returns
            MutableStateFlow(
                timelineEvent(
                    user1,
                    reaction1,
                    ReactionEventContent(relatesTo = RelatesTo.Annotation(eventId, key = "🎉")),
                )
            )

        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
            MutableStateFlow(mapOf(reaction1 to MutableStateFlow(timelineEventRelation(reaction1))))

        every { roomServiceMock.getOutbox(any()) } returns
            MutableStateFlow(
                listOf(
                    MutableStateFlow(
                        RoomOutboxMessage(
                            roomId = roomId,
                            transactionId = "123",
                            content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                            createdAt = Instant.fromEpochSeconds(123, 0),
                            sentAt = null,
                            eventId = null,
                            sendError = null,
                        )
                    )
                )
            )

        getEventReactions() shouldBe
            EventReactions(
                all =
                    setOf(
                        EventReaction(
                            value = "🎉",
                            sender = UserInfoElement(userId = user1, name = "user 1", initials = "U1"),
                            eventOrTransactionId = EventIdOrTransactionId("123"),
                            status = ReactionStatus.Pending,
                        )
                    )
            )
    }

    @Test
    fun `should return reactions from timeline with redaction pending status if a redaction is in the outbox`() =
        runTest {
            every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
                MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
            every { roomServiceMock.getTimelineEvent(any(), reaction1) } returns
                MutableStateFlow(
                    timelineEvent(
                        user1,
                        reaction1,
                        ReactionEventContent(relatesTo = RelatesTo.Annotation(eventId, key = "🎉")),
                    )
                )

            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
                MutableStateFlow(mapOf(reaction1 to MutableStateFlow(timelineEventRelation(reaction1))))

            every { roomServiceMock.getOutbox(any()) } returns
                MutableStateFlow(
                    listOf(
                        MutableStateFlow(
                            RoomOutboxMessage(
                                roomId = roomId,
                                transactionId = "123",
                                content = RedactionEventContent(reaction1),
                                createdAt = Instant.fromEpochSeconds(123, 0),
                                sentAt = null,
                                eventId = null,
                                sendError = null,
                            )
                        )
                    )
                )

            val allReactions = getEventReactions().all
            allReactions shouldHaveSize 1
            allReactions.first().status shouldBe ReactionStatus.RedactionPending
        }

    @Test
    fun `should not return reactions from outbox if they have been redacted via the server`() = runTest {
        every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
            MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
        every { roomServiceMock.getOutbox(any()) } returns
            MutableStateFlow(
                listOf(
                    MutableStateFlow(
                        RoomOutboxMessage(
                            roomId = roomId,
                            transactionId = "123",
                            content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                            createdAt = Instant.fromEpochSeconds(123, 0),
                            sentAt = Instant.fromEpochSeconds(123, 0),
                            eventId = reaction1,
                            sendError = null,
                        )
                    )
                )
            )
        every { roomServiceMock.getTimelineEvent(roomId, reaction1) } returns
            MutableStateFlow(
                timelineEvent(content = RedactedEventContent("m.annotation"), sender = user1, eventId = reaction1)
            )

        every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
            MutableStateFlow(mapOf(reaction1 to MutableStateFlow(timelineEventRelation(reaction1))))
        getEventReactions() shouldBe EventReactions(all = emptySet())
    }

    @Test
    fun `should return reactions from timeline with redaction pending status if a redaction and the original reaction are in the outbox`() =
        runTest {
            every { roomServiceMock.getTimelineEvent(any(), eventId) } returns
                MutableStateFlow(timelineEvent(user1, eventId, RoomMessageEventContent.TextBased.Text("Hello")))
            every { roomServiceMock.getOutbox(any()) } returns
                MutableStateFlow(
                    listOf(
                        MutableStateFlow(
                            RoomOutboxMessage(
                                roomId = roomId,
                                transactionId = "123",
                                content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                                createdAt = Instant.fromEpochSeconds(123, 0),
                                sentAt = Instant.fromEpochSeconds(123, 0),
                                eventId = reaction1,
                                sendError = null,
                            )
                        ),
                        MutableStateFlow(
                            RoomOutboxMessage(
                                roomId = roomId,
                                transactionId = "124",
                                content = RedactionEventContent(reaction1),
                                createdAt = Instant.fromEpochSeconds(124, 0),
                                sentAt = null,
                                eventId = null,
                                sendError = null,
                            )
                        ),
                    )
                )
            every { roomServiceMock.getTimelineEvent(roomId, reaction1) } returns
                MutableStateFlow(
                    timelineEvent(
                        content = ReactionEventContent(RelatesTo.Annotation(eventId, "🎉")),
                        sender = user1,
                        eventId = reaction1,
                    )
                )

            every { roomServiceMock.getTimelineEventRelations(any(), any(), any()) } returns
                MutableStateFlow(mapOf(reaction1 to MutableStateFlow(timelineEventRelation(reaction1))))

            val allReactions = getEventReactions().all
            allReactions shouldHaveSize 1
            allReactions.first().status shouldBe ReactionStatus.RedactionPending
        }

    private suspend fun getEventReactions(): EventReactions =
        GetEventReactionsImpl(i18n)
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
