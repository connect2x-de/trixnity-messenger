package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallService
import de.connect2x.trixnity.messenger.call.CallSession
import de.connect2x.trixnity.messenger.call.CallSessionState
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.restoreCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@TrixnityMessengerCallApi
interface CallControlsViewModelFactory {
    fun create(coroutineScope: CoroutineScope): CallControlsViewModel
}

@TrixnityMessengerCallApi
interface CallControlsViewModel {
    val state: StateFlow<CallSessionState?>

    fun leave()

    fun toggleMicrophone()

    fun openFullscreenCall()
}

@TrixnityMessengerCallApi
internal class CallControlsViewModelFactoryImpl(
    private val calls: CallService,
    private val navigation: RouteNavigation,
) : CallControlsViewModelFactory {
    override fun create(coroutineScope: CoroutineScope): CallControlsViewModel {
        return CallControlsViewModelImpl(
            coroutineScope = coroutineScope,
            calls = calls,
            onOpenFullscreenCall = navigation.navigationCallback { session -> restoreCall(session.target) },
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@TrixnityMessengerCallApi
internal class CallControlsViewModelImpl(
    coroutineScope: CoroutineScope,
    private val calls: CallService,
    private val onOpenFullscreenCall: (CallSession) -> Unit,
) : CallControlsViewModel {
    override val state =
        calls.session
            .flatMapLatest { it?.state ?: flowOf(null) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), calls.session.value?.state?.value)

    override fun leave() {
        calls.session.value?.let(calls::unregister)
    }

    override fun toggleMicrophone() {
        calls.session.value?.toggleMicrophone()
    }

    override fun openFullscreenCall() {
        calls.session.value?.let(onOpenFullscreenCall)
    }
}
