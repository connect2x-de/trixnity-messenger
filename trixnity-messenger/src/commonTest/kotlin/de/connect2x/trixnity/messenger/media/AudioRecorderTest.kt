package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import de.connect2x.trixnity.utils.ByteArrayFlow
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

class AudioRecorderTest {
    private val platformAudioRecorder = mock<PlatformAudioRecorder>()
    private val clock = mock<Clock>()

    private val intoMediaStoreMock = { _: ByteArrayFlow ->
        AudioRecorder.State.Completed.MediaReference.Unencrypted("unused")
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
        resetMocks(platformAudioRecorder, clock)

        every { platformAudioRecorder.close() } returns Unit
    }

    @Test
    fun `ready - initial state is ready`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        var stateEmits = 0
        cut.state.onEach { stateEmits++ }.launchIn(backgroundScope)
        delay(1.seconds)

        cut.state.value shouldBe AudioRecorder.State.Ready
        stateEmits shouldBe 1
    }

    @Test
    fun `start when ready - when pressing start button then start recording`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = commonAudioRecorder(coroutineScope)

            val startTime = Clock.System.now()
            everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
                AudioRecorderImpl.State.Recording(
                    startTime,
                    { 5f },
                    { _ ->
                        AudioRecorderImpl.State.Completed(
                            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                            1.seconds,
                            1000L,
                            ContentType("audio", "ogg"),
                            "ogg",
                        ) {}
                    },
                )
            val nextTime = startTime + 5.seconds
            every { clock.now() } returns nextTime
            backgroundScope.launch { cut.startSuspending(intoMediaStoreMock) }
            delay(1.seconds)

            cut.state.value shouldBe AudioRecorder.State.Recording(5.seconds, 5f)
        }

    @Test
    fun `start - when starting in any state then close the platform recorder and possibly delete capture before starting`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = commonAudioRecorder(coroutineScope)

            val startTime = Clock.System.now()
            var captureDeleted = false
            everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
                AudioRecorderImpl.State.Recording(
                    startTime,
                    { 5F },
                    { _ ->
                        AudioRecorderImpl.State.Completed(
                            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                            1.seconds,
                            1000L,
                            ContentType("audio", "ogg"),
                            "ogg",
                        ) {
                            captureDeleted = true
                        }
                    },
                )
            var platformRecorderClosed = false
            every { platformAudioRecorder.close() } calls { platformRecorderClosed = true }
            every { clock.now() } returns (startTime + 5.seconds)

            backgroundScope.launch { cut.startSuspending(intoMediaStoreMock) }
            delay(1.seconds)
            platformRecorderClosed shouldBe true
            (cut.state.value is AudioRecorder.State.Recording) shouldBe true

            platformRecorderClosed = false
            backgroundScope.launch { cut.startSuspending(intoMediaStoreMock) }
            delay(1.seconds)
            captureDeleted shouldBe true
            platformRecorderClosed shouldBe true
            (cut.state.value is AudioRecorder.State.Recording) shouldBe true

            platformRecorderClosed = false
            captureDeleted = false
            backgroundScope.launch {
                cut.complete()
                cut.startSuspending(intoMediaStoreMock)
            }
            delay(1.seconds)
            captureDeleted shouldBe true
            platformRecorderClosed shouldBe true
            (cut.state.value is AudioRecorder.State.Recording) shouldBe true
        }

    @Test
    fun `recording - show increasing duration when recording`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        val startTime = Clock.System.now()
        everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
            AudioRecorderImpl.State.Recording(
                startTime,
                { 5f },
                { _ ->
                    AudioRecorderImpl.State.Completed(
                        AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                        1.seconds,
                        1000L,
                        ContentType("audio", "ogg"),
                        "ogg",
                    ) {}
                },
            )

        var nextTime = startTime
        fun increasingTime(): Instant {
            nextTime += 1.seconds
            return nextTime
        }
        every { clock.now() } calls { increasingTime() }
        backgroundScope.launch { cut.startSuspending(intoMediaStoreMock) }
        delay(5.seconds)

        when (val state = cut.state.value) {
            is AudioRecorder.State.Completed -> throw AssertionError("Should be Recording")
            AudioRecorder.State.Ready -> throw AssertionError("Should be Recording")
            is AudioRecorder.State.Recording -> state.duration shouldBeGreaterThan 5.seconds
        }
    }

    @Test
    fun `recording - show adjusting loudness when recording`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        val startTime = Clock.System.now()

        var nextLoudness = 0F
        fun increasingLoudness(): Float {
            nextLoudness += 1F
            return nextLoudness
        }
        everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
            AudioRecorderImpl.State.Recording(
                startTime,
                { increasingLoudness() },
                { _ ->
                    AudioRecorderImpl.State.Completed(
                        AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                        1.seconds,
                        1000L,
                        ContentType("audio", "ogg"),
                        "ogg",
                    ) {}
                },
            )

        every { clock.now() } returns (startTime + 5.seconds)
        backgroundScope.launch { cut.startSuspending(intoMediaStoreMock) }
        delay(5.seconds)

        when (val state = cut.state.value) {
            is AudioRecorder.State.Completed -> throw AssertionError("Should be Recording")
            AudioRecorder.State.Ready -> throw AssertionError("Should be Recording")
            is AudioRecorder.State.Recording -> state.loudness shouldBeGreaterThan 5F
        }
    }

    @Test
    fun `recording - should continue recording when loudness throws`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        val startTime = Clock.System.now()
        everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
            AudioRecorderImpl.State.Recording(
                startTime,
                { throw IllegalStateException() },
                { _ ->
                    AudioRecorderImpl.State.Completed(
                        AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                        1.seconds,
                        1000L,
                        ContentType("audio", "ogg"),
                        "ogg",
                    ) {}
                },
            )
        every { clock.now() } returns (startTime + 5.seconds)
        backgroundScope.launch { cut.startSuspending(intoMediaStoreMock) }
        delay(1.seconds)

        cut.state.value shouldBe AudioRecorder.State.Recording(5.seconds, 0f)
    }

    @Test
    fun `recording - should continue recording when loudness null`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        val startTime = Clock.System.now()
        everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
            AudioRecorderImpl.State.Recording(
                startTime,
                { null },
                { _ ->
                    AudioRecorderImpl.State.Completed(
                        AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                        1.seconds,
                        1000L,
                        ContentType("audio", "ogg"),
                        "ogg",
                    ) {}
                },
            )
        every { clock.now() } returns (startTime + 5.seconds)
        backgroundScope.launch { cut.startSuspending(intoMediaStoreMock) }
        delay(1.seconds)

        cut.state.value shouldBe AudioRecorder.State.Recording(5.seconds, 0f)
    }

    @Test
    fun `recording - when recording over maximum duration then complete automatically`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = commonAudioRecorder(coroutineScope)

            val startTime = Clock.System.now()
            var captureDeleted = false
            everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
                AudioRecorderImpl.State.Recording(
                    startTime,
                    { 5F },
                    { _ ->
                        AudioRecorderImpl.State.Completed(
                            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                            1.seconds,
                            1000L,
                            ContentType("audio", "ogg"),
                            "ogg",
                        ) {
                            captureDeleted = true
                        }
                    },
                )

            var nextTime = startTime
            fun increasingTime(): Instant {
                nextTime += 1.hours
                return nextTime
            }
            every { clock.now() } calls { increasingTime() }

            var platformRecorderClosed = false
            every { platformAudioRecorder.close() } calls { platformRecorderClosed = true }

            backgroundScope.launch {
                cut.startSuspending(intoMediaStoreMock)
                platformRecorderClosed = false
            }
            delay(1.seconds)

            captureDeleted shouldBe false
            platformRecorderClosed shouldBe false
            (cut.state.value is AudioRecorder.State.Completed) shouldBe true
        }

    @Test
    fun `complete when recording - when pressing stop button then complete recording`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = commonAudioRecorder(coroutineScope)

            val startTime = Clock.System.now()
            everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
                AudioRecorderImpl.State.Recording(
                    startTime,
                    { 5F },
                    { _ ->
                        AudioRecorderImpl.State.Completed(
                            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                            1.seconds,
                            1000L,
                            ContentType("audio", "ogg"),
                            "ogg",
                        ) {}
                    },
                )
            every { clock.now() } returns (startTime + 5.seconds)
            backgroundScope.launch {
                cut.startSuspending(intoMediaStoreMock)
                cut.complete()
            }
            delay(1.seconds)

            cut.state.value shouldBe
                AudioRecorder.State.Completed(AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"), 1.seconds, 1000L, ContentType("audio", "ogg"), "ogg")
        }

    @Test
    fun `complete - should reset to ready when complete throws`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        val startTime = Clock.System.now()
        everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
            AudioRecorderImpl.State.Recording(startTime, { 5F }, { _ -> throw IllegalStateException() })
        every { clock.now() } returns (startTime + 5.seconds)
        backgroundScope.launch {
            cut.startSuspending(intoMediaStoreMock)
            cut.complete()
        }
        delay(1.seconds)

        cut.state.value shouldBe AudioRecorder.State.Ready
    }

    @Test
    fun `completed - emit completed state only once`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        val startTime = Clock.System.now()
        everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
            AudioRecorderImpl.State.Recording(
                startTime,
                { 5F },
                { _ ->
                    AudioRecorderImpl.State.Completed(
                        AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                        1.seconds,
                        1000L,
                        ContentType("audio", "ogg"),
                        "ogg",
                    ) {}
                },
            )
        every { clock.now() } returns (startTime + 5.seconds)
        var stateEmits = 0
        cut.state.filterIsInstance<AudioRecorder.State.Completed>().onEach { stateEmits++ }.launchIn(backgroundScope)
        backgroundScope.launch {
            cut.startSuspending(intoMediaStoreMock)
            cut.complete()
        }
        delay(1.seconds)

        stateEmits shouldBe 1
    }

    @Test
    fun `close when completed - when sending or cancelling a voice message then close the platform recorder and delete capture and return to ready state`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = commonAudioRecorder(coroutineScope)

            val startTime = Clock.System.now()
            var captureDeleted = false
            everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
                AudioRecorderImpl.State.Recording(
                    startTime,
                    { 5F },
                    { _ ->
                        AudioRecorderImpl.State.Completed(
                            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                            1.seconds,
                            1000L,
                            ContentType("audio", "ogg"),
                            "ogg",
                        ) {
                            captureDeleted = true
                        }
                    },
                )
            var platformRecorderClosed = false
            every { platformAudioRecorder.close() } calls { platformRecorderClosed = true }
            every { clock.now() } returns (startTime + 5.seconds)
            backgroundScope.launch {
                cut.startSuspending(intoMediaStoreMock)
                cut.complete()
                cut.close()
            }
            delay(1.seconds)

            captureDeleted shouldBe true
            platformRecorderClosed shouldBe true
            cut.state.value shouldBe AudioRecorder.State.Ready
        }

    @Test
    fun `close - when closing in any state then close the platform recorder and possibly delete capture and return to ready state`() =
        runTestWithCoroutineScope { coroutineScope ->
            val cut = commonAudioRecorder(coroutineScope)

            val startTime = Clock.System.now()
            var captureDeleted = false
            everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
                AudioRecorderImpl.State.Recording(
                    startTime,
                    { 5F },
                    { _ ->
                        AudioRecorderImpl.State.Completed(
                            AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                            1.seconds,
                            1000L,
                            ContentType("audio", "ogg"),
                            "ogg",
                        ) {
                            captureDeleted = true
                        }
                    },
                )
            var platformRecorderClosed = false
            every { platformAudioRecorder.close() } calls { platformRecorderClosed = true }
            every { clock.now() } returns (startTime + 5.seconds)

            backgroundScope.launch {
                cut.startSuspending(intoMediaStoreMock)
                platformRecorderClosed = false
                cut.close()
            }
            delay(1.seconds)
            captureDeleted shouldBe true
            platformRecorderClosed shouldBe true
            cut.state.value shouldBe AudioRecorder.State.Ready

            platformRecorderClosed = false
            backgroundScope.launch { cut.close() }
            delay(1.seconds)
            platformRecorderClosed shouldBe true
            cut.state.value shouldBe AudioRecorder.State.Ready
        }

    @Test
    fun `close - should reset to ready when deleteCapture throws`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = commonAudioRecorder(coroutineScope)

        val startTime = Clock.System.now()
        everySuspend { platformAudioRecorder.start(intoMediaStoreMock) } returns
            AudioRecorderImpl.State.Recording(
                startTime,
                { 5F },
                { _ ->
                    AudioRecorderImpl.State.Completed(
                        AudioRecorder.State.Completed.MediaReference.Unencrypted("unused"),
                        1.seconds,
                        1000L,
                        ContentType("audio", "ogg"),
                        "ogg",
                    ) {
                        throw IllegalStateException()
                    }
                },
            )
        every { clock.now() } returns (startTime + 5.seconds)
        backgroundScope.launch {
            cut.startSuspending(intoMediaStoreMock)
            cut.complete()
            cut.close()
        }
        delay(1.seconds)

        cut.state.value shouldBe AudioRecorder.State.Ready
    }

    fun TestScope.commonAudioRecorder(coroutineScope: CoroutineScope): AudioRecorderImpl {
        val cut = AudioRecorderImpl(platformAudioRecorder, clock, coroutineScope)
        backgroundScope.launch { cut.state.collect() }
        return cut
    }
}
