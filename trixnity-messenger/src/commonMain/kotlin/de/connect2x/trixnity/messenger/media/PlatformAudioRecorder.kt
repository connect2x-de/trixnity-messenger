package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface PlatformAudioRecorder : AutoCloseable {
    suspend fun start(): AudioRecorderImpl.State.Recording?

    suspend fun load(state: AudioRecorder.State.Completed): AudioRecorderImpl.State.Completed?
}
