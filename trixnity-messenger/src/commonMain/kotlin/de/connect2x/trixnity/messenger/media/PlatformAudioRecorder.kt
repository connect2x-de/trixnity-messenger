package de.connect2x.trixnity.messenger.media

interface PlatformAudioRecorder: AutoCloseable {
    suspend fun start(): CommonAudioRecorder.CommonState.Recording?
}
