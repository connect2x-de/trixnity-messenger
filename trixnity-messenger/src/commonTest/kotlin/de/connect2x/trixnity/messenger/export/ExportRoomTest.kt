package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.export.ExportRoomResult.Success.DecryptionFailed
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
    private val roomIdWithErrors = RoomId("roomWithErrors", "localhost")
    private val alice = UserId("alice", "localhost")
    private val fakeProperties = object : ExportRoomSinkProperties {}

    private fun timelineEvent(i: Long, decryptionError: Boolean = false) = flowOf(
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
        ).run {
            if (decryptionError) copy(
                content = Result.failure(
                    TimelineEvent.TimelineEventContentError.DecryptionError(
                        IllegalStateException("decryption error")
                    )
                )
            ) else this
        }
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
            every { roomServiceMock.getById(roomId) } returns MutableStateFlow(
                Room(isDirect = true, roomId = roomId, lastEventId = EventId("19"))
            )
            every { roomServiceMock.getById(roomIdWithErrors) } returns MutableStateFlow(
                Room(isDirect = true, roomId = roomIdWithErrors, lastEventId = EventId("21"))
            )
            val timeline =
                (0..9).map { timelineEvent(it.toLong()) } +
                        (10..19).map { timelineEventWithMedia(it.toLong()) }
            val timelineWithErrors =
                (0..7).map { timelineEvent(it.toLong()) } +
                        (8..9).map { timelineEvent(it.toLong(), true) } +
                        (10..21).map { timelineEventWithMedia(it.toLong()) }

            every {
                roomServiceMock.getTimelineEvents(roomId, any(), Direction.BACKWARDS, any())
            } returns timeline.reversed().asFlow()
            every {
                roomServiceMock.getTimelineEvents(
                    roomIdWithErrors,
                    any(),
                    Direction.BACKWARDS,
                    any()
                )
            } returns timelineWithErrors.reversed().asFlow()

            every {
                roomServiceMock.getTimelineEvents(roomId, any(), Direction.FORWARDS, any())
            } calls { params ->
                val eventId = params.args[1] as EventId
                timeline.asFlow().dropWhile { it.first().eventId != eventId }
            }
            every {
                roomServiceMock.getTimelineEvents(
                    roomIdWithErrors,
                    any(),
                    Direction.FORWARDS,
                    any()
                )
            } calls { params ->
                val eventId = params.args[1] as EventId
                timelineWithErrors.asFlow().dropWhile { it.first().eventId != eventId }
            }

            everySuspend {
                mediaServiceMock.getMedia(any(), any(), any())
            } returns Result.success(flowOf(ByteArray(0)))
            everySuspend {
                mediaServiceMock.getMedia("mxc://localhost/20", any(), any())
            } returns Result.failure(IllegalStateException("download error"))
            everySuspend {
                mediaServiceMock.getMedia("mxc://localhost/21", any(), any())
            } returns Result.failure(IllegalStateException("download error"))

            every { sinkFactoryMock.create(any(), any()) } returns sinkMock
            everySuspend { sinkMock.start() } returns Result.success(Unit)
            everySuspend { sinkMock.finish() } returns Result.success(Unit)
            everySuspend { sinkMock.processTimelineEvent(any(), any()) } returns Result.success(Unit)
        }

        should("export timeline") {
            val cut = cut()

            cut(roomId, fakeProperties, matrixClientMock) shouldBe ExportRoomResult.Success()

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
        should("export timeline in chunks") {
            val cut = cut()

            cut(roomId, fakeProperties, matrixClientMock, buffer = 15) shouldBe ExportRoomResult.Success()

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
            cut(roomId, fakeProperties, matrixClientMock, progress = progress) shouldBe ExportRoomResult.Success()

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
            ) shouldBe ExportRoomResult.Success()

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
        should("add errors to result") {
            val cut = cut()

            cut(roomIdWithErrors, fakeProperties, matrixClientMock) shouldBe
                    ExportRoomResult.Success(
                        missingMedia = listOf(
                            ExportRoomResult.Success.MissingMedia(
                                EventId("20"),
                                "bXhjOi8vbG9jYWxob3N0LzIw",
                                "download error"
                            ),
                            ExportRoomResult.Success.MissingMedia(
                                EventId("21"),
                                "bXhjOi8vbG9jYWxob3N0LzIx",
                                "download error"
                            ),
                        ),
                        decryptionFailed = listOf(
                            DecryptionFailed(eventId = EventId("8"), reason = "error while decrypting TimelineEvent"),
                            DecryptionFailed(eventId = EventId("9"), reason = "error while decrypting TimelineEvent"),
                        ),
                    )
        }
    }

    private fun cut(): ExportRoom = ExportRoomImpl(listOf(sinkFactoryMock))
}
