package de.connect2x.trixnity.messenger.export

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.clientserverapi.model.room.GetEvents
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.export.ExportRoomResult.Success.DecryptionFailed
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.nullable.notNull
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class ExportRoomTest {
    val roomId = RoomId("!room1")
    val roomIdWithErrors = RoomId("!roomWithErrors")
    val alice = UserId("alice", "localhost")
    val maxMediaSize = Long.MAX_VALUE
    val fakeProperties = object : ExportRoomSinkProperties {}
    val timeline = (0..9).map { timelineEvent(it.toLong()) } + (10..19).map { timelineEventWithMedia(it.toLong()) }
    val timelineWithErrors =
        (0..7).map { timelineEvent(it.toLong()) } +
            (8..9).map { timelineEvent(it.toLong(), true) } +
            (10..21).map { timelineEventWithMedia(it.toLong()) }

    val roomServiceMock =
        mock<RoomService> {
            every { getById(roomId) } returns
                MutableStateFlow(Room(isDirect = true, roomId = roomId, lastEventId = EventId("19")))
            every { getById(roomIdWithErrors) } returns
                MutableStateFlow(Room(isDirect = true, roomId = roomIdWithErrors, lastEventId = EventId("21")))
            every { getTimelineEvents(roomId, any(), GetEvents.Direction.BACKWARDS, any()) } returns
                timeline.reversed().asFlow()
            every { getTimelineEvents(roomIdWithErrors, any(), GetEvents.Direction.BACKWARDS, any()) } returns
                timelineWithErrors.reversed().asFlow()
            every { getTimelineEvents(roomId, any(), GetEvents.Direction.FORWARDS, any()) } calls
                { params ->
                    val eventId = params.args[1] as EventId
                    timeline.asFlow().dropWhile { it.first().eventId != eventId }
                }
            every { getTimelineEvents(roomIdWithErrors, any(), GetEvents.Direction.FORWARDS, any()) } calls
                { params ->
                    val eventId = params.args[1] as EventId
                    timelineWithErrors.asFlow().dropWhile { it.first().eventId != eventId }
                }
        }
    val mediaServiceMock =
        mock<MediaService> {
            everySuspend { getMedia(any(), any(), any(), any()) } returns
                Result.success(InMemoryPlatformMedia(flowOf(ByteArray(0))))
            everySuspend { getMedia("mxc://localhost/20", any(), any(), any()) } returns
                Result.failure(IllegalStateException("download error"))
            everySuspend { getMedia("mxc://localhost/21", any(), any(), any()) } returns
                Result.failure(IllegalStateException("download error"))
        }

    val matrixClientMock =
        mock<MatrixClient> {
            every { di } returns
                koinApplication {
                        modules(
                            module {
                                single { roomServiceMock }
                                single { mediaServiceMock }
                            }
                        )
                    }
                    .koin
        }

    val sinkMock =
        mock<ExportRoomSink> {
            everySuspend { start() } returns Result.success(Unit)
            everySuspend { finish() } returns Result.success(Unit)
            everySuspend { processTimelineEvent(any(), any()) } returns Result.success(Unit)
        }

    val sinkFactoryMock = mock<ExportRoomSinkFactory> { every { create(any(), any()) } returns sinkMock }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `export timeline`() = runTest {
        val cut = cut()

        cut(
            roomId,
            fakeProperties,
            matrixClientMock,
            includeMedia = true,
            timeZone = TimeZone.of("CET"),
            maxMediaSize = maxMediaSize,
        ) shouldBe ExportRoomResult.Success()

        val timelineEvents = (0..9).map { timelineEvent(it.toLong()).first() }
        val timelineEventsWithMedia = (10..19).map { timelineEventWithMedia(it.toLong()).first() }

        // WARNING: These loops have to stay while loops because kotlin miscompiles wasm otherwise
        var index: Int
        verifySuspend(VerifyMode.order) {
            sinkFactoryMock.create(roomId, fakeProperties)

            sinkMock.start()
            index = 0
            while (index < timelineEvents.size) {
                sinkMock.processTimelineEvent(timelineEvents[index], null)
                index += 1
            }

            index = 0
            while (index < timelineEventsWithMedia.size) {
                mediaServiceMock.getMedia(any(), any(), any(), false)
                sinkMock.processTimelineEvent(timelineEventsWithMedia[index], notNull())
                index += 1
            }
            sinkMock.finish()
        }
    }

    @Test
    fun `export timeline in chunks`() = runTest {
        val cut = cut()

        cut(
            roomId,
            fakeProperties,
            matrixClientMock,
            buffer = 15,
            includeMedia = true,
            timeZone = TimeZone.of("CET"),
            maxMediaSize = maxMediaSize,
        ) shouldBe ExportRoomResult.Success()

        val timelineEvents = (0..9).map { timelineEvent(it.toLong()).first() }
        val timelineEventsWithMedia = (10..19).map { timelineEventWithMedia(it.toLong()).first() }

        // WARNING: These loops have to stay while loops because kotlin miscompiles wasm otherwise
        var index: Int
        verifySuspend(VerifyMode.order) {
            sinkFactoryMock.create(roomId, fakeProperties)

            sinkMock.start()
            index = 0
            while (index < timelineEvents.size) {
                sinkMock.processTimelineEvent(timelineEvents[index], null)
                index += 1
            }

            index = 0
            while (index < timelineEventsWithMedia.size) {
                mediaServiceMock.getMedia(any(), any(), any(), false)
                sinkMock.processTimelineEvent(timelineEventsWithMedia[index], notNull())
                index += 1
            }
            sinkMock.finish()
        }
    }

    @Test
    fun `should not export media when downloads are disabled`() = runTest {
        val cut = cut()

        cut(
            roomId,
            fakeProperties,
            matrixClientMock,
            buffer = 15,
            includeMedia = false,
            timeZone = TimeZone.of("CET"),
            maxMediaSize = maxMediaSize,
        ) shouldBe ExportRoomResult.Success()

        verifySuspend(VerifyMode.not) { mediaServiceMock.getMedia(any(), any(), false) }
    }

    @Test
    fun `track progress`() = runTest {
        val cut = cut()

        val progress = MutableStateFlow(ExportRoomProgress())
        cut(
            roomId,
            fakeProperties,
            matrixClientMock,
            progress = progress,
            includeMedia = true,
            timeZone = TimeZone.of("CET"),
            maxMediaSize = maxMediaSize,
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
            includeMedia = true,
            rangeStartCondition = { it.eventId == EventId("5") },
            rangeEndCondition = { it.eventId == EventId("15") },
            timeZone = TimeZone.of("CET"),
            maxMediaSize = maxMediaSize,
        ) shouldBe ExportRoomResult.Success()

        val timelineEvents = (6..9).map { timelineEvent(it.toLong()).first() }
        val timelineEventsWithMedia = (10..14).map { timelineEventWithMedia(it.toLong()).first() }

        // WARNING: These loops have to stay while loops because kotlin miscompiles wasm otherwise
        var index: Int
        verifySuspend(VerifyMode.order) {
            sinkFactoryMock.create(roomId, fakeProperties)

            sinkMock.start()

            roomServiceMock.getTimelineEvents(any(), any(), any(), any())

            index = 0
            while (index < timelineEvents.size) {
                sinkMock.processTimelineEvent(timelineEvents[index], null)
                index += 1
            }
            index = 0
            while (index < timelineEventsWithMedia.size) {
                matrixClientMock.di
                mediaServiceMock.getMedia(any(), any(), any(), false)
                sinkMock.processTimelineEvent(timelineEventsWithMedia[index], notNull())
                index += 1
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
            includeMedia = true,
            timeZone = TimeZone.of("CET"),
            maxMediaSize = maxMediaSize,
        ) shouldBe
            ExportRoomResult.Success(
                missingMedia =
                    listOf(
                        ExportRoomResult.Success.MissingMedia(
                            EventId("20"),
                            "1970-01-01 01-00-00 5QY614QVkcLdYtkkYsCMVNBTJpodaRYi_eWwpDOq3Pw= - 20",
                            "download error",
                        ),
                        ExportRoomResult.Success.MissingMedia(
                            EventId("21"),
                            "1970-01-01 01-00-00 q-16CVWL4b6kAxFcS-a4WalLqh7GwCFXHvFQa1P0eIE= - 21",
                            "download error",
                        ),
                    ),
                decryptionFailed =
                    listOf(
                        DecryptionFailed(eventId = EventId("8"), reason = "error while decrypting TimelineEvent"),
                        DecryptionFailed(eventId = EventId("9"), reason = "error while decrypting TimelineEvent"),
                    ),
            )
    }

    fun cut(): ExportRoom = ExportRoomImpl(listOf(sinkFactoryMock))

    fun timelineEvent(i: Long, decryptionError: Boolean = false) =
        flowOf(
            TimelineEvent(
                    event =
                        ClientEvent.RoomEvent.MessageEvent(
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
                .run {
                    if (decryptionError)
                        copy(
                            content =
                                Result.failure(
                                    TimelineEvent.TimelineEventContentError.DecryptionError(
                                        IllegalStateException("decryption error")
                                    )
                                )
                        )
                    else this
                }
        )

    fun timelineEventWithMedia(i: Long) =
        flowOf(
            TimelineEvent(
                event =
                    ClientEvent.RoomEvent.MessageEvent(
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
