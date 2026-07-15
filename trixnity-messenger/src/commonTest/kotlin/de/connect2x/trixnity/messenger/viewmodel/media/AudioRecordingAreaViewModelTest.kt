package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.room.message.MessageBuilder
import de.connect2x.trixnity.client.room.message.audio
import de.connect2x.trixnity.client.room.message.roomMessageBuilder
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.Mentions
import de.connect2x.trixnity.core.model.events.m.room.AudioInfo
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.media.AudioRecorderHolder
import de.connect2x.trixnity.messenger.media.PlatformMediaMock
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.AudioRecordingAreaViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.AudioRecordingAreaViewModelFactory
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AudioRecordingAreaViewModelTest {
    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val mediaServiceMock = mock<MediaService>()
    val recorder = mock<AudioRecorder>()
    val mediaPlayerFactory = mock<MediaPlayerViewModelFactory>()
    val audioRecorderHolder = AudioRecorderHolder(recorder)
    val player = mock<MediaPlayerViewModel>()

    val roomId = RoomId("!room1")
    val userId = UserId("User1")

    val currentReply = MutableStateFlow<Pair<RoomId, EventId>?>(null)

    var draftMessage: MutableStateFlow<RoomOutboxMessage<*>?> = MutableStateFlow(null)

    init {
        resetMocks(matrixClientMock, roomServiceMock, mediaServiceMock, recorder, mediaPlayerFactory, player)

        every { matrixClientMock.di } returns
            koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { mediaServiceMock }
                        }
                    )
                }
                .koin
        every { matrixClientMock.userId } returns userId

        every { mediaPlayerFactory.create(any(), any(), any(), any(), any()) } returns player
        every { player.pause() } returns Unit

        every { recorder.close() } returns Unit
        everySuspend { recorder.closeSuspending() } returns Unit

        everySuspend { mediaServiceMock.prepareUploadMedia(any(), any()) } returns "testUrl"

        every { roomServiceMock.getById(roomId) } returns MutableStateFlow(Room(roomId, isDirect = true))
        every { roomServiceMock.getDraftMessage(any()) } returns draftMessage
        everySuspend { roomServiceMock.setDraftMessage(any(), any(), any()) } calls
            {
                val builder = it.arg<(suspend MessageBuilder.() -> Unit)>(2)
                val content = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, userId).build(builder)
                requireNotNull(content) { "you must add some sort of content to set a draft" }
                draftMessage.value =
                    RoomOutboxMessage(
                        roomId = roomId,
                        transactionId = "0",
                        content = content,
                        createdAt = Clock.System.now(),
                        sentAt = null,
                        eventId = null,
                        sendError = null,
                        keepMediaInCache = true,
                        isDraft = true,
                    )
                "0"
            }
        everySuspend { roomServiceMock.deleteDraftMessage(any()) } calls { draftMessage.value = null }
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `recorder unavailable - when recorder not available then player never initialized`() = runTest {
        val cut =
            audioRecordingAreaViewModel(
                backgroundScope.coroutineContext,
                additionalModule = module { single<MediaPlayerViewModelFactory> { mediaPlayerFactory } },
            )

        cut.capturePlayer.value shouldBe null
    }

    @Test
    fun `recorder unavailable - when recorder not available then sending not possible`() = runTest {
        var sent = false
        everySuspend { roomServiceMock.sendDraftMessage(any()) } calls
            {
                sent = true
                draftMessage.value = null
                "0"
            }

        val cut =
            audioRecordingAreaViewModel(
                backgroundScope.coroutineContext,
                additionalModule = module { single<MediaPlayerViewModelFactory> { mediaPlayerFactory } },
            )
        cut.sendAudioMessage()

        eventually(300.milliseconds) { sent shouldBe false }
    }

    @Test
    fun `reset recorder - when opening another room which causes a new view model to initialize then close the previous recorder from the previous room`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> = MutableStateFlow(AudioRecorder.State.Completed(
            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
            5.seconds,
            1000L,
            ContentType("audio", "ogg"),
            "ogg",
        ))
        every { recorder.state } returns recorderState
        everySuspend { recorder.closeSuspending() } calls {
            recorderState.value = AudioRecorder.State.Ready
        }
        every { recorder.close() } calls {
            recorderState.value = AudioRecorder.State.Ready
        }

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        delay(1.seconds)

        verifySuspend {
            recorder.closeSuspending()
        }
    }

    @Test
    fun `init drafts - when opening another room which causes a new view model to initialize then do not save the audio from the previous room as a draft`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> = MutableStateFlow(AudioRecorder.State.Completed(
            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
            5.seconds,
            1000L,
            ContentType("audio", "ogg"),
            "ogg",
        ))
        every { recorder.state } returns recorderState
        everySuspend { recorder.closeSuspending() } calls {
            recorderState.value = AudioRecorder.State.Ready
        }
        every { recorder.close() } calls {
            recorderState.value = AudioRecorder.State.Ready
        }

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        delay(1.seconds)

        verifySuspend(VerifyMode.not) {
            roomServiceMock.setDraftMessage(any(), any(), any())
        }
    }

    @Test
    fun `recording - when recording then player not initialized`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> = MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        recorderState.value = AudioRecorder.State.Recording(5.seconds, 5F)
        delay(1.seconds)
        cut.capturePlayer.value shouldBe null
    }

    @Test
    fun `completed - when completed then player initialized`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> = MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        recorderState.value =
            AudioRecorder.State.Completed(
                AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                5.seconds,
                1000L,
                ContentType("audio", "mp4"),
                "m4a",
            )
        delay(1.seconds)
        cut.capturePlayer.value shouldBe player
    }

    @Test
    fun `completed - when completed an audio draft is saved`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> = MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        draftMessage.value shouldBe null

        recorderState.value =
            AudioRecorder.State.Completed(
                AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                5.seconds,
                1000L,
                ContentType("audio", "mp4"),
                "m4a",
            )
        delay(1.seconds)
        draftMessage.value?.content shouldBe
            RoomMessageEventContent.FileBased.Audio(
                body = "",
                format = null,
                formattedBody = null,
                fileName = "voice_message.m4a",
                info = AudioInfo(duration = 5000, mimeType = "audio/mp4", size = 1000),
                url = "unused",
                file = null,
                relatesTo = null,
                mentions = Mentions(users = null, room = null),
                externalUrl = null,
            )
    }

    @Test
    fun `not completed - when not completed then old player paused`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> = MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        recorderState.value =
            AudioRecorder.State.Completed(
                AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                5.seconds,
                1000L,
                ContentType("audio", "mp4"),
                "m4a",
            )
        delay(1.seconds)
        recorderState.value = AudioRecorder.State.Ready
        delay(1.seconds)
        verify { player.pause() }
        cut.capturePlayer.value shouldBe null

        recorderState.value =
            AudioRecorder.State.Completed(
                AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                5.seconds,
                1000L,
                ContentType("audio", "mp4"),
                "m4a",
            )
        delay(1.seconds)
        recorderState.value = AudioRecorder.State.Recording(5.seconds, 5F)
        delay(1.seconds)
        verify { player.pause() }
        cut.capturePlayer.value shouldBe null
    }

    @Test
    fun `send - should only send when completed`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> = MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        var sent = false
        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        everySuspend { roomServiceMock.sendDraftMessage(any()) } calls
            {
                sent = true
                draftMessage.value = null
                "0"
            }
        everySuspend { roomServiceMock.sendMessage(any(), any(), any()) } calls
            {
                sent = true
                "0"
            }

        recorderState.value = AudioRecorder.State.Ready
        cut.sendAudioMessage()
        eventually(300.milliseconds) { sent shouldBe false }

        recorderState.value = AudioRecorder.State.Recording(5.seconds, 5F)
        cut.sendAudioMessage()
        eventually(300.milliseconds) { sent shouldBe false }

        recorderState.value =
            AudioRecorder.State.Completed(
                AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                5.seconds,
                1000L,
                ContentType("audio", "mp4"),
                "m4a",
            )
        cut.sendAudioMessage()
        eventually(300.milliseconds) { sent shouldBe true }
    }

    @Test
    fun `load - should load draft into recorder if duration not null`() = runTest {
        var wasLoaded = false
        everySuspend { recorder.load(any()) } calls { wasLoaded = true }
        every { recorder.state } returns MutableStateFlow(AudioRecorder.State.Ready)

        val body = "cool audio"
        val fileName = "cool_audio.m4a"
        val contentType = ContentType("audio", "mp4")
        val platformMedia = PlatformMediaMock

        everySuspend { mediaServiceMock.getMedia(any(), any(), any(), any()) } returns Result.success(platformMedia)

        val builder: (suspend MessageBuilder.() -> Unit) = {
            audio(body = body, audio = platformMedia, fileName = fileName, type = contentType, duration = 69)
        }

        val content = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, userId).build(builder)

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        cut.loadAudioMessage(content as RoomMessageEventContent.FileBased.Audio)

        eventually(300.milliseconds) { wasLoaded shouldBe true }
    }

    @Test
    fun `load - should not load draft into recorder if duration is null`() = runTest {
        var wasLoaded = false
        everySuspend { recorder.load(any()) } calls { wasLoaded = true }
        every { recorder.state } returns MutableStateFlow(AudioRecorder.State.Ready)

        val body = "cool audio"
        val fileName = "cool_audio.m4a"
        val contentType = ContentType("audio", "mp4")
        val platformMedia = PlatformMediaMock

        everySuspend { mediaServiceMock.getMedia(any(), any(), any(), any()) } returns Result.success(platformMedia)

        val builder: (suspend MessageBuilder.() -> Unit) = {
            audio(body = body, audio = platformMedia, fileName = fileName, type = contentType, duration = null)
        }

        val content = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, userId).build(builder)

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        cut.loadAudioMessage(content as RoomMessageEventContent.FileBased.Audio)

        eventually(300.milliseconds) { wasLoaded shouldBe false }
    }

    @Test
    fun `load - should not load draft into recorder if media retrieval failed`() = runTest {
        var wasLoaded = false
        everySuspend { recorder.load(any()) } calls { wasLoaded = true }
        every { recorder.state } returns MutableStateFlow(AudioRecorder.State.Ready)

        val body = "cool audio"
        val fileName = "cool_audio.m4a"
        val contentType = ContentType("audio", "mp4")

        everySuspend { mediaServiceMock.getMedia(any(), any(), any(), any()) } returns Result.failure(Exception())

        val builderWithoutUrlNorFile: (suspend MessageBuilder.() -> Unit) = {
            roomMessageBuilder(body, null, null) {
                RoomMessageEventContent.FileBased.Audio(
                    body = this.body,
                    format = this.format,
                    formattedBody = this.formattedBody,
                    fileName = fileName,
                    info = AudioInfo(
                        duration = 420,
                        mimeType = ContentType.Audio.OGG.toString(),
                        size = 5,
                    ),
                    url = null,
                    file = null,
                    relatesTo = relatesTo,
                    mentions = mentions,
                )
            }
        }

        val content = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, userId).build(builderWithoutUrlNorFile)

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        cut.loadAudioMessage(content as RoomMessageEventContent.FileBased.Audio)

        eventually(300.milliseconds) { wasLoaded shouldBe false }
    }

    @Test
    fun `load - should not load draft into recorder if content type missing`() = runTest {
        var wasLoaded = false
        everySuspend { recorder.load(any()) } calls { wasLoaded = true }
        every { recorder.state } returns MutableStateFlow(AudioRecorder.State.Ready)

        val body = "cool audio"
        val contentType = null
        val platformMedia = PlatformMediaMock

        everySuspend { mediaServiceMock.getMedia(any(), any(), any(), any()) } returns Result.success(platformMedia)

        val builder: (suspend MessageBuilder.() -> Unit) = {
            audio(body = body, audio = platformMedia, type = contentType, duration = 69)
        }

        val content = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, userId).build(builder)

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        cut.loadAudioMessage(content as RoomMessageEventContent.FileBased.Audio)

        eventually(300.milliseconds) { wasLoaded shouldBe false }
    }

    @Test
    fun `load - should not load draft into recorder if file extension missing`() = runTest {
        var wasLoaded = false
        everySuspend { recorder.load(any()) } calls { wasLoaded = true }
        every { recorder.state } returns MutableStateFlow(AudioRecorder.State.Ready)

        val body = "cool audio"
        val fileName = "cool_audio"
        val contentType = ContentType("audio", "mp4")
        val platformMedia = PlatformMediaMock

        everySuspend { mediaServiceMock.getMedia(any(), any(), any(), any()) } returns Result.success(platformMedia)

        val builder: (suspend MessageBuilder.() -> Unit) = {
            audio(body = body, audio = platformMedia, fileName = fileName, type = contentType, duration = 69)
        }

        val content = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, userId).build(builder)

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        cut.loadAudioMessage(content as RoomMessageEventContent.FileBased.Audio)

        eventually(300.milliseconds) { wasLoaded shouldBe false }
    }

    @Test
    fun `delete - should delete Draft Message and close recorder`() = runTest {
        var wasClosed = false

        every { recorder.close() } calls { wasClosed = true }
        everySuspend { recorder.closeSuspending() } calls { wasClosed = true }
        every { recorder.state } returns MutableStateFlow(AudioRecorder.State.Ready)

        val cut = audioRecordingAreaViewModel(backgroundScope.coroutineContext)

        val body = "cool audio"
        val platformMedia = PlatformMediaMock

        roomServiceMock.setDraftMessage(roomId) { audio(body = body, audio = platformMedia, duration = 420) }

        draftMessage.value.shouldNotBeNull()

        cut.deleteAudioMessage()

        eventually(300.milliseconds) {
            draftMessage.value shouldBe null
            wasClosed shouldBe true
        }
    }

    private fun TestScope.audioRecordingAreaViewModel(
        coroutineContext: CoroutineContext,
        additionalModule: Module = module {
            single<MediaPlayerViewModelFactory> { mediaPlayerFactory }
            single<AudioRecorderHolder> { audioRecorderHolder }
        },
    ): AudioRecordingAreaViewModel {
        val di =
            koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock)) + additionalModule
                    )
                }
                .koin
        val cut =
            AudioRecordingAreaViewModelFactory.create(
                testMatrixClientViewModelContext(di = di, userId, coroutineContext),
                roomId,
                currentReply,
                { currentReply.value = null },
                Mutex(),
            )
        backgroundScope.launch { cut.recorder?.state?.collect() }
        backgroundScope.launch { cut.capturePlayer.collect() }
        return cut
    }
}
