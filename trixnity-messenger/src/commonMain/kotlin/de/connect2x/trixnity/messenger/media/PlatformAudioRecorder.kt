package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.utils.ByteArrayFlow

/** In order to record audio messages, implement an audio recorder for the specific platform. */
@TrixnityMessengerPrivateApi
interface PlatformAudioRecorder : AutoCloseable {
    /**
     * Start recording the audio message.
     *
     * @param intoMediaStore Use this to stream the recording chunk-wise into the
     *   [de.connect2x.trixnity.client.media.MediaStore]. We do this so that we do not have to keep big files in memory.
     *
     * When in an encrypted room then this also has the advantage of not leaking any unencrypted audio (e.g. into the
     * file system).
     *
     * If you cannot stream chunk-wise then writing into a file first is also possible. You then loose all advantages
     * listed above.
     *
     * @return [AudioRecorderImpl.State.Recording] if successful or `null` if starting the recording failed
     */
    suspend fun start(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference
    ): AudioRecorderImpl.State.Recording?

    /** Is used for drafts. */
    suspend fun load(state: AudioRecorder.State.Completed): AudioRecorderImpl.State.Completed?
}
