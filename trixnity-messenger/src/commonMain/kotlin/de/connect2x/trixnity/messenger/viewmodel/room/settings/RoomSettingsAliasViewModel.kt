package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent

private val log = KotlinLogging.logger {}

interface RoomSettingsAliasViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
    ): RoomSettingsAliasViewModel {
        return RoomSettingsAliasViewModelImpl(viewModelContext, selectedRoomId)
    }

    companion object : RoomSettingsAliasViewModelFactory
}

interface RoomSettingsAliasViewModel {
    val roomAliases: StateFlow<CanonicalAliasEventContent?>
    val canChangeRoomAlias: StateFlow<Boolean>
    fun addRoomAlias(roomId: RoomId, alias: RoomAliasId): StateFlow<RoomAliasSettingsStatus>
    fun removeRoomAlias(alias: RoomAliasId): StateFlow<RoomAliasSettingsStatus>
    fun setMainAlias(roomAliasId: RoomAliasId): StateFlow<RoomAliasSettingsStatus>
}

class RoomSettingsAliasViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsAliasViewModel {
    override val roomAliases: StateFlow<CanonicalAliasEventContent?> =
        matrixClient.room.getState<CanonicalAliasEventContent>(selectedRoomId)
            .map { it?.content }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val canChangeRoomAlias: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<CanonicalAliasEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun addRoomAlias(roomId: RoomId, alias: RoomAliasId): StateFlow<RoomAliasSettingsStatus> {
        return handleSetRequest(
            CanonicalAliasEventContent(
                roomAliases.value?.alias,
                roomAliases.value?.aliases?.plus(alias)
            )
        )
    }

    override fun removeRoomAlias(alias: RoomAliasId): StateFlow<RoomAliasSettingsStatus> {
        return handleSetRequest(
            CanonicalAliasEventContent(
                roomAliases.value?.alias.let {
                    if (it == alias) null else it
                },
                roomAliases.value?.aliases?.minus(alias)
            )
        )
    }

    override fun setMainAlias(roomAliasId: RoomAliasId): StateFlow<RoomAliasSettingsStatus> {
        return handleSetRequest(
            CanonicalAliasEventContent(
                roomAliasId,
                roomAliases.value?.aliases
            )
        )
    }

    private fun handleSetRequest(event: CanonicalAliasEventContent): StateFlow<RoomAliasSettingsStatus> = flow {
        if (!canChangeRoomAlias.value) {
            log.error { "Insufficient power level to remove room alias from $selectedRoomId" }
            emit(RoomAliasSettingsStatus.INSUFFICIENT_POWER_LEVEL)
        } else {
            matrixClient.api.room.sendStateEvent(
                selectedRoomId,
                event
            )
                .onFailure {
                    if (it.cause !is MatrixServerException) {
                        emit(RoomAliasSettingsStatus.FAILED_UNKNOWN)
                    } else {
                        when ((it.cause as MatrixServerException).errorResponse) {
                            is ErrorResponse.InvalidParam -> emit(RoomAliasSettingsStatus.INVALID)
                            else -> emit(RoomAliasSettingsStatus.FAILED_UNKNOWN)
                        }
                    }
                }
                .onSuccess {
                    emit(RoomAliasSettingsStatus.SUCCESS)
                }
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), RoomAliasSettingsStatus.PROCESSING)


}

enum class RoomAliasSettingsStatus {
    INSUFFICIENT_POWER_LEVEL, PROCESSING, FAILED_UNKNOWN, INVALID, SUCCESS
}

class PreviewRoomSettingsAliasViewModel : RoomSettingsAliasViewModel {
    override val roomAliases: StateFlow<CanonicalAliasEventContent?> = MutableStateFlow(null)
    override val canChangeRoomAlias: StateFlow<Boolean> = MutableStateFlow(false)

    override fun addRoomAlias(roomId: RoomId, alias: RoomAliasId): StateFlow<RoomAliasSettingsStatus> =
        MutableStateFlow(RoomAliasSettingsStatus.PROCESSING)

    override fun removeRoomAlias(alias: RoomAliasId): StateFlow<RoomAliasSettingsStatus> =
        MutableStateFlow(RoomAliasSettingsStatus.PROCESSING)

    override fun setMainAlias(roomAliasId: RoomAliasId): StateFlow<RoomAliasSettingsStatus> =
        MutableStateFlow(RoomAliasSettingsStatus.PROCESSING)

}
