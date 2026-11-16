package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallService
import de.connect2x.trixnity.messenger.call.CallSession
import de.connect2x.trixnity.messenger.call.CallSessionState
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

@TrixnityMessengerCallApi
interface RoomCallViewModelFactory {
    fun create(viewModelContext: ViewModelContext, target: CallTarget, onBack: () -> Unit): RoomCallViewModel {
        return RoomCallViewModelImpl(
            viewModelContext = viewModelContext,
            target = target,
            calls = viewModelContext.get<CallService>(),
            onBack = onBack,
        )
    }

    companion object : RoomCallViewModelFactory
}

@TrixnityMessengerCallApi
interface RoomCallViewModel {
    val state: StateFlow<CallSessionState?>

    fun back()

    fun leave()

    fun toggleMicrophone()

    fun toggleCamera()
}

@OptIn(ExperimentalCoroutinesApi::class)
@TrixnityMessengerCallApi
internal class RoomCallViewModelImpl(
    viewModelContext: ViewModelContext,
    private val target: CallTarget,
    private val calls: CallService,
    private val onBack: () -> Unit,
) : RoomCallViewModel, ViewModelContext by viewModelContext {
    override val state =
        calls.session
            .flatMapLatest { session -> session?.takeIf { it.target == target }?.state ?: flowOf(null) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    init {
        coroutineScope.launch { if (calls.session.value?.target != target) onBack() }
    }

    private fun currentCall(): CallSession? {
        return calls.session.value?.takeIf { it.target == target }
    }

    override fun back() {
        onBack()
    }

    override fun leave() {
        currentCall()?.let(calls::unregister)
    }

    override fun toggleMicrophone() {
        currentCall()?.toggleMicrophone()
    }

    override fun toggleCamera() {
        currentCall()?.toggleCamera()
    }
}
