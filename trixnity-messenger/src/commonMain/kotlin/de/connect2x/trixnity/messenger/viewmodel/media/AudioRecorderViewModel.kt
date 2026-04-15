package de.connect2x.trixnity.messenger.viewmodel.media

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnStop
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.util.ExperimentalTrixnityMessengerApi
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@TrixnityMessengerPrivateApi
interface AudioRecorderViewModel: AudioRecorder {
    fun start()
}

@TrixnityMessengerPrivateApi
class AudioRecorderViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    recorder: AudioRecorder,
): MatrixClientViewModelContext by viewModelContext, AudioRecorder by recorder, AudioRecorderViewModel {
    init {
        doOnDestroy {
            close()
        }
        doOnStop {
            complete()
        }
        doOnPause {
            complete()
        }
    }

    override fun start() {
        coroutineScope.launch {
            startSuspending()
        }
    }
}
