package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnStop
import de.connect2x.trixnity.client.media
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.media.AudioRecorder.State
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@TrixnityMessengerPrivateApi
interface AudioRecorderViewModel {
    val state: StateFlow<State>

    fun start()

    fun complete()

    suspend fun load(state: State.Completed)

    suspend fun close()
}

class AudioRecorderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val recorder: AudioRecorder,
    private val roomId: RoomId,
) : MatrixClientViewModelContext by viewModelContext, AudioRecorderViewModel {
    override val state: StateFlow<State> = recorder.state

    init {
        doOnDestroy { coroutineScope.launch { close() } }
        doOnStop { coroutineScope.launch { complete() } }
        doOnPause { coroutineScope.launch { complete() } }
    }

    override fun start() {
        coroutineScope.launch {
            recorder.start { data ->
                val isEncryptedRoom = matrixClient.room.getById(roomId).first()?.encrypted == true
                if (isEncryptedRoom) {
                    State.Completed.MediaReference.Encrypted(matrixClient.media.prepareUploadEncryptedMedia(data))
                } else {
                    State.Completed.MediaReference.Unencrypted(matrixClient.media.prepareUploadMedia(data, null))
                }
            }
        }
    }

    override fun complete() {
        coroutineScope.launch { recorder.complete() }
    }

    override suspend fun load(state: State.Completed) {
        recorder.load(state)
    }

    override suspend fun close() {
        recorder.closeSuspending()
    }
}
