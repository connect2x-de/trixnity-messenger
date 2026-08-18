package de.connect2x.trixnity.messenger.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresPermission
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.media.AudioRecorderImpl.Format.BitRate
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.requestRecordPermissionActivityResult
import de.connect2x.trixnity.utils.ByteArrayFlow
import de.connect2x.trixnity.utils.readByteArrayFlow
import io.ktor.http.*
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem

internal class AndroidAudioRecorder(
    private val clock: Clock,
    private val fileSystem: FileSystem,
    private val getContext: ContextGetter,
    private val getActivity: ActivityGetter,
    private val i18n: I18n,
) : PlatformAudioRecorder {
    private val log = Logger("de.connect2x.trixnity.messenger.media.AndroidAudioRecorder")

    private val tempFilePath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "voice_messages"
    private val audioFileExtension = "m4a"

    var registeredRequestPermission: ActivityResultLauncher<String>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun start(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference
    ): PlatformAudioRecorder.StartResult {
        fun requestPermission() {
            registeredRequestPermission?.unregister()
            registeredRequestPermission =
                requestRecordPermissionActivityResult(
                    getActivity(),
                    i18n.audioRecordingManuallyGiveMicrophonePermissionPrompt(),
                )
            registeredRequestPermission?.launch(Manifest.permission.RECORD_AUDIO)
        }

        return when (getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> startRecorder(intoMediaStore)
            PackageManager.PERMISSION_DENIED -> {
                requestPermission()
                PlatformAudioRecorder.StartResult.RequestedPermissions
            }

            else ->
                // should never be reached
                PlatformAudioRecorder.StartResult.Failure(i18n.genericRecordingError())
        }
    }

    override fun close() {
        registeredRequestPermission?.unregister()
    }

    private suspend fun startRecorder(
        intoMediaStore: suspend (ByteArrayFlow) -> AudioRecorder.State.Completed.MediaReference
    ): PlatformAudioRecorder.StartResult {
        registeredRequestPermission?.unregister()
        var releaseRecorder: (() -> Unit)? = null
        return try {
            withContext(Dispatchers.IO) {
                val recorder =
                    if (Build.VERSION.SDK_INT >= 31) {
                        MediaRecorder(getContext())
                    } else {
                        MediaRecorder()
                    }
                releaseRecorder = recorder::release
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)

                val format =
                    AudioRecorderImpl.Format(
                        MediaRecorder.OutputFormat.MPEG_4,
                        MediaRecorder.AudioEncoder.HE_AAC,
                        AudioRecorderImpl.Format.SampleRateHz.AAC_SAMPLING_RATE_HZ,
                        BitRate.AAC_BIT_RATE,
                        ContentType.Audio.MP4,
                    )
                recorder.setOutputFormat(format.container)
                recorder.setAudioEncoder(format.encoder)
                recorder.setOutputFile(tempFilePath.toString())
                recorder.setAudioChannels(1)
                recorder.setAudioSamplingRate(format.sampleRate.value)
                recorder.setAudioEncodingBitRate(format.bitRate.value)

                recorder.prepare()
                recorder.start()

                val failure =
                    AudioRecorderImpl.genericFailureOnError(i18n) { setFailure ->
                        recorder.setOnErrorListener { _, errorCode, _ ->
                            val logMessage =
                                when (errorCode) {
                                    MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN ->
                                        "Unknown error from Android API recorder while recording"
                                    MediaRecorder.MEDIA_ERROR_SERVER_DIED -> "Media server died while recording"
                                    else -> "Unexpected error while recording"
                                }
                            log.error { logMessage }
                            setFailure()
                        }
                    }

                val start = clock.now()
                PlatformAudioRecorder.StartResult.Success(
                    AudioRecorderImpl.State.Recording(
                        start = start,
                        loudness = { recorder.maxAmplitude.toFloat() },
                        complete = {
                            try {
                                recorder.stop()
                                val fileData = fileSystem.readByteArrayFlow(tempFilePath)
                                if (fileData != null) {
                                    val media = intoMediaStore(fileData)
                                    Result.success(
                                        AudioRecorderImpl.State.Completed(
                                            media,
                                            duration = clock.now() - start,
                                            sizeBytes = fileSystem.metadata(tempFilePath).size,
                                            contentType = format.contentType,
                                            fileExtension = audioFileExtension,
                                        )
                                    )
                                } else {
                                    log.warn { "Reading recording file from file system failed" }
                                    Result.failure(Throwable(i18n.genericRecordingError()))
                                }
                            } finally {
                                fileSystem.delete(tempFilePath)
                                recorder.release()
                            }
                        },
                        failure = failure,
                    )
                )
            }
        } catch (e: Throwable) {
            log.error(e) { "Unexpected error while starting recording" }
            releaseRecorder?.invoke()
            fileSystem.delete(tempFilePath)
            PlatformAudioRecorder.StartResult.Failure(i18n.genericRecordingError())
        }
    }
}
