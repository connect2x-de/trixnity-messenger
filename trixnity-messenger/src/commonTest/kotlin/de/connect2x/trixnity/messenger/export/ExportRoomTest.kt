package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.matcher.nullable.notNull
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
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

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val mediaServiceMock = mock<MediaService>()

    val sinkFactoryMock = mock<ExportRoomSinkFactory>()

    val sinkMock = mock<ExportRoomSink>()

    init {
        beforeTest {
            resetMocks(matrixClientMock, roomServiceMock, mediaServiceMock, sinkFactoryMock, sinkMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock }
                        single { mediaServiceMock }
                    }
                )
            }.koin
            every { roomServiceMock.getById(any()) } returns MutableStateFlow(
                Room(isDirect = true, roomId = roomId, lastEventId = EventId("19"))
            )
            val timeline =
                (0..9).map { timelineEvent(it.toLong()) } +
                        (10..19).map { timelineEventWithMedia(it.toLong()) }

            every {
                roomServiceMock.getTimelineEvents(any(), any(), eq(Direction.BACKWARDS), any())
            } returns timeline.reversed().asFlow()
            every {
                roomServiceMock.getTimelineEvents(any(), any(), eq(Direction.FORWARDS), any())
            } calls { params ->
                val eventId = params.args[1] as EventId
                timeline.asFlow().dropWhile { it.first().eventId != eventId }
            }

            everySuspend { mediaServiceMock.getMedia(any(), any(), any()) } returns Result.success(
                flowOf(ByteArray(0))
            )

            every { sinkFactoryMock.create(any(), any()) } returns sinkMock
            everySuspend { sinkMock.start() } returns Result.success(Unit)
            everySuspend { sinkMock.finish() } returns Result.success(Unit)
            everySuspend { sinkMock.processTimelineEvent(any(), any()) } returns Result.success(Unit)
        }

        should("export timeline") {
            val cut = cut()

            cut(roomId, fakeProperties, matrixClientMock) shouldBe ExportRoomResult.Success

            verifySuspend(VerifyMode.order) {
                sinkFactoryMock.create(roomId, fakeProperties)

                sinkMock.start()
                (0..9).forEach {
                    sinkMock.processTimelineEvent(eq(timelineEvent(it.toLong()).first()), eqNull())
                }
                (10..19).forEach {
                    mediaServiceMock.getMedia(any(), any(), eq(false))
                    sinkMock.processTimelineEvent(eq(timelineEventWithMedia(it.toLong()).first()), notNull())
                }
                sinkMock.finish()
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

            verifySuspend(VerifyMode.order) {
                sinkFactoryMock.create(roomId, fakeProperties)

                sinkMock.start()

                roomServiceMock.getTimelineEvents(any(), any(), any(), any())

                (6..9).forEach {
                    sinkMock.processTimelineEvent(eq(timelineEvent(it.toLong()).first()), eqNull())
                }
                (10..14).forEach {
                    matrixClientMock.di
                    mediaServiceMock.getMedia(any(), any(), eq(false))
                    sinkMock.processTimelineEvent(eq(timelineEventWithMedia(it.toLong()).first()), notNull())
                }
                sinkMock.finish()
            }
        }
    }

    private fun cut(): ExportRoom = ExportRoomImpl(listOf(sinkFactoryMock))
}
