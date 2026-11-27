package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotId
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.CoroutineScope

@TrixnityMessengerCallApi
@OptIn(MSC4143::class)
internal class CallSignallingFactory(
    private val configuration: MatrixMessengerConfiguration,
    private val initials: Initials,
    private val roomName: RoomName,
    private val i18n: I18n,
) {
    fun create(
        coroutineScope: CoroutineScope,
        matrixClient: MatrixClient,
        roomId: RoomId,
        slotId: RtcSlotId,
        onLeave: () -> Unit,
    ): CallSignalling {
        return MatrixRtcSignalling(
            coroutineScope = coroutineScope,
            matrixClient = matrixClient,
            roomId = roomId,
            slotId = slotId,
            configuration = configuration,
            initials = initials,
            getRoomName = roomName,
            i18n = i18n,
            onLeave = onLeave,
        )
    }
}
