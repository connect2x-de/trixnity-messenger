package de.connect2x.trixnity.messenger.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class RecordingTimingTest {
    @Test
    fun delayedChunksUseTimestamps() {
        val timing = RecordingTiming(100.0)
        timing.update(1100.0)
        timing.update(3700.0)

        assertEquals(3600.milliseconds, timing.duration)
    }

    @Test
    fun finalChunkIncludesShortRecording() {
        val timing = RecordingTiming(100.0)
        timing.stop(350.0)
        timing.update(360.0)

        assertEquals(250.milliseconds, timing.duration)
    }

    @Test
    fun finalChunkDeliveryDelayDoesNotExtendRecording() {
        val timing = RecordingTiming(100.0)
        timing.update(1100.0)
        timing.stop(1600.0)
        timing.update(5000.0)

        assertEquals(1500.milliseconds, timing.duration)
    }

    @Test
    fun streamEndingBeforeStopPreservesLastTimestamp() {
        val timing = RecordingTiming(100.0)
        timing.update(1500.0)
        timing.stop(3000.0)

        assertEquals(1400.milliseconds, timing.duration)
    }
}
