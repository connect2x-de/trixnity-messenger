package de.connect2x.trixnity.messenger.viewmodel.media

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.media.AudioRecorderHolder
import de.connect2x.trixnity.messenger.media.PlatformMediaMock
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.util.ExperimentalTrixnityMessengerApi
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.AudioRecordingAreaViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.AudioRecordingAreaViewModelFactory
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class AudioRecordingAreaViewModelTest {
    val matrixClientMock = mock<MatrixClient>()
    val recorder = mock<AudioRecorderViewModel>()
    val mediaPlayerFactory = mock<MediaPlayerViewModelFactory>()
    val audioRecorderHolder = AudioRecorderHolder(recorder)
    val player = mock<MediaPlayerViewModel>()

    init {
        resetMocks(
            matrixClientMock,
            recorder,
            mediaPlayerFactory,
            player
        )

        every { mediaPlayerFactory.create(
            any(),
            any(),
            any(),
            any(),
            any()
        ) } returns player
        every { player.pause() } returns Unit
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `recorder unavailable - when recorder not available then player never initialized`() = runTest {
        val cut = audioRecordingAreaViewModel(
            backgroundScope.coroutineContext,
            additionalModule = module {
                single<MediaPlayerViewModelFactory> { mediaPlayerFactory }
            }
        )

        cut.capturePlayer.value shouldBe null
    }

    @Test
    fun `recorder unavailable - when recorder not available then sending not possible`() = runTest {
        var sent = false
        val cut = audioRecordingAreaViewModel(
            backgroundScope.coroutineContext,
            additionalModule = module {
                single<MediaPlayerViewModelFactory> { mediaPlayerFactory }
            },
            sendAudioMessageMock = { sent = true }
        )
        cut.sendAudioMessage()

        sent shouldBe false
    }

    @Test
    fun `recording - when recording then player not initialized`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> =
            MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        val cut = audioRecordingAreaViewModel(
            backgroundScope.coroutineContext,
        )

        recorderState.value = AudioRecorder.State.Recording(5.seconds, 5F)
        delay(1.seconds)
        cut.capturePlayer.value shouldBe null
    }

    @Test
    fun `completed - when completed then player initialized`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> =
            MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        val cut = audioRecordingAreaViewModel(
            backgroundScope.coroutineContext,
        )

        recorderState.value = AudioRecorder.State.Completed(
            PlatformMediaMock, 5.seconds, 1000L, ContentType("audio", "ogg")
        )
        delay(1.seconds)
        cut.capturePlayer.value shouldBe player
    }

    @Test
    fun `not completed - when not completed then old player paused`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> =
            MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        val cut = audioRecordingAreaViewModel(
            backgroundScope.coroutineContext,
        )

        recorderState.value = AudioRecorder.State.Completed(
            PlatformMediaMock, 5.seconds, 1000L, ContentType("audio", "ogg")
        )
        delay(1.seconds)
        recorderState.value = AudioRecorder.State.Ready
        delay(1.seconds)
        verify { player.pause() }
        cut.capturePlayer.value shouldBe null

        recorderState.value = AudioRecorder.State.Completed(
            PlatformMediaMock, 5.seconds, 1000L, ContentType("audio", "ogg")
        )
        delay(1.seconds)
        recorderState.value = AudioRecorder.State.Recording(5.seconds, 5F)
        delay(1.seconds)
        verify { player.pause() }
        cut.capturePlayer.value shouldBe null
    }

    @Test
    fun `send - should only send when completed`() = runTest {
        val recorderState: MutableStateFlow<AudioRecorder.State> =
            MutableStateFlow(AudioRecorder.State.Ready)
        every { recorder.state } returns recorderState

        var sent = false
        val cut = audioRecordingAreaViewModel(
            backgroundScope.coroutineContext,
            sendAudioMessageMock = { sent = true },
        )

        recorderState.value = AudioRecorder.State.Ready
        cut.sendAudioMessage()
        sent shouldBe false

        recorderState.value = AudioRecorder.State.Recording(5.seconds, 5F)
        cut.sendAudioMessage()
        sent shouldBe false

        recorderState.value = AudioRecorder.State.Completed(
            PlatformMediaMock, 5.seconds, 1000L, ContentType("audio", "ogg")
        )
        cut.sendAudioMessage()
        sent shouldBe true
    }

    private fun TestScope.audioRecordingAreaViewModel(
        coroutineContext: CoroutineContext,
        additionalModule: Module = module {
            single<MediaPlayerViewModelFactory> { mediaPlayerFactory }
            single<AudioRecorderHolder> { audioRecorderHolder }
        },
        sendAudioMessageMock: () -> Unit = {},
    ): AudioRecordingAreaViewModel {
        val userId = UserId("")
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules(
                    mapOf(userId to matrixClientMock)
                ) + additionalModule
            )
        }.koin
        val cut = AudioRecordingAreaViewModelFactory.create(
            testMatrixClientViewModelContext(
                di = di,
                userId,
                coroutineContext
            )
        ) { sendAudioMessageMock() }
        backgroundScope.launch { cut.recorder?.state?.collect() }
        backgroundScope.launch { cut.capturePlayer.collect() }
        return cut
    }
}
