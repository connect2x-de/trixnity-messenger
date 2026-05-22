package de.connect2x.trixnity.messenger.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresPermission
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.requestRecordPermissionActivityResult
import io.ktor.http.ContentType
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
    private val tempFilePath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "voice_messages"

    var registeredRequestPermission: ActivityResultLauncher<String>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun start(): AudioRecorderImpl.State.Recording? {
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
            PackageManager.PERMISSION_GRANTED -> startRecorder()
            PackageManager.PERMISSION_DENIED -> {
                requestPermission()
                null
            }
            else -> null
        }
    }

    override fun close() {
        registeredRequestPermission?.unregister()
    }

    private suspend fun startRecorder(): AudioRecorderImpl.State.Recording {
        registeredRequestPermission?.unregister()
        return withContext(Dispatchers.IO) {
            val recorder =
                if (Build.VERSION.SDK_INT >= 31) {
                    MediaRecorder(getContext())
                } else {
                    MediaRecorder()
                }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)

            val format =
                if (Build.VERSION.SDK_INT >= 29) {
                    AudioRecorderImpl.Format(
                        MediaRecorder.OutputFormat.OGG,
                        MediaRecorder.AudioEncoder.OPUS,
                        AudioRecorderImpl.Format.SampleRateHz.OPUS_SAMPLING_RATE_HZ,
                        ContentType.Audio.OGG,
                    )
                } else {
                    AudioRecorderImpl.Format(
                        MediaRecorder.OutputFormat.AMR_WB,
                        MediaRecorder.AudioEncoder.AMR_WB,
                        AudioRecorderImpl.Format.SampleRateHz.AMR_WB_SAMPLING_RATE_HZ,
                        AudioRecorderImpl.Format.amrWbContentType,
                    )
                }
            recorder.setOutputFormat(format.container)
            recorder.setAudioEncoder(format.encoder)
            recorder.setOutputFile(tempFilePath.toString())
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(format.sampleRate.value)

            recorder.prepare()
            recorder.start()

            AudioRecorderImpl.State.Recording(
                start = clock.now(),
                loudness = { recorder.maxAmplitude.toFloat() },
                complete = { recordingState ->
                    try {
                        recorder.stop()
                        val capture = ReadOnlyFileOkioPlatformMedia(tempFilePath, fileSystem)
                        AudioRecorderImpl.State.Completed(
                            capture,
                            duration = clock.now() - recordingState.start,
                            sizeBytes = fileSystem.metadata(tempFilePath).size,
                            contentType = format.contentType,
                        ) {
                            fileSystem.delete(tempFilePath)
                        }
                    } finally {
                        recorder.release()
                    }
                },
            )
        }
    }
}
