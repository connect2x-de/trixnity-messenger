package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.i18n.I18n
import kotlinx.coroutines.CoroutineScope

@TrixnityMessengerCallApi
interface CallSessionFactory {
    fun create(target: CallTarget): CallSession
}

@TrixnityMessengerCallApi
@OptIn(MSC4143::class)
internal class CallSessionFactoryImpl(
    private val coroutineScope: CoroutineScope,
    private val matrixClients: MatrixClients,
    private val mediaFactory: CallMediaFactory,
    private val signallingFactory: CallSignallingFactory,
    private val i18n: I18n,
) : CallSessionFactory {
    override fun create(target: CallTarget): CallSession {
        return CallSessionImpl(
            parentScope = coroutineScope,
            target = target,
            mediaFactory = mediaFactory,
            createSignalling = { scope, onLeave ->
                signallingFactory.create(
                    coroutineScope = scope,
                    matrixClient = checkNotNull(matrixClients.value[target.userId]),
                    roomId = target.roomId,
                    slotId = target.slotId,
                    onLeave = onLeave,
                )
            },
            matrixClients = matrixClients,
            i18n = i18n,
        )
    }
}
