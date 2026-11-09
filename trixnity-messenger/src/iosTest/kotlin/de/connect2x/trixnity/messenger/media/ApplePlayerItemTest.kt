package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.client.media.okio.OkioPlatformMedia
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.util.toNSUrl
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSUUID

@OptIn(ExperimentalCoroutinesApi::class)
class ApplePlayerItemTest {
    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `should reset the previous playing item when another item starts`() = runTest {
        withItems { player, first, second ->
            first.play()
            first.elapsedTime.value = 3.seconds
            second.seekTo(2.seconds)

            second.play()

            first.state.value shouldBe MediaPlayer.Item.State.Ready
            first.elapsedTime.value shouldBe Duration.ZERO
            second.state.value shouldBe MediaPlayer.Item.State.Playing
            second.elapsedTime.value shouldBe 2.seconds
            player.currentItemPlaying.value shouldBe second

            first.play()

            first.elapsedTime.value shouldBe Duration.ZERO
            second.elapsedTime.value shouldBe Duration.ZERO
            player.currentItemPlaying.value shouldBe first
        }
    }

    @Test
    fun `should reset the previous paused item when another item starts`() = runTest {
        withItems { player, first, second ->
            first.play()
            first.elapsedTime.value = 3.seconds
            first.pause()
            first.elapsedTime.value shouldBe 3.seconds

            second.play()

            first.state.value shouldBe MediaPlayer.Item.State.Ready
            first.elapsedTime.value shouldBe Duration.ZERO
            player.currentItemPlaying.value shouldBe second
        }
    }

    @Test
    fun `should preserve the position when resuming the same item`() = runTest {
        withItems { player, first, _ ->
            first.seekTo(3.seconds)
            first.play()
            first.pause()

            first.play()

            first.elapsedTime.value shouldBe 3.seconds
            first.state.value shouldBe MediaPlayer.Item.State.Playing
            player.currentItemPlaying.value shouldBe first
        }
    }

    @Test
    fun `should preserve the position when playing an already playing item`() = runTest {
        withItems { player, first, _ ->
            first.play()
            first.elapsedTime.value = 3.seconds

            first.play()

            first.elapsedTime.value shouldBe 3.seconds
            first.state.value shouldBe MediaPlayer.Item.State.Playing
            player.currentItemPlaying.value shouldBe first
        }
    }

    @Test
    fun `should not reset the previous item when the next item cannot play`() = runTest {
        withItems { player, first, second ->
            first.play()
            first.elapsedTime.value = 3.seconds
            second.state.value = MediaPlayer.Item.State.Failed("Playback unavailable")

            second.play()

            first.elapsedTime.value shouldBe 3.seconds
            first.state.value shouldBe MediaPlayer.Item.State.Playing
            player.currentItemPlaying.value shouldBe first
        }
    }

    private suspend fun TestScope.withItems(
        test: suspend (AppleMediaPlayer, ApplePlayerItem, ApplePlayerItem) -> Unit
    ) {
        val tempFile =
            object : OkioPlatformMedia.TemporaryFile {
                override val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "${NSUUID().UUIDString}.wav"

                override suspend fun delete() {
                    FileSystem.SYSTEM.delete(path)
                }
            }
        FileSystem.SYSTEM.write(tempFile.path) {
            val sampleRate = 8000
            val dataSize = sampleRate * 2 * 10
            writeUtf8("RIFF")
            writeIntLe(36 + dataSize)
            writeUtf8("WAVEfmt ")
            writeIntLe(16)
            writeShortLe(1)
            writeShortLe(1)
            writeIntLe(sampleRate)
            writeIntLe(sampleRate * 2)
            writeShortLe(2)
            writeShortLe(16)
            writeUtf8("data")
            writeIntLe(dataSize)
            write(ByteArray(dataSize))
        }
        val player = AppleMediaPlayer(backgroundScope)
        val asset = AVURLAsset.URLAssetWithURL(tempFile.path.toNSUrl(), null)
        val first = ApplePlayerItem("first", "first.wav", 10.seconds, asset, tempFile, backgroundScope, player)
        val second = ApplePlayerItem("second", "second.wav", 10.seconds, asset, tempFile, backgroundScope, player)
        try {
            test(player, first, second)
        } finally {
            first.pause()
            second.pause()
            first.close()
            second.close()
            runCurrent()
            player.close()
            tempFile.delete()
        }
    }
}
