package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.util.PreviewEditableTextFieldViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
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
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface RoomSettingsAliasViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        removeAliasError: MutableStateFlow<String?>,
    ): RoomSettingsAliasViewModel {
        return RoomSettingsAliasViewModelImpl(viewModelContext, selectedRoomId, removeAliasError)
    }

    companion object : RoomSettingsAliasViewModelFactory
}

interface RoomSettingsAliasViewModel {
    val otherAliasesIds: StateFlow<Set<RoomAliasId>?>
    val mainAliasId: StateFlow<RoomAliasId?>
    val canChangeRoomAlias: StateFlow<Boolean>
    val mainAlias: EditableTextFieldViewModel
    val newAlias: EditableTextFieldViewModel

    val removingAliases: StateFlow<Set<RoomAliasId>>
    fun removeAlias(alias: RoomAliasId)
}

class RoomSettingsAliasViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val removeAliasError: MutableStateFlow<String?>,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsAliasViewModel {
    private val roomAliases: StateFlow<CanonicalAliasEventContent?> =
        matrixClient.room.getState<CanonicalAliasEventContent>(selectedRoomId)
            .map { it?.content }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val otherAliasesIds: StateFlow<Set<RoomAliasId>?> = roomAliases
        .map { it?.aliases }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val mainAliasId: StateFlow<RoomAliasId?> = roomAliases
        .map { it?.alias }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val canChangeRoomAlias: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<CanonicalAliasEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val mainAlias = EditableTextFieldViewModelImpl(
        flowOf(roomAliases.value?.alias?.full),
        coroutineScope,
        onApplyChange = { alias ->
            matrixClient.api.room.sendStateEvent(
                selectedRoomId,
                CanonicalAliasEventContent(
                    RoomAliasId(alias),
                    roomAliases.value?.aliases?.minus(RoomAliasId(alias))
                )
            )
        }
    )

    override val newAlias = EditableTextFieldViewModelImpl(
        flowOf(""),
        coroutineScope,
        onApplyChange = { alias ->
            matrixClient.api.room.sendStateEvent(
                selectedRoomId,
                CanonicalAliasEventContent(
                    roomAliases.value?.alias,
                    roomAliases.value?.aliases?.plus(RoomAliasId(alias))
                )
            )
        }
    )

    private val _removingAliases = MutableStateFlow(emptySet<RoomAliasId>())
    override val removingAliases = _removingAliases.asStateFlow()


    override fun removeAlias(alias: RoomAliasId) {
        val i18n = get<I18n>()

        _removingAliases.value += alias

        if (!canChangeRoomAlias.value) {
            log.error { "Cancelled removal of Alias $alias due to missing permissions" }
            removeAliasError.value =
                i18n.settingsRoomLeaveRoomError(i18n.settingsRoomAliasRemoveInsufficientPowerLevel())
            _removingAliases.value -= alias
            return
        }

        coroutineScope.launch {
            matrixClient.api.room.sendStateEvent(
                selectedRoomId,
                CanonicalAliasEventContent(
                    roomAliases.value?.alias.let {
                        if (it == alias) null else it
                    },
                    roomAliases.value?.aliases?.minus(alias)
                )
            ).onFailure { error ->
                if (error.cause !is MatrixServerException) {
                    removeAliasError.value = i18n.settingsRoomAliasRemoveGeneric()
                } else {
                    when ((error.cause as MatrixServerException).errorResponse) {
                        is ErrorResponse.NotFound -> i18n.settingsRoomAliasRemoveNotFound()
                        else -> removeAliasError.value = i18n.settingsRoomAliasRemoveGeneric()
                    }
                }

                _removingAliases.value -= alias
            }.onSuccess {
                removeAliasError.value = null
                _removingAliases.value -= alias
            }
        }
    }
}

class PreviewRoomSettingsAliasViewModel : RoomSettingsAliasViewModel {
    override val otherAliasesIds: StateFlow<Set<RoomAliasId>?> = MutableStateFlow(null)
    override val mainAliasId: StateFlow<RoomAliasId?> = MutableStateFlow(null)
    override val canChangeRoomAlias: StateFlow<Boolean> = MutableStateFlow(false)
    override val mainAlias: EditableTextFieldViewModel = PreviewEditableTextFieldViewModel()
    override val newAlias: EditableTextFieldViewModel = PreviewEditableTextFieldViewModel()
    override val removingAliases: StateFlow<Set<RoomAliasId>> = MutableStateFlow(emptySet())

    override fun removeAlias(alias: RoomAliasId) {
    }
}
