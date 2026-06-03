package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnStop
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.launch

@TrixnityMessengerPrivateApi
interface AudioRecorderViewModel : AudioRecorder {
    fun start()
    fun completeNonSuspending()
}

class AudioRecorderViewModelImpl(viewModelContext: MatrixClientViewModelContext, recorder: AudioRecorder) :
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
        coroutineScope.launch { startSuspending() }
    }

    override fun completeNonSuspending() {
        coroutineScope.launch {
            complete()
        }
    }
}
