package de.connect2x.trixnity.messenger.export

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.MediaService
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents.Direction
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ExportRoomTest : ShouldSpec() {

    private val roomId = RoomId("room1", "localhost")
    private val alice = UserId("alice", "localhost")
    private val fakeProperties = object : ExportRoomSinkProperties {}

    private fun timelineEvent(i: Long) = flowOf(
        TimelineEvent(
            event = ClientEvent.RoomEvent.MessageEvent(
                content = RoomMessageEventContent.TextBased.Text(i.toString()),
                id = EventId(i.toString()),
                sender = alice,
                roomId = roomId,
                originTimestamp = i,
            ),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
    )

    private fun timelineEventWithMedia(i: Long) = flowOf(
        TimelineEvent(
            event = ClientEvent.RoomEvent.MessageEvent(
                content = RoomMessageEventContent.FileBased.File(i.toString(), url = "mxc://localhost/$i"),
                id = EventId(i.toString()),
                sender = alice,
                roomId = roomId,
                originTimestamp = i,
            ),
            previousEventId = null,
            nextEventId = null,
            gap = null,
        )
    )

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var mediaServiceMock: MediaService

    @Mock
    lateinit var sinkFactoryMock: ExportRoomSinkFactory

    @Mock
    lateinit var sinkMock: ExportRoomSink

    init {
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { mediaServiceMock }
                        }
                    )
                }.koin
                every { roomServiceMock.getById(isAny()) } returns MutableStateFlow(
                    Room(isDirect = true, roomId = roomId, lastEventId = EventId("3"))
                )
                val timeline =
                    (0..9).map { timelineEvent(it.toLong()) } +
                            (10..19).map { timelineEventWithMedia(it.toLong()) }

                every {
                    roomServiceMock.getTimelineEvents(isAny(), isAny(), isEqual(Direction.BACKWARDS), isAny())
                } returns timeline.reversed().asFlow()
                every {
                    roomServiceMock.getTimelineEvents(isAny(), isAny(), isEqual(Direction.FORWARDS), isAny())
                } runs { params ->
                    val eventId = params[1] as EventId
                    timeline.asFlow().dropWhile { it.first().eventId != eventId }
                }


                everySuspending { mediaServiceMock.getMedia(isAny(), isAny(), isAny()) } returns Result.success(
                    flowOf(ByteArray(0))
                )

                every { sinkFactoryMock.create(isAny(), isAny()) } returns sinkMock
                everySuspending { sinkMock.start() } returns Result.success(Unit)
                everySuspending { sinkMock.finish() } returns Result.success(Unit)
                everySuspending { sinkMock.processTimelineEvent(isAny(), isAny()) } returns Result.success(Unit)
            }
        }

        should("export timeline") {
            val cut = cut()

            cut(roomId, fakeProperties, matrixClientMock) shouldBe ExportRoomResult.Success

            with(mocker) {
                verifyWithSuspend(exhaustive = false) {
                    sinkFactoryMock.create(roomId, fakeProperties)

                    sinkMock.start()
                    (0..9).forEach {
                        sinkMock.processTimelineEvent(isEqual(timelineEvent(it.toLong()).first()), isNull())
                    }
                    (10..19).forEach {
                        mediaServiceMock.getMedia(isAny(), isAny(), isEqual(false))
                        sinkMock.processTimelineEvent(isEqual(timelineEventWithMedia(it.toLong()).first()), isNotNull())
                    }
                    sinkMock.finish()
                }
            }
        }
        should("track progress") {
            val cut = cut()

            val progress = MutableStateFlow(ExportRoomProgress())
            cut(roomId, fakeProperties, matrixClientMock, progress = progress) shouldBe ExportRoomResult.Success

            progress.value shouldBe ExportRoomProgress(20, 20)
        }
        should("allow to export range") {
            val cut = cut()

            cut(
                roomId,
                fakeProperties,
                matrixClientMock,
                rangeStartCondition = { it.eventId == EventId("5") },
                rangeEndCondition = { it.eventId == EventId("15") },
            ) shouldBe ExportRoomResult.Success

            with(mocker) {
                verifyWithSuspend(exhaustive = false) {
                    (6..9).forEach {
                        sinkMock.processTimelineEvent(isEqual(timelineEvent(it.toLong()).first()), isNull())
                    }
                    (10..14).forEach {
                        sinkMock.processTimelineEvent(isEqual(timelineEventWithMedia(it.toLong()).first()), isNotNull())
                    }
                }
            }
        }
    }

    private fun cut(): ExportRoom = ExportRoomImpl(listOf(sinkFactoryMock))
}
