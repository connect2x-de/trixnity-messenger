package de.connect2x.trixnity.messenger.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresPermission
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.media.CommonAudioRecorder.CommonState
import de.connect2x.trixnity.messenger.media.CommonAudioRecorder.sampleToPublicState
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.requestPermissionActivityResult
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okio.FileSystem
import kotlin.time.Clock


internal class AndroidAudioRecorder(
    private val clock: Clock,
    private val fileSystem: FileSystem,
    private val getContext: ContextGetter,
    private val getActivity: ActivityGetter,
    parentScope: CoroutineScope
) : AudioRecorder {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.media.AndroidAudioRecorder")
    private val tempFilePath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "voice_messages"
    private val commonState: MutableStateFlow<CommonState> =
        MutableStateFlow(CommonState.Ready)
    
    override val state: StateFlow<AudioRecorder.State> =
        commonState.sampleToPublicState(clock)
            .onEach { CommonAudioRecorder.onMaxDuration(it) { complete() } }
            .stateIn(parentScope, SharingStarted.WhileSubscribed(), AudioRecorder.State.Ready)

    var requestPermission: ActivityResultLauncher<String>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun startSuspending() {
        fun requestPermission() {
            if (requestPermission == null) {
                requestPermission = requestPermissionActivityResult(
                    getActivity(),
                    Manifest.permission.RECORD_AUDIO,
                    "Microphone"
                )
            }
            requestPermission?.launch(Manifest.permission.RECORD_AUDIO)
        }

        close()
        when (getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED ->
                startRecorder()
            PackageManager.PERMISSION_DENIED -> {
                requestPermission()
            }
        }
    }

    override fun complete() {
        commonState.update {
            CommonAudioRecorder.complete(it)
        }
    }

    override fun close() {
        requestPermission?.unregister()
        commonState.update {
            CommonAudioRecorder.close(it)
        }
    }

    private suspend fun startRecorder() {
        requestPermission?.unregister()
        withContext(Dispatchers.IO) {
            val recorder =
                if (Build.VERSION.SDK_INT >= 31) {
                    MediaRecorder(getContext())
                } else {
                    MediaRecorder()
                }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)

            val format =
                if (Build.VERSION.SDK_INT >= 29) {
                    CommonAudioRecorder.Format(
                        MediaRecorder.OutputFormat.OGG,
                        MediaRecorder.AudioEncoder.OPUS,
                        CommonAudioRecorder.Format.SampleRateHz.OPUS_SAMPLING_RATE_HZ,
                        ContentType.Audio.OGG
                    )
                } else {
                    CommonAudioRecorder.Format(
                        MediaRecorder.OutputFormat.AMR_WB,
                        MediaRecorder.AudioEncoder.AMR_WB,
                        CommonAudioRecorder.Format.SampleRateHz.AMR_WB_SAMPLING_RATE_HZ,
                        CommonAudioRecorder.Format.amrWbContentType
                    )
                }
            recorder.setOutputFormat(format.container)
            recorder.setAudioEncoder(format.encoder)
            recorder.setOutputFile(tempFilePath.toString())
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(format.sampleRate.value)

            recorder.prepare()
            log.warn { "Start audio record" }
            recorder.start()

            val nextState =
                CommonState.Recording(
                    start = clock.now(),
                    loudness = {
                        recorder.maxAmplitude.toFloat()
                    },
                    complete = { recordingState ->
                        try {
                            recorder.stop()
                            val capture = ReadOnlyFileOkioPlatformMedia(tempFilePath, fileSystem)
                            CommonState.Completed(
                                capture,
                                duration = clock.now() - recordingState.start,
                                sizeBytes = fileSystem.metadata(tempFilePath).size,
                                contentType = format.contentType
                            ) {
                                fileSystem.delete(tempFilePath)
                            }
                        } finally {
                            recorder.release()
                        }
                    }
                )
            commonState.value =
                CommonAudioRecorder.withCatchCallbacks(nextState)
        }
    }
}
