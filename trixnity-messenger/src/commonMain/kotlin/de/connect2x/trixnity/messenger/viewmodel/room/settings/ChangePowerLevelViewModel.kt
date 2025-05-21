package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModel.Role
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent

private val log = KotlinLogging.logger {}

interface ChangePowerLevelViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        error: MutableStateFlow<String?>,
        selectedRoomId: RoomId,
        targetUser: UserId,
    ): ChangePowerLevelViewModel {
        return ChangePowerLevelViewModelImpl(
            viewModelContext,
            error,
            selectedRoomId,
            targetUser,
        )
    }

    companion object : ChangePowerLevelViewModelFactory
}

interface ChangePowerLevelViewModel {
    val canSetRoleToUser: StateFlow<Boolean>
    val canSetRoleToAdmin: StateFlow<Boolean>
    val canSetRoleToModerator: StateFlow<Boolean>

    val changingRoleWarningDialogOpen: StateFlow<Role?>
    val changingPowerLevelDialogOpen: StateFlow<Boolean>

    val showPowerLevelHelp: StateFlow<Boolean>

    val canSetPowerLevelToMax: StateFlow<Long?>

    val changingPowerLevelDialogError: StateFlow<String?>
    val changingPowerLevelDialogInput: TextFieldViewModel

    fun openChangingRoleWarningDialog(role: Role)
    fun closeChangingRoleWarningDialog()

    fun openChangingPowerLevelDialog()
    fun closeChangingPowerLevelDialog()

    fun openPowerLevelHelp()
    fun closePowerLevelHelp()

    fun setRoleToUser()
    fun setRoleToModerator()
    fun setRoleToAdmin()
    fun setPowerLevelTo(level: Long)

    enum class Role {
        USER {
            override fun getMinPowerLevel() = 0L
        },
        MODERATOR {
            override fun getMinPowerLevel() = 50L
        },
        ADMIN {
            override fun getMinPowerLevel() = 100L
        };

        abstract fun getMinPowerLevel(): Long
    }
}

open class ChangePowerLevelViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val error: MutableStateFlow<String?>,
    private val selectedRoomId: RoomId,
    private val targetUser: UserId,
) : MatrixClientViewModelContext by viewModelContext, ChangePowerLevelViewModel {

    override val canSetPowerLevelToMax =
        matrixClient.user.canSetPowerLevelToMax(selectedRoomId, targetUser)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val combineSetPowerLevelToMaxAndCurrentPowerLevel =
        combine(
            canSetPowerLevelToMax,
            matrixClient.user.getPowerLevel(selectedRoomId, targetUser)
        ) { maxPowerLevel, targetUserPowerLevel ->
            maxPowerLevel to targetUserPowerLevel
        }.shareIn(coroutineScope, started = SharingStarted.WhileSubscribed(), 1)

    internal fun canSetPowerLevelToRole(role: Role) = // internal for test only
        combineSetPowerLevelToMaxAndCurrentPowerLevel.map { (canSetPowerLevelToMax, currentPowerLevel) ->
            log.trace { "role=$role canSetPowerLevelToMax=$canSetPowerLevelToMax currentPowerLevel=$currentPowerLevel" }
            canSetPowerLevelToMax != null &&
                    currentPowerLevel != role.getMinPowerLevel() &&
                    canSetPowerLevelToMax >= role.getMinPowerLevel()
        }


    override val canSetRoleToUser = canSetPowerLevelToRole(Role.USER)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val canSetRoleToModerator = canSetPowerLevelToRole(Role.MODERATOR)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val canSetRoleToAdmin = canSetPowerLevelToRole(Role.ADMIN)
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)


    override val changingRoleWarningDialogOpen =
        MutableStateFlow<Role?>(null)

    override val changingPowerLevelDialogOpen = MutableStateFlow(false)

    override val changingPowerLevelDialogInput = TextFieldViewModelImpl()

    override val changingPowerLevelDialogError =
        combine(
            canSetPowerLevelToMax,
            changingPowerLevelDialogInput.text,
        ) { canSetPowerLevelToMax, text ->
            if (text.isEmpty()) {
                null
            } else {
                validateNewPowerLevelInput(
                    text,
                    maxPowerLevel = canSetPowerLevelToMax,
                    i18n
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val showPowerLevelHelp = MutableStateFlow(false)

    override fun setRoleToUser() =
        setUserToPowerLevel(Role.USER.getMinPowerLevel())

    override fun setRoleToModerator() =
        setUserToPowerLevel(Role.MODERATOR.getMinPowerLevel())

    override fun setRoleToAdmin() =
        setUserToPowerLevel(Role.ADMIN.getMinPowerLevel())

    override fun setPowerLevelTo(level: Long) = setUserToPowerLevel(level)

    override fun openChangingRoleWarningDialog(role: Role) {
        changingRoleWarningDialogOpen.value = role
    }

    override fun closeChangingRoleWarningDialog() {
        changingRoleWarningDialogOpen.value = null
    }

    override fun openChangingPowerLevelDialog() {
        changingPowerLevelDialogOpen.value = true
    }

    override fun closeChangingPowerLevelDialog() {
        changingPowerLevelDialogOpen.value = false
    }

    override fun openPowerLevelHelp() {
        showPowerLevelHelp.value = true
    }

    override fun closePowerLevelHelp() {
        showPowerLevelHelp.value = false
    }

    private fun setUserToPowerLevel(powerLevel: Long) {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomMemberListChangePowerLevelErrorOffline()
            } else {
                val oldPowerLevelsEventContent =
                    matrixClient.room.getState<PowerLevelsEventContent>(
                        roomId = selectedRoomId,
                        stateKey = ""
                    ).first()?.content ?: PowerLevelsEventContent()
                val newUsers = oldPowerLevelsEventContent.users.plus(targetUser to powerLevel)

                matrixClient.api.room.sendStateEvent(
                    roomId = selectedRoomId,
                    oldPowerLevelsEventContent.copy(users = newUsers)
                )
                    .fold(
                        onSuccess = {
                            error.value = null
                        },
                        onFailure = {
                            if (it is CancellationException) {
                                return@launch
                            }
                            log.error(it) { "cannot set user $targetUser to role $powerLevel in room $selectedRoomId" }
                            error.value = i18n.settingsRoomMemberListChangePowerLevelError(targetUser.full)
                        })
            }
            changingPowerLevelDialogInput.update("")
        }
    }

    private fun validateNewPowerLevelInput(
        input: String,
        maxPowerLevel: Long?,
        i18n: I18n
    ): String? {
        val powerLevel = input.toIntOrNull()
        return when {
            maxPowerLevel == null -> i18n.settingsRoomMemberListChangePowerLevelInputValidationNotEntitled()

            powerLevel == null || powerLevel < 0 || powerLevel > 100 ->
                i18n.settingsRoomMemberListChangePowerLevelInputValidationShouldBeNumber(maxPowerLevel)

            powerLevel > maxPowerLevel ->
                i18n.settingsRoomMemberListChangePowerLevelInputValidationPowerLevelTooLow(maxPowerLevel)

            else -> null
        }
    }
}
