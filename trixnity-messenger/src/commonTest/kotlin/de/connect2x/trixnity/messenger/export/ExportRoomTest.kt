package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.messenger.eqNull
import de.connect2x.trixnity.messenger.export.ExportRoomResult.Success.DecryptionFailed
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
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
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
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
import kotlin.test.Test

class ExportRoomTest {

    val roomId = RoomId("room1", "localhost")
    val roomIdWithErrors = RoomId("roomWithErrors", "localhost")
    val alice = UserId("alice", "localhost")
    val fakeProperties = object : ExportRoomSinkProperties {}
    val timeline = (0..9).map { timelineEvent(it.toLong()) } + (10..19).map { timelineEventWithMedia(it.toLong()) }
    val timelineWithErrors = (0..7).map { timelineEvent(it.toLong()) } + (8..9).map {
        timelineEvent(
            it.toLong(),
            true
        )
    } + (10..21).map { timelineEventWithMedia(it.toLong()) }


    val roomServiceMock = mock<RoomService> {
        every { getById(roomId) } returns MutableStateFlow(
            Room(isDirect = true, roomId = roomId, lastEventId = EventId("19"))
        )
        every { getById(roomIdWithErrors) } returns MutableStateFlow(
            Room(isDirect = true, roomId = roomIdWithErrors, lastEventId = EventId("21"))
        )
        every {
            getTimelineEvents(roomId, any(), Direction.BACKWARDS, any())
        } returns timeline.reversed().asFlow()
        every {
            getTimelineEvents(
                roomIdWithErrors, any(), Direction.BACKWARDS, any()
            )
        } returns timelineWithErrors.reversed().asFlow()
        every {
            getTimelineEvents(roomId, any(), Direction.FORWARDS, any())
        } calls { params ->
            val eventId = params.args[1] as EventId
            timeline.asFlow().dropWhile { it.first().eventId != eventId }
        }
        every {
            getTimelineEvents(
                roomIdWithErrors, any(), Direction.FORWARDS, any()
            )
        } calls { params ->
            val eventId = params.args[1] as EventId
            timelineWithErrors.asFlow().dropWhile { it.first().eventId != eventId }
        }
    }
    val mediaServiceMock = mock<MediaService> {
        everySuspend {
            getMedia(any(), any(), any())
        } returns Result.success(InMemoryPlatformMedia(flowOf(ByteArray(0))))
        everySuspend {
            getMedia("mxc://localhost/20", any(), any())
        } returns Result.failure(IllegalStateException("download error"))
        everySuspend {
            getMedia("mxc://localhost/21", any(), any())
        } returns Result.failure(IllegalStateException("download error"))
    }

    val matrixClientMock = mock<MatrixClient> {
        every { di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { mediaServiceMock }
                })
        }.koin
    }

    val sinkMock = mock<ExportRoomSink> {
        everySuspend { start() } returns Result.success(Unit)
        everySuspend { finish() } returns Result.success(Unit)
        everySuspend { processTimelineEvent(any(), any()) } returns Result.success(Unit)
    }

    val sinkFactoryMock = mock<ExportRoomSinkFactory> {
        every { create(any(), any()) } returns sinkMock
    }

    @Test
    fun `export timeline`() = runTest {
        val cut = cut()

        cut(
            roomId, fakeProperties, matrixClientMock, timeZone = TimeZone.of("CET")
        ) shouldBe ExportRoomResult.Success()

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

    @Test
    fun `export timeline in chunks`() = runTest {
        val cut = cut()

        cut(
            roomId, fakeProperties, matrixClientMock, buffer = 15, timeZone = TimeZone.of("CET")
        ) shouldBe ExportRoomResult.Success()

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

    @Test
    fun `track progress`() = runTest {
        val cut = cut()

        val progress = MutableStateFlow(ExportRoomProgress())
        cut(
            roomId, fakeProperties, matrixClientMock, progress = progress, timeZone = TimeZone.of("CET")
        ) shouldBe ExportRoomResult.Success()

        progress.value shouldBe ExportRoomProgress(20, 20)
    }

    @Test
    fun `allow to export range`() = runTest {
        val cut = cut()

        cut(
            roomId,
            fakeProperties,
            matrixClientMock,
            rangeStartCondition = { it.eventId == EventId("5") },
            rangeEndCondition = { it.eventId == EventId("15") },
            timeZone = TimeZone.of("CET"),
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

    @Test
    fun `add errors to result`() = runTest {
        val cut = cut()

        cut(
            roomIdWithErrors,
            fakeProperties,
            matrixClientMock,
            timeZone = TimeZone.of("CET")
        ) shouldBe ExportRoomResult.Success(
            missingMedia = listOf(
                ExportRoomResult.Success.MissingMedia(
                    EventId("20"),
                    "1970-01-01 01-00-00 5QY614QVkcLdYtkkYsCMVNBTJpodaRYi_eWwpDOq3Pw= - 20",
                    "download error"
                ),
                ExportRoomResult.Success.MissingMedia(
                    EventId("21"),
                    "1970-01-01 01-00-00 q-16CVWL4b6kAxFcS-a4WalLqh7GwCFXHvFQa1P0eIE= - 21",
                    "download error"
                ),
            ),
            decryptionFailed = listOf(
                DecryptionFailed(eventId = EventId("8"), reason = "error while decrypting TimelineEvent"),
                DecryptionFailed(eventId = EventId("9"), reason = "error while decrypting TimelineEvent"),
            ),
        )
    }


    fun cut(): ExportRoom = ExportRoomImpl(listOf(sinkFactoryMock))

    fun timelineEvent(i: Long, decryptionError: Boolean = false) = flowOf(
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
        })

    fun timelineEventWithMedia(i: Long) = flowOf(
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
}
