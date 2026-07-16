package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.utils.ByteArrayFlow

@TrixnityMessengerPrivateApi
interface PlatformAudioRecorder : AutoCloseable {
    suspend fun start(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference
    ): AudioRecorderImpl.State.Recording?

    suspend fun load(state: AudioRecorder.State.Completed): AudioRecorderImpl.State.Completed?
}
