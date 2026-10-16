package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.client.rtc.PerMemberRtcSlotMembershipEncryptionInfo
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel.LiveKitCallParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@TrixnityMessengerCallApi
interface RoomCallViewModel {
    val roomInfo: StateFlow<RoomInfoElement?>
    val publishing: StateFlow<LiveKitCallParameters?>

    /** this will never include the url from [publishing] */
    val receiving: StateFlow<List<LiveKitCallParameters>>
    val members: StateFlow<Map<Pair<UserId, RtcMemberId>, UserInfoElement>>
    val error: StateFlow<String?>

    fun back()

    @OptIn(MSC4143::class)
    data class LiveKitCallParameters(
        val url: String,
        val token: String,
        val userId: UserId,
        val memberId: RtcMemberId,
        // TODO this is currently the exact same for all instances of LiveKitCallParameters.
        //      We should be RtcEncryptionKeyEventContent for the specific participant instead.
        val encryptionInfo: PerMemberRtcSlotMembershipEncryptionInfo? = null,
    )
}

@OptIn(TrixnityMessengerCallApi::class)
internal class RoomCallViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
) : RoomCallViewModel, MatrixClientViewModelContext by viewModelContext {

    private val _roomInfo = MutableStateFlow<RoomInfoElement?>(null)

    private val _publishing = MutableStateFlow<LiveKitCallParameters?>(null)

    private val _receiving = MutableStateFlow<List<LiveKitCallParameters>>(emptyList())

    private val _members = MutableStateFlow<Map<Pair<UserId, RtcMemberId>, UserInfoElement>>(emptyMap())

    private val _error = MutableStateFlow<String?>(null)

    override val roomInfo: StateFlow<RoomInfoElement?> = _roomInfo.asStateFlow()

    override val publishing: StateFlow<LiveKitCallParameters?> = _publishing.asStateFlow()

    override val receiving: StateFlow<List<LiveKitCallParameters>> = _receiving.asStateFlow()

    override val members: StateFlow<Map<Pair<UserId, RtcMemberId>, UserInfoElement>> = _members.asStateFlow()

    override val error: StateFlow<String?> = _error.asStateFlow()

    override fun back() {
        onBack()
    }
}
