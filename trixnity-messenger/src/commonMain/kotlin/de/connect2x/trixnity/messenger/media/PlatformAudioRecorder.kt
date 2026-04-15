package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.util.ExperimentalTrixnityMessengerApi

@TrixnityMessengerPrivateApi
interface PlatformAudioRecorder: AutoCloseable {
    suspend fun start(): CommonAudioRecorder.CommonState.Recording?
}
