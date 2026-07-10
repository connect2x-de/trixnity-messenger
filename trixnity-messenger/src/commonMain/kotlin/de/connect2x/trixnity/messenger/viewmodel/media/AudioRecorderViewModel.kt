package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnStop
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@TrixnityMessengerPrivateApi
interface AudioRecorderViewModel : AudioRecorder {
    fun start()
    fun completeNonSuspending()
}

class AudioRecorderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    recorder: AudioRecorder,
    private val roomId: RoomId
) :
    MatrixClientViewModelContext by viewModelContext, AudioRecorder by recorder, AudioRecorderViewModel {
    init {
        doOnDestroy { close() }
        doOnStop {
            coroutineScope.launch {
                complete()
            }
        }
        doOnPause {
            coroutineScope.launch {
                complete()
            }
        }
    }

    override fun start() {
        coroutineScope.launch {
            startSuspending { data ->
                val isEncryptedRoom = matrixClient.room.getById(roomId).first()?.encrypted == true
                if (isEncryptedRoom) {
                    AudioRecorder.State.Completed.MediaReference.Encrypted(
                        matrixClient.media.prepareUploadEncryptedMedia(data)
                    )
                } else {
                    AudioRecorder.State.Completed.MediaReference.Unencrypted(
                        matrixClient.media.prepareUploadMedia(data, null)
                    )
                }
            }
        }
    }

    override fun completeNonSuspending() {
        coroutineScope.launch {
            complete()
        }
    }
}
