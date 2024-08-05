package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger {}

interface BannedMemberListElementViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        roomUser: RoomUser,
        error: MutableStateFlow<String?>
    ): BannedMemberListElementViewModel =
        BannedMemberListElementViewModelImpl(
            viewModelContext,
            selectedRoomId,
            roomUser,
            error
        )

    companion object : BannedMemberListElementViewModelFactory
}

interface BannedMemberListElementViewModel {
    val userId: UserId
    val bannedMemberOptionsOpen: StateFlow<Boolean>
    val iHavePowerToUnbanUser: StateFlow<Boolean>
    val error: StateFlow<String?>
    val reason: String?

    fun unbanUser(reason: String?)
    fun openBannedMemberOptions()
    fun closeBannedMemberOptions()
}

class BannedMemberListElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val roomUser: RoomUser,
    override val error: MutableStateFlow<String?>
): MatrixClientViewModelContext by viewModelContext, BannedMemberListElementViewModel {
    override val bannedMemberOptionsOpen = MutableStateFlow(false)
    override val iHavePowerToUnbanUser = matrixClient.user.canUnbanUser(selectedRoomId, roomUser.userId)
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val reason: String? = roomUser.event.content.reason

    override fun unbanUser(reason: String?) {
        val roomUserId = roomUser.userId
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomMemberUnbanUserErrorOffline()
                return@launch
            }

            if (iHavePowerToUnbanUser.value) {
                matrixClient.api.room.unbanUser(
                    roomId = selectedRoomId,
                    userId = roomUserId,
                    reason = reason,
                    asUserId = matrixClient.userId
                ).fold(
                    onSuccess = {
                        this.closeBannedMemberOptions()
                        error.value = null
                    },
                    onFailure = {
                        if (it is CancellationException) {
                            return@launch
                        }

                        log.error(it) { "cannot unban user $roomUserId from $selectedRoomId: ${it.message}" }
                        error.value = i18n.settingsRoomMemberUnbanUserError()
                    }
                )
            } else {
                log.error { "cannot unban user $roomUserId from $selectedRoomId: User is not able to unban this member" }
                error.value = i18n.settingsRoomMemberUnbanUserErrorNotPossible()
            }
        }
    }

    override fun openBannedMemberOptions() {
        this.bannedMemberOptionsOpen.value = true
    }

    override fun closeBannedMemberOptions() {
        this.bannedMemberOptionsOpen.value = false
    }
}
