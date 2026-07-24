package de.connect2x.trixnity.messenger.media

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.readByteArrayFlow
import io.ktor.http.*
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.posix.pow

internal class AppleAudioRecorder(private val clock: Clock, private val fileSystem: FileSystem) :
    PlatformAudioRecorder {
    private val log = Logger("de.connect2x.trixnity.messenger.media.AppleAudioRecorder")

    private val audioFileExtension = "m4a"

    override suspend fun start(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference
    ): AudioRecorderImpl.State.Recording? {
        val granted = requestMicrophonePermission()
        return if (granted) {
            configureSession()
            startRecording(intoMediaStore)
        } else {
            null
        }
    }

    override suspend fun load(state: AudioRecorder.State.Completed): AudioRecorderImpl.State.Completed {
        return AudioRecorderImpl.State.Completed(
            capture = state.media,
            duration = state.duration,
            sizeBytes = state.sizeBytes,
            contentType = state.contentType,
            fileExtension = state.fileExtension,
        ) {}
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun close() {
        AVAudioSession.sharedInstance().setActive(false, error = null)
    }

    private suspend fun requestMicrophonePermission(): Boolean = suspendCancellableCoroutine { cont ->
        val session = AVAudioSession.sharedInstance()

        when (session.recordPermission()) {
            AVAudioSessionRecordPermissionGranted -> {
                cont.resume(true)
            }
            AVAudioSessionRecordPermissionDenied -> {
                openSettings()
                cont.resume(session.recordPermission() == AVAudioSessionRecordPermissionGranted)
            }

            AVAudioSessionRecordPermissionUndetermined -> {
                session.requestRecordPermission { granted -> cont.resume(granted) }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun startRecording(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference
    ): AudioRecorderImpl.State.Recording? {
        val settings =
            mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to AudioRecorderImpl.Format.SampleRateHz.AAC_SAMPLING_RATE_HZ.value,
                AVNumberOfChannelsKey to 1,
                AVEncoderAudioQualityKey to AVAudioQualityHigh,
            )

        val path =
            NSSearchPathForDirectoriesInDomains(
                    directory = NSDocumentDirectory,
                    domainMask = NSUserDomainMask,
                    expandTilde = true,
                )
                .first() as String

        val file = "$path/voice_message.m4a".toPath(normalize = true)
        val url = NSURL.fileURLWithPath(file.toString())

        val audioRecorder: AVAudioRecorder
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            audioRecorder = AVAudioRecorder(uRL = url, settings = settings, error = errorPtr.ptr)
            errorPtr.value?.let {
                log.error { "AudioRecorder init error: ${it.localizedDescription}" }
                return null
            }
        }

        audioRecorder.meteringEnabled = true
        val prepareToRecord = audioRecorder.prepareToRecord()
        if (prepareToRecord.not()) return null
        val record = audioRecorder.record()
        if (record.not()) return null

        return AudioRecorderImpl.State.Recording(
            start = clock.now(),
            loudness = {
                audioRecorder.updateMeters()
                val dbFS = audioRecorder.averagePowerForChannel(0u) // do not use peak -> updates are weird
                // we need to convert dbFS to linear 16bit PCM values (like from Android)
                val amplitude =
                    if (dbFS <= -160.0) {
                        0f
                    } else {
                        (32767.0 * pow(10.0, dbFS / 20.0)).toFloat()
                    }
                amplitude
            },
            complete = { recordingState ->
                try {
                    audioRecorder.stop()
                    val fileData = fileSystem.readByteArrayFlow(file)
                    if (fileData != null) {
                        val media = intoMediaStore(fileData)
                        AudioRecorderImpl.State.Completed(
                            media,
                            duration = clock.now() - recordingState.start,
                            sizeBytes = fileSystem.metadata(file).size,
                            contentType = ContentType.Audio.MP4,
                            fileExtension = audioFileExtension,
                        ) {
                            fileSystem.delete(file)
                        }
                    } else {
                        null
                    }
                } finally {
                    fileSystem.delete(file)
                    AVAudioSession.sharedInstance().setActive(false, error = null)
                }
            },
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun configureSession() {
        val session = AVAudioSession.sharedInstance()

        session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
        session.setActive(true, error = null)
    }

    private fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}
