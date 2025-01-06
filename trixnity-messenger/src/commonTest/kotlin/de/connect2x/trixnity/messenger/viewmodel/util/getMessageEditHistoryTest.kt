package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.firstNotNullWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.advanceUntilIdle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.GetTimelineEventConfig
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.TimelineEventRelation
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.m.RelationType
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import org.koin.dsl.koinApplication
import org.koin.dsl.module


@OptIn(ExperimentalCoroutinesApi::class)
class getMessageEditHistoryTest : ShouldSpec() {

    private val matrixClientMock = mock<MatrixClient>()
    private val roomServiceMock = mock<RoomService>()

    private val userId = UserId("mimi", "localhost")
    private val roomId = RoomId("room1")
    private val timestamp0: Long = 1000
    private val timestamp1: Long = 2000
    private val timestamp2: Long = 3000
    private val timestamp3: Long = 4000
    private val message0: String = "initial message"
    private val message1: String = "first edit"
    private val message2: String = "second edit"
    private val message3: String = "third edit"

    private val fakeTimeline: MutableStateFlow<
            Pair<Pair<EventId, Map<EventId, Flow<TimelineEventRelation>>>,
                    Map<EventId, TimelineEvent>>?
            > = MutableStateFlow(null)

    private fun makeTimelineEvent(eventId: EventId, timestamp: Long = 0L, message: String = "") = TimelineEvent(
        MessageEvent(
            MegolmEncryptedMessageEventContent(
                "",
                Key.Curve25519Key(value = ""),
                deviceId = "",
                sessionId = "",
            ),
            eventId,
            userId,
            roomId,
            timestamp,
        ),
        Result.success(
            RoomMessageEventContent.TextBased.Text(message),
        ),
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

    private fun applyEventHistory(
        parentEvent: TimelineEvent,
        relatedEvents: List<TimelineEvent> = listOf(),
        transformRelatedEventFlow: (eventId: EventId, eventFlow: Flow<TimelineEventRelation>) -> Flow<TimelineEventRelation> =
            { _, eventFlow -> eventFlow },
    ) {
        val relations = relatedEvents.associate {
            it.eventId to transformRelatedEventFlow(
                it.eventId, flowOf(
                    TimelineEventRelation(
                        roomId, it.eventId, RelationType.Replace, parentEvent.eventId,
                    )
                )
            )
        }
        val events = (relatedEvents + parentEvent).associateBy { it.eventId }
        fakeTimeline.value = Pair(Pair(parentEvent.eventId, relations), events)
    }

    init {
        coroutineTestScope = true

        beforeTest {
            resetCalls(
                matrixClientMock,
                roomServiceMock,
            )
            resetMocks(
                matrixClientMock,
                roomServiceMock,
            )
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns userId
            every { roomServiceMock.getTimelineEventRelations(eq(roomId), any(), eq(RelationType.Replace)) } calls {
                val eventId: EventId = it.arg(1)
                fakeTimeline.flatMapLatest { timeline ->
                    timeline.shouldNotBeNull()
                    val relations = timeline.first
                    relations.first shouldBe eventId
                    flowOf(relations.second)
                }
            }
            every { roomServiceMock.getTimelineEvent(eq(roomId), any(), any()) } calls {
                val eventId: EventId = it.arg(1)
                val config: GetTimelineEventConfig.() -> Unit = it.arg(2)
//                GetTimelineEventConfig().apply(config).copy().apply {
//                    fetchSize shouldBe 1
//                    allowReplaceContent shouldBe false
//                }
                fakeTimeline.value.shouldNotBeNull()
                val event = fakeTimeline.value!!.second.get(eventId)
                flowOf(event)
            }
        }

        should("return one element if there is no edits") {
            val eventId0 = EventId("event0")
            applyEventHistory(makeTimelineEvent(eventId0, timestamp0, message0))
            val cut = cut(eventId0)
            cut.firstNotNullWithClue().let {
                it shouldHaveSize 1
                it[0].eventId shouldBe eventId0
                it[0].originTimestamp shouldBe timestamp0
                it[0].content!! shouldBeSuccess RoomMessageEventContent.TextBased.Text(message0)
            }
            cancelNeverEndingCoroutines()
        }

        should("return the current message and the one edit") {
            val eventId0 = EventId("event0")
            val eventId1 = EventId("event1")
            applyEventHistory(
                makeTimelineEvent(eventId0, timestamp0, message0), listOf(
                    makeTimelineEvent(eventId1, timestamp1, message1),
                )
            )
            val cut = cut(eventId0)
            cut.firstNotNullWithClue().let {
                it shouldHaveSize 2
                it[0].eventId shouldBe eventId0
                it[0].originTimestamp shouldBe timestamp0
                it[0].content!! shouldBeSuccess RoomMessageEventContent.TextBased.Text(message0)
                it[1].eventId shouldBe eventId1
                it[1].originTimestamp shouldBe timestamp1
                it[1].content!! shouldBeSuccess RoomMessageEventContent.TextBased.Text(message1)
            }
            cancelNeverEndingCoroutines()
        }

        should("return the current message and all three edits") {
            val eventId0 = EventId("event0")
            val eventId1 = EventId("event1")
            val eventId2 = EventId("event2")
            val eventId3 = EventId("event3")
            applyEventHistory(
                makeTimelineEvent(eventId0, timestamp0, message0), listOf(
                    makeTimelineEvent(eventId1, timestamp1, message1),
                    makeTimelineEvent(eventId2, timestamp2, message2),
                    makeTimelineEvent(eventId3, timestamp3, message3),
                )
            )
            val cut = cut(eventId0)
            cut.firstNotNullWithClue().let {
                it shouldHaveSize 4
                it[0].eventId shouldBe eventId0
                it[0].originTimestamp shouldBe timestamp0
                it[0].content!! shouldBeSuccess RoomMessageEventContent.TextBased.Text(message0)
                it[1].eventId shouldBe eventId1
                it[1].originTimestamp shouldBe timestamp1
                it[1].content!! shouldBeSuccess RoomMessageEventContent.TextBased.Text(message1)
                it[2].eventId shouldBe eventId2
                it[2].originTimestamp shouldBe timestamp2
                it[2].content!! shouldBeSuccess RoomMessageEventContent.TextBased.Text(message2)
                it[3].eventId shouldBe eventId3
                it[3].originTimestamp shouldBe timestamp3
                it[3].content!! shouldBeSuccess RoomMessageEventContent.TextBased.Text(message3)
            }
        }

        should("not block the loading of the other edits if one's unavailable") {
            val eventId0 = EventId("event0")
            val eventId1 = EventId("event1")
            val eventId2 = EventId("event2")
            val eventId3 = EventId("event3")
            applyEventHistory(
                makeTimelineEvent(eventId0, timestamp0, message0), listOf(
                    makeTimelineEvent(eventId1, timestamp1, message1),
                    makeTimelineEvent(eventId2, timestamp2, message2),
                    makeTimelineEvent(eventId3, timestamp3, message3),
                ),
                { eventId, eventFlow ->
                    if (eventId == eventId2)
                        flow {
//                            while (true) {
//                            }
                        }
                    else eventFlow
                }
            )
            val cut = cut(eventId0)
            cut.firstNotNullWithClue().let {
                it shouldHaveSize 4

            }

            cancelNeverEndingCoroutines()
        }

        should("update list once new edit has been made") {
            val eventId0 = EventId("event0")
            val eventId1 = EventId("event1")
            val eventId2 = EventId("event2")
            applyEventHistory(
                makeTimelineEvent(eventId0, timestamp0, message0), listOf(
                    makeTimelineEvent(eventId1, timestamp1, message1),
                )
            )
            val history = MutableStateFlow<List<TimelineEvent>?>(null)
//            val cut = cut(eventId0)
            cut(eventId0).collect {
                history.value = it
            }

            advanceUntilIdle()

            history.value!!.let {
                it shouldHaveSize 2

            }


            applyEventHistory(
                makeTimelineEvent(eventId0, timestamp0, message0), listOf(
                    makeTimelineEvent(eventId1, timestamp1, message1),
                    makeTimelineEvent(eventId2, timestamp2, message2),
                )
            )

            advanceUntilIdle()
            history.value!!.let {
                it shouldHaveSize 3

            }

            cancelNeverEndingCoroutines()
        }

        should("throw error if access is not granted") { TODO() }
    }

    private fun TestScope.cut(
        eventId: EventId,
    ): Flow<List<TimelineEvent>> {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock))
            )
        }.koin
        return getMessageEditHistory(
            client = testMatrixClientViewModelContext(
                di = di,
                userId = userId,
            ).matrixClient,
            eventId = eventId,
            roomId = roomId,
        )
    }
}
