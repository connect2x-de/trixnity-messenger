package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.EditableTextFieldViewModelImpl
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
import net.folivo.trixnity.core.MatrixRegex
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
        updateError: MutableStateFlow<String?>,
        newAliasError: MutableStateFlow<String?>,
    ): RoomSettingsAliasViewModel {
        return RoomSettingsAliasViewModelImpl(
            viewModelContext,
            selectedRoomId,
            removeAliasError,
            updateError,
            newAliasError
        )
    }

    companion object : RoomSettingsAliasViewModelFactory
}

interface RoomSettingsAliasViewModel {
    val canChangeRoomAlias: StateFlow<Boolean>

    val mainAlias: StateFlow<String?>
    val moreAliases: StateFlow<List<String>>

    val isUpdating: StateFlow<Boolean>

    val newAlias: MutableStateFlow<String>

    fun addNewAlias()
    fun changeMainAlias(alias: RoomAliasId?)
    fun removeAlias(alias: RoomAliasId)

}

class RoomSettingsAliasViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val removeAliasError: MutableStateFlow<String?>,
    private val updateError: MutableStateFlow<String?>,
    private val newAliasError: MutableStateFlow<String?>
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsAliasViewModel {
    private val roomAliases: StateFlow<CanonicalAliasEventContent?> =
        matrixClient.room.getState<CanonicalAliasEventContent>(selectedRoomId)
            .map { it?.content }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val mainAlias: StateFlow<String?> = roomAliases
        .map { it?.alias?.full }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val moreAliases: StateFlow<List<String>> = roomAliases
        .map { it?.aliases?.map { it.toString() } ?: emptyList() }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    override val canChangeRoomAlias: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<CanonicalAliasEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val _isUpdating = MutableStateFlow(false)
    override val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    override val newAlias: MutableStateFlow<String> = MutableStateFlow("")

    override fun addNewAlias() {
        val i18n = get<I18n>()

        if (!MatrixRegex.roomAlias.matches(newAlias.value)) {
            newAliasError.value = i18n.settingsRoomAliasAddAliasInvalid()
            return
        }

        val alias = RoomAliasId(newAlias.value)

        if (isUpdating.value) {
            log.debug { "Cancelled add Alias $alias due to event still updating" }
            return
        }

        _isUpdating.value = true

        if (!canChangeRoomAlias.value) {
            log.error { "Cancelled add Alias $alias due to missing permissions" }
            newAliasError.value = i18n.settingsRoomAliasAddAliasInsufficientPowerLevel()
            _isUpdating.value = false
            return
        }

        coroutineScope.launch {
            matrixClient.api.room.sendStateEvent(
                selectedRoomId,
                CanonicalAliasEventContent(
                    roomAliases.value?.alias,
                    roomAliases.value?.aliases?.plus(alias)
                )
            ).fold(
                onSuccess = {
                    newAliasError.value = null
                    _isUpdating.value = false
                },
                onFailure = { error ->
                    if (error.cause !is MatrixServerException) {
                        newAliasError.value = i18n.settingsRoomAliasGeneric()
                    } else {
                        newAliasError.value =
                            when (val response = (error.cause as MatrixServerException).errorResponse) {
                                else -> {
                                    log.warn { "Unexpected Error: ${response.error}" }
                                    i18n.settingsRoomAliasGeneric()
                                }
                            }
                    }

                    _isUpdating.value = false
                }
            )
        }
    }

    override fun changeMainAlias(alias: RoomAliasId?) {
        if (isUpdating.value) {
            log.debug { "Cancelled change of Alias $alias to main alias due to event still updating" }
            return
        }

        val i18n = get<I18n>()
        _isUpdating.value = true

        if (!canChangeRoomAlias.value) {
            log.error { "Cancelled change of Alias $alias to mainalias due to missing permissions" }
            updateError.value = i18n.settingsRoomAliasChangeMainInsufficientPowerLevel()
            _isUpdating.value = false
            return
        }

        coroutineScope.launch {
            matrixClient.api.room.sendStateEvent(
                selectedRoomId,
                CanonicalAliasEventContent(
                    alias,
                    roomAliases.value?.aliases
                        ?.minus(alias)
                        ?.filterNotNull()?.toSet()
                )
            ).fold(
                onSuccess = {
                    updateError.value = null
                    _isUpdating.value = false
                },
                onFailure = { error ->
                    if (error.cause !is MatrixServerException) {
                        updateError.value = i18n.settingsRoomAliasGeneric()
                    } else {
                        updateError.value = when (val response = (error.cause as MatrixServerException).errorResponse) {
                            is ErrorResponse.NotFound -> i18n.settingsRoomAliasChangeMainNotFound()
                            else -> {
                                log.warn { "Unexpected Error: ${response.error}" }
                                i18n.settingsRoomAliasGeneric()
                            }
                        }
                    }

                    _isUpdating.value = false
                }
            )
        }
    }

    override fun removeAlias(alias: RoomAliasId) {
        if (isUpdating.value) {
            log.debug { "Cancelled removal of Alias $alias due to event still updating" }
            return
        }

        val i18n = get<I18n>()
        _isUpdating.value = true

        if (!canChangeRoomAlias.value) {
            log.error { "Cancelled removal of Alias $alias due to missing permissions" }
            removeAliasError.value = i18n.settingsRoomAliasRemoveInsufficientPowerLevel()
            _isUpdating.value = false
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
            ).fold(
                onSuccess = {
                    removeAliasError.value = null
                    _isUpdating.value = false
                },
                onFailure = { error ->
                    if (error.cause !is MatrixServerException) {
                        removeAliasError.value = i18n.settingsRoomAliasGeneric()
                    } else {
                        removeAliasError.value =
                            when (val response = (error.cause as MatrixServerException).errorResponse) {
                                is ErrorResponse.NotFound -> i18n.settingsRoomAliasRemoveNotFound()
                                else -> {
                                    log.warn { "Unexpected Error: ${response.error}" }
                                    i18n.settingsRoomAliasGeneric()
                                }
                            }
                    }

                    _isUpdating.value = false
                }
            )
        }
    }
}

class PreviewRoomSettingsAliasViewModel : RoomSettingsAliasViewModel {
    override val canChangeRoomAlias: StateFlow<Boolean> = MutableStateFlow(false)
    override val mainAlias: MutableStateFlow<Nothing?> = MutableStateFlow(null)
    override val moreAliases: StateFlow<List<String>> = MutableStateFlow(emptyList())
    override val isUpdating: StateFlow<Boolean> = MutableStateFlow(false)
    override val newAlias: MutableStateFlow<String> = MutableStateFlow("")

    override fun addNewAlias() {
    }

    override fun changeMainAlias(alias: RoomAliasId?) {
    }

    override fun removeAlias(alias: RoomAliasId) {
    }
}
