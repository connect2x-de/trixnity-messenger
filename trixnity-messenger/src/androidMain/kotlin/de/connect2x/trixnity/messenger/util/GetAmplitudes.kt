package de.connect2x.trixnity.messenger.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Half.abs
import de.connect2x.trixnity.messenger.viewmodel.util.GetAmplitudes
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.client.media.PlatformMedia
import net.folivo.trixnity.client.media.okio.OkioPlatformMedia
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

private val log = KotlinLogging.logger { }

private fun lerp(a: Short, b: Short, t: Short) = a + (b - a) * t

private fun List<Short>.downsample(): Short = this.maxOf { abs(it) }

object GetAmplitudesImpl : GetAmplitudes {
    override suspend fun invoke(
        media: PlatformMedia,
        count: Int?,
        deleteFile: Boolean,
        normalizeWithMax: Boolean
    ): Result<MutableList<Float>> {
        require(media is OkioPlatformMedia) { "Platform media is required to be OkioPlatformMedia" }
        val temporaryFileResult = media.getTemporaryFile()
        if (temporaryFileResult.isFailure) {
            return Result.failure(requireNotNull(temporaryFileResult.exceptionOrNull()))
        }

        val temporaryFile = temporaryFileResult.getOrThrow()
        val closeFile: suspend () -> Unit = {
            if (deleteFile) {
                log.trace { "Delete temporary file from filesystem" }
                temporaryFile.delete()
            }
        }

        // We want to use the first audio track available in the media file. When no found, we can't extract the
        // amplitudes from the file.
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(temporaryFile.path.toString())
        val (trackIndex, trackFormat) = (0 until mediaExtractor.trackCount)
            .map { Pair(it, mediaExtractor.getTrackFormat(it)) }
            .firstOrNull { (_, format) -> format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            ?: run {
                closeFile()
                return Result.failure(IllegalStateException("Platform media is expected to contain audio tracks"))
            }
        mediaExtractor.selectTrack(trackIndex)

        // We don't want and need to store all amplitudes of the file in the memory. For this purpose, we calculate the
        // count of the file's PCM samples. We use this formula: (durationUs / 1.000.000) * sampleRateHz * channelCount
        val duration = requireNotNull(trackFormat.getLong(MediaFormat.KEY_DURATION))
        val sampleRate = requireNotNull(trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))
        val channelCount = requireNotNull(trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
        val totalAmplitudes = ((duration / 1_000_000.0) * sampleRate).toInt()
        val targetCount = count ?: totalAmplitudes
        val chunkSize = if (targetCount > totalAmplitudes) {
            targetCount / totalAmplitudes
        } else if (targetCount < totalAmplitudes) {
            totalAmplitudes / targetCount
        } else 1

        val mimeType = requireNotNull(trackFormat.getString(MediaFormat.KEY_MIME))
        val mediaCodec = MediaCodec.createDecoderByType(mimeType)
        mediaCodec.configure(trackFormat, null, null, 0)
        mediaCodec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var tempSampleBuffer: MutableList<Short> = mutableListOf()
        val outputList: MutableList<Short> = ArrayList(count ?: totalAmplitudes)
        while (true) {
            val inputIndex = mediaCodec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = requireNotNull(mediaCodec.getInputBuffer(inputIndex))
                val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)
                    mediaExtractor.advance()
                }
            }

            val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = requireNotNull(mediaCodec.getOutputBuffer(outputIndex))
                val chunk = ByteArray(bufferInfo.size)
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                outputBuffer.get(chunk)

                val shorts = ShortArray(chunk.size / 2)
                ByteBuffer.wrap(chunk)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(shorts)

                // When we have multiple channels, we run into the problem we can have duplicates of the same sample. To
                // prevent too much samples, we reduce by averaging the channels' amplitudes.
                when (channelCount) {
                    1 -> tempSampleBuffer.addAll(shorts.toTypedArray())
                    else -> for (index in shorts.indices step channelCount) {
                        val sample = (0 until channelCount).map { shorts.getOrNull(index + it) ?: 0 }.average()
                        tempSampleBuffer.add(sample.toInt().toShort())
                    }
                }

                while (tempSampleBuffer.size >= chunkSize) {
                    val chunk = tempSampleBuffer.subList(0, chunkSize)
                    tempSampleBuffer = tempSampleBuffer.drop(chunkSize).toMutableList()
                    outputList.addAll(chunk)

                    // TODO: Upsample and downsample
                }

                mediaCodec.releaseOutputBuffer(outputIndex, false)
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break
            }
        }

        mediaCodec.stop()
        mediaCodec.release()
        mediaExtractor.release()
        val max = if (normalizeWithMax) outputList.maxOfOrNull { abs(it.toInt()).toFloat() } ?: 1f else 1f
        return Result.success(outputList.map { it / max }.toMutableList())
    }
}
