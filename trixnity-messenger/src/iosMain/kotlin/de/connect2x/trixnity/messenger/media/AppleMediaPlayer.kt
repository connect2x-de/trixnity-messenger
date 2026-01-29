package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.darwin.NSEC_PER_SEC
import kotlin.time.Duration.Companion.milliseconds

internal class AppleMediaPlayer(private val coroutineScope: CoroutineScope) : MediaPlayer {
    private val _playingItem: MutableStateFlow<ApplePlayerItem?> = MutableStateFlow(null)
    private val playerMutex: Mutex = Mutex()
    private var player: AVPlayer? = null
    private var timeObserver: Any? = null

    override val state: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
    override val playingItem: StateFlow<MediaPlayer.Item?> = _playingItem.asStateFlow()

    override fun open(media: RoomMessageTimelineElementViewModel.FileBased<*>): MediaPlayer.Item =
        ApplePlayerItem(media, state, playerMutex, coroutineScope, _playingItem, ::withPlayer)

    override fun close() {
        timeObserver?.let {
            player?.removeTimeObserver(it)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun withPlayer(item: AVPlayerItem?, closure: (AVPlayer) -> Unit) {
        if (item != null) {
            player?.replaceCurrentItemWithPlayerItem(item)
            if (player == null) {
                player = AVPlayer.playerWithPlayerItem(item)

                val interval = CMTimeMakeWithSeconds(0.125, NSEC_PER_SEC.toInt()) // 125ms
                timeObserver = player?.addPeriodicTimeObserverForInterval(interval, null) { time ->
                    time.useContents {
                        if (timescale <= 0)
                            return@useContents

                        val elapsedTime = (this.value * 1000 / this.timescale).milliseconds
                        _playingItem.value?.elapsedTime?.value = elapsedTime
                    }
                }
            }
        }

        if (player != null) {
            closure(requireNotNull(player))
        }
    }
}
