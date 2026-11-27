package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.removeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@TrixnityMessengerCallApi
interface CallService : AutoCloseable {
    val session: StateFlow<CallSession?>

    fun register(call: CallSession): Boolean

    fun unregister(call: CallSession)
}

@TrixnityMessengerCallApi
internal class CallServiceImpl(private val navigation: RouteNavigation) : CallService {
    override val session = MutableStateFlow<CallSession?>(null)

    override fun register(call: CallSession): Boolean {
        if (!session.compareAndSet(null, call)) return false
        call.start { unregister(call) }
        return true
    }

    override fun unregister(call: CallSession) {
        if (session.compareAndSet(call, null)) {
            call.close()
            navigation.updateNavigation { removeCall(call.target) }
        }
    }

    override fun close() {
        session.value?.let(::unregister)
    }
}
