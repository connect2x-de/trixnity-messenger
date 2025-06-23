package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

interface RoomSettingsAliasViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isDirect: StateFlow<Boolean>,
        removeAliasError: MutableStateFlow<String?>,
        updateError: MutableStateFlow<String?>,
        newAliasError: MutableStateFlow<String?>,
    ): RoomSettingsAliasViewModel {
        return RoomSettingsAliasViewModelImpl(
            viewModelContext,
            selectedRoomId,
            isDirect,
            removeAliasError,
            updateError,
            newAliasError
        )
    }

    companion object : RoomSettingsAliasViewModelFactory
}

interface RoomSettingsAliasViewModel {
    val canChangeRoomAlias: StateFlow<Boolean>
    val showRoomAliasSettings: StateFlow<Boolean>

    val mainAlias: StateFlow<String?>
    val moreAliases: StateFlow<Set<String>>

    val domain: String

    val isUpdating: StateFlow<Boolean>

    val newAlias: TextFieldViewModel

    fun addNewAlias(onlyLocalpart: Boolean = false)
    fun changeMainAlias(alias: RoomAliasId?)
    fun removeAlias(alias: RoomAliasId)
    fun removeMainAlias(alias: RoomAliasId)

}

class RoomSettingsAliasViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val isDirect: StateFlow<Boolean>,
    private val removeAliasError: MutableStateFlow<String?>,
    private val updateError: MutableStateFlow<String?>,
    private val newAliasError: MutableStateFlow<String?>
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsAliasViewModel {
    private val roomAliases: StateFlow<CanonicalAliasEventContent?> =
        matrixClient.room.getState<CanonicalAliasEventContent>(selectedRoomId)
            .map { it?.content }
            .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    private val allAliases: StateFlow<Set<RoomAliasId>> =
        roomAliases.map {
            it?.aliases.orEmpty().plus(it?.alias).filterNotNull().toSet()
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptySet())
    override val mainAlias: StateFlow<String?> = roomAliases
        .map { it?.alias?.full }
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)
    override val moreAliases: StateFlow<Set<String>> = roomAliases
        .map { it?.aliases?.map(RoomAliasId::toString)?.toSet().orEmpty() }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptySet())

    override val domain: String = matrixClient.userId.domain

    override val canChangeRoomAlias: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<CanonicalAliasEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val showRoomAliasSettings: StateFlow<Boolean> =
        combine(isDirect, canChangeRoomAlias, allAliases) { isDirect, canChangeRoomAlias, allAliases ->
            !isDirect && (canChangeRoomAlias || allAliases.isNotEmpty())
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    private val _isUpdating = MutableStateFlow(false)
    override val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    override val newAlias = TextFieldViewModelImpl(maxLength = 1_000)

    internal val i18n = get<I18n>()

    override fun addNewAlias(onlyLocalpart: Boolean) {
        val currentNewAlias = if (onlyLocalpart) "#${newAlias.value.text}:$domain" else newAlias.value.text

        if (_isUpdating.getAndUpdate { true }) {
            log.debug { "Cancelled add Alias $currentNewAlias due to event still updating" }
            return
        } else {
            coroutineScope.launch {
                if (!canChangeRoomAlias.value) {
                    log.warn { "Cancelled add Alias $currentNewAlias due to missing permissions" }
                    newAliasError.value = i18n.settingsRoomAliasAddAliasInsufficientPowerLevel()
                    return@launch
                }

                if (!MatrixRegex.roomAlias.matches(currentNewAlias)) {
                    newAliasError.value = i18n.settingsRoomAliasAddAliasInvalid()
                    return@launch
                }

                val alias = RoomAliasId(currentNewAlias)

                if (allAliases.value.contains(alias)) {
                    log.warn { "Cancelled adding Alias $alias due to already existing" }
                    newAliasError.value = i18n.settingsRoomAliasAddAliasExisting()
                    return@launch
                }
                val newAliases = roomAliases.value?.aliases.orEmpty() + alias

                matrixClient.api.room.getRoomAlias(alias).fold(
                    onSuccess = {
                        if (it.roomId == selectedRoomId) {
                            log.warn { "Alias $alias already exists in this room" }
                            newAliasError.value = i18n.settingsRoomAliasAddExists()
                            return@launch
                        } else {
                            log.warn { "Alias $alias already exists in another room" }
                            newAliasError.value = i18n.settingsRoomAliasAddExists()
                            return@launch
                        }
                    },
                    onFailure = { error ->
                        newAliasError.value =
                            if (error !is MatrixServerException) {
                                log.error(error) { "Unexpected Failure" }
                                i18n.settingsRoomAliasGeneric()
                            } else {
                                when (val response = error.errorResponse) {
                                    is ErrorResponse.InvalidParam -> i18n.settingsRoomAliasChangeInvalidSyntax()
                                    is ErrorResponse.NotFound -> {
                                        log.trace { "Happy path: Room Alias doesn't exist yet" }
                                        return@fold
                                    }

                                    else -> {
                                        log.error(error) { "Unexpected Error: ${response.error}" }
                                        i18n.settingsRoomAliasGeneric()
                                    }
                                }
                            }

                        return@launch
                    }
                )

                matrixClient.api.room.setRoomAlias(selectedRoomId, alias, userId).onFailure { error ->
                    newAliasError.value =
                        if (error !is MatrixServerException) {
                            log.error(error) { "Unexpected Failure" }
                            i18n.settingsRoomAliasGeneric()
                        } else {
                            when (val response = error.errorResponse) {
                                is ErrorResponse.InvalidParam -> i18n.settingsRoomAliasChangeInvalidSyntax()
                                is ErrorResponse.Unknown -> i18n.settingsRoomAliasAddExists()

                                else -> {
                                    log.error(error) { "Unexpected Error: ${response.error}" }
                                    i18n.settingsRoomAliasGeneric()
                                }
                            }
                        }

                    return@launch
                }


                matrixClient.api.room.sendStateEvent(
                    selectedRoomId,
                    CanonicalAliasEventContent(
                        roomAliases.value?.alias,
                        newAliases
                    )
                ).fold(
                    onSuccess = {
                        withTimeoutOrNull(5.seconds) {
                            matrixClient.room.getState<CanonicalAliasEventContent>(selectedRoomId)
                                .first { it?.content?.aliases?.contains(alias) == true }
                        }
                    },
                    onFailure = { error ->
                        newAliasError.value =
                            if (error !is MatrixServerException) {
                                log.error(error) { "Unexpected Failure" }
                                i18n.settingsRoomAliasGeneric()
                            } else {
                                when (val response = error.errorResponse) {
                                    is ErrorResponse.InvalidParam -> i18n.settingsRoomAliasChangeInvalidSyntax()
                                    is ErrorResponse.BadState -> i18n.settingsRoomAliasBadAlias()
                                    is ErrorResponse.CustomErrorResponse -> when (response.errorCode) {
                                        "M_BAD_ALIAS" -> i18n.settingsRoomAliasBadAlias()
                                        else -> {
                                            log.error(error) { "Unexpected Error: ${response.errorCode}" }
                                            i18n.settingsRoomAliasGeneric()
                                        }
                                    }

                                    else -> {
                                        log.error(error) { "Unexpected Error: ${response.error}" }
                                        i18n.settingsRoomAliasGeneric()
                                    }
                                }
                            }

                        return@launch
                    }
                )

                newAlias.update("")
                newAliasError.value = null
            }.invokeOnCompletion { _isUpdating.value = false }
        }
    }

    override fun changeMainAlias(alias: RoomAliasId?) {
        if (_isUpdating.getAndUpdate { true }) {
            log.debug { "Cancelled change of Alias $alias to main alias due to event still updating" }
            _isUpdating.value = false
            return
        } else {
            coroutineScope.launch {
                if (!canChangeRoomAlias.value) {
                    log.warn { "Cancelled change of Alias $alias to mainalias due to missing permissions" }
                    updateError.value = i18n.settingsRoomAliasChangeMainInsufficientPowerLevel()
                    return@launch
                }

                if (alias != null && !moreAliases.value.contains(alias.full) && mainAlias.value != alias.full) {
                    log.warn { "Cancelled change of Alias $alias to mainalias due to not being related to that room" }
                    updateError.value = i18n.settingsRoomAliasChangeMainUnrelatedAlias()
                    return@launch
                }

                val currentMainAlias = RoomAliasId(mainAlias.value ?: "")
                val currentMoreAliases = moreAliases.value.map {
                    RoomAliasId(it)
                }.toSet()

                if (alias == currentMainAlias) {
                    log.warn { "Cancelled change of Alias $alias to mainalias due to already being it" }
                    updateError.value = null
                    return@launch
                }

                matrixClient.api.room.sendStateEvent(
                    selectedRoomId,
                    CanonicalAliasEventContent(
                        alias,
                        currentMoreAliases
                            .let {
                                if (alias == null) {
                                    log.trace { "Moved mainAlias ($currentMainAlias) into others" }
                                    it + currentMainAlias
                                } else it
                            }.let {
                                if (alias != null) {
                                    log.trace { "Moved alias ($alias) into mainAlias" }
                                    it - alias
                                } else it
                            }.let {
                                if (currentMainAlias.full.isNotBlank()) {
                                    log.trace { "Moved mainalias ($currentMainAlias) into aliases" }
                                    it + currentMainAlias
                                } else it
                            }
                    )
                ).fold(
                    onSuccess = {
                        withTimeoutOrNull(5.seconds) {
                            matrixClient.room.getState<CanonicalAliasEventContent>(selectedRoomId)
                                .first {
                                    if (alias == null) {
                                        it?.content?.aliases?.contains(currentMainAlias) ?: false
                                    } else {
                                        it?.content?.alias == alias
                                    }
                                }
                        }
                    },
                    onFailure = { error ->
                        updateError.value =
                            if (error !is MatrixServerException) {
                                log.error(error) { "Unexpected Failure" }
                                i18n.settingsRoomAliasGeneric()
                            } else {
                                when (val response = error.errorResponse) {
                                    is ErrorResponse.InvalidParam -> i18n.settingsRoomAliasChangeInvalidSyntax()
                                    is ErrorResponse.BadState -> i18n.settingsRoomAliasBadAlias()
                                    is ErrorResponse.NotFound -> i18n.settingsRoomAliasChangeMainNotFound()
                                    is ErrorResponse.CustomErrorResponse -> when (response.errorCode) {
                                        "M_BAD_ALIAS" -> i18n.settingsRoomAliasBadAlias()
                                        else -> {
                                            log.error(error) { "Unexpected Error: ${response.error}" }
                                            i18n.settingsRoomAliasGeneric()
                                        }
                                    }

                                    else -> {
                                        log.error(error) { "Unexpected Error: ${response.error}" }
                                        i18n.settingsRoomAliasGeneric()
                                    }
                                }
                            }

                        return@launch
                    }
                )

                updateError.value = null
            }.invokeOnCompletion { _isUpdating.value = false }
        }
    }

    override fun removeAlias(alias: RoomAliasId) {
        if (_isUpdating.getAndUpdate { true }) {
            log.debug { "Cancelled removal of Alias $alias due to event still updating" }
            return
        } else {
            coroutineScope.launch {
                if (!canChangeRoomAlias.value) {
                    log.warn { "Cancelled removal of Alias $alias due to missing permissions" }
                    removeAliasError.value = i18n.settingsRoomAliasRemoveInsufficientPowerLevel()
                    return@launch
                }

                matrixClient.api.room.deleteRoomAlias(alias, userId).onFailure { error ->
                    if (error !is MatrixServerException) {
                        log.error(error) { "Unexpected Failure" }
                        removeAliasError.value = i18n.settingsRoomAliasGeneric()
                    } else {
                        removeAliasError.value =
                            when (val response = error.errorResponse) {
                                is ErrorResponse.NotFound -> return@onFailure

                                else -> {
                                    log.error(error) { "Unexpected Error: ${response.error}" }
                                    i18n.settingsRoomAliasGeneric()
                                }
                            }
                    }

                    return@launch
                }

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
                        withTimeoutOrNull(5.seconds) {
                            matrixClient.room.getState<CanonicalAliasEventContent>(selectedRoomId)
                                .first { it?.content?.alias != alias && it?.content?.aliases?.contains(alias) != true }
                        }
                    },
                    onFailure = { error ->
                        removeAliasError.value =
                            if (error !is MatrixServerException) {
                                log.error(error) { "Unexpected Failure" }
                                i18n.settingsRoomAliasGeneric()
                            } else {
                                when (val response = error.errorResponse) {
                                    is ErrorResponse.InvalidParam -> i18n.settingsRoomAliasChangeInvalidSyntax()
                                    is ErrorResponse.BadState -> i18n.settingsRoomAliasBadAlias()
                                    is ErrorResponse.CustomErrorResponse -> when (response.errorCode) {
                                        "M_BAD_ALIAS" -> i18n.settingsRoomAliasBadAlias()
                                        else -> {
                                            log.error(error) { "Unexpected Error: ${response.error}" }
                                            i18n.settingsRoomAliasGeneric()
                                        }
                                    }

                                    is ErrorResponse.NotFound -> i18n.settingsRoomAliasRemoveNotFound()
                                    else -> {
                                        log.error(error) { "Unexpected Error: ${response.error}" }
                                        i18n.settingsRoomAliasGeneric()
                                    }
                                }
                            }

                        return@launch
                    }
                )

                removeAliasError.value = null
            }.invokeOnCompletion { _isUpdating.value = false }
        }
    }

    override fun removeMainAlias(alias: RoomAliasId) {
        coroutineScope.launch {
            changeMainAlias(null)
            isUpdating.first { it.not() }
            removeAlias(alias)
        }
    }
}

class PreviewRoomSettingsAliasViewModel : RoomSettingsAliasViewModel {
    override val canChangeRoomAlias: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showRoomAliasSettings: StateFlow<Boolean> = MutableStateFlow(false)
    override val mainAlias: MutableStateFlow<Nothing?> = MutableStateFlow(null)
    override val domain: String = "example.org"
    override val moreAliases: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val isUpdating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val newAlias = TextFieldViewModelImpl(maxLength = 1_000, )

    override fun addNewAlias(onlyLocalpart: Boolean) {
    }

    override fun changeMainAlias(alias: RoomAliasId?) {
    }

    override fun removeAlias(alias: RoomAliasId) {
    }

    override fun removeMainAlias(alias: RoomAliasId) {
    }
}
