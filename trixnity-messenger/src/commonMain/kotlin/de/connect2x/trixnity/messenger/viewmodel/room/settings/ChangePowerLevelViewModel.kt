package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent

private val log = KotlinLogging.logger {}

interface ChangePowerLevelViewModelFactory {
    fun newChangePowerLevelViewModelFactory(
        viewModelContext: MatrixClientViewModelContext,
        powerLevel: StateFlow<Int>,
        error: MutableStateFlow<String?>,
        selectedRoomId: RoomId,
        roomUser: RoomUser,
        closeMemberOptions: () -> Unit,
    ): ChangePowerLevelViewModel {
        return ChangePowerLevelViewModelImpl(
            viewModelContext,
            powerLevel,
            error,
            selectedRoomId,
            roomUser,
            closeMemberOptions,
        )
    }
}

interface ChangePowerLevelViewModel {

    val canSetRoleToUser: StateFlow<Boolean>
    val canSetRoleToAdmin: StateFlow<Boolean>
    val canSetRoleToModerator: StateFlow<Boolean>

    val changingRoleWarningDialogOpen: StateFlow<MemberListElementViewModel.Role?>
    val changingPowerLevelDialogOpen: StateFlow<Boolean>

    val showPowerLevelHelp: StateFlow<Boolean>

    val canSetPowerLevelToMax: StateFlow<Int?>

    val changingPowerLevelDialogInput: MutableStateFlow<InputWrapper>

    fun openChangingRoleWarningDialog(role: MemberListElementViewModel.Role)
    fun closeChangingRoleWarningDialog()

    fun openChangingPowerLevelDialog()
    fun closeChangingPowerLevelDialog()

    fun openPowerLevelHelp()
    fun closePowerLevelHelp()

    fun setRoleToUser()
    fun setRoleToModerator()
    fun setRoleToAdmin()
    fun setPowerLevelTo(level: Int)

    fun onPowerLevelEntered(input: String)

    @Parcelize
    data class InputWrapper(
        val value: String = "",
        val errorId: String? = null
    ) : Parcelable
}

open class ChangePowerLevelViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val powerLevel: StateFlow<Int>,
    val error: MutableStateFlow<String?>,
    private val selectedRoomId: RoomId,
    private val roomUser: RoomUser,
    private val closeMemberOptions: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, ChangePowerLevelViewModel {

    private val userId = roomUser.userId

    override val canSetPowerLevelToMax =
        matrixClient.user.canSetPowerLevelToMax(selectedRoomId, userId)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), 0)

    private val combineSetPowerLevelToMaxAndCurrentPowerLevel =
        canSetPowerLevelToMax.combine(powerLevel) { maxPowerLevel, currentPowerLevel -> maxPowerLevel to currentPowerLevel }
            .shareIn(coroutineScope, started = SharingStarted.WhileSubscribed(), 1)

    override val canSetRoleToUser =
        combineSetPowerLevelToMaxAndCurrentPowerLevel.map { (maxPowerLevel, currentPowerLevel) ->
            currentPowerLevel != MemberListElementViewModel.Role.USER.getMinPowerLevel() && (maxPowerLevel
                ?: -1) >= MemberListElementViewModel.Role.USER.getMinPowerLevel()
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val canSetRoleToAdmin =
        combineSetPowerLevelToMaxAndCurrentPowerLevel.map { (maxPowerLevel, currentPowerLevel) ->
            currentPowerLevel != MemberListElementViewModel.Role.ADMIN.getMinPowerLevel() && (maxPowerLevel
                ?: -1) >= MemberListElementViewModel.Role.ADMIN.getMinPowerLevel()
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val canSetRoleToModerator =
        combineSetPowerLevelToMaxAndCurrentPowerLevel.map { (maxPowerLevel, currentPowerLevel) ->
            currentPowerLevel != MemberListElementViewModel.Role.MODERATOR.getMinPowerLevel() && (maxPowerLevel
                ?: -1) >= MemberListElementViewModel.Role.MODERATOR.getMinPowerLevel()
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override val changingRoleWarningDialogOpen =
        MutableStateFlow<MemberListElementViewModel.Role?>(null)

    override val changingPowerLevelDialogOpen = MutableStateFlow(false)

    override val changingPowerLevelDialogInput =
        MutableStateFlow(ChangePowerLevelViewModel.InputWrapper())

    override val showPowerLevelHelp = MutableStateFlow(false)

    override fun setRoleToUser() =
        setUserToPowerLevel(MemberListElementViewModel.Role.USER.getMinPowerLevel())

    override fun setRoleToModerator() =
        setUserToPowerLevel(MemberListElementViewModel.Role.MODERATOR.getMinPowerLevel())

    override fun setRoleToAdmin() =
        setUserToPowerLevel(MemberListElementViewModel.Role.ADMIN.getMinPowerLevel())

    override fun setPowerLevelTo(level: Int) = setUserToPowerLevel(level)

    override fun openChangingRoleWarningDialog(role: MemberListElementViewModel.Role) {
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

    private fun setUserToPowerLevel(powerLevel: Int) {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                error.value = i18n.settingsRoomMemberListChangePowerLevelErrorOffline()
            } else {
                val oldPowerLevelsEventContent =
                    matrixClient.room.getState<PowerLevelsEventContent>(
                        roomId = selectedRoomId,
                        stateKey = ""
                    ).first()?.content ?: PowerLevelsEventContent()
                val newUsers = oldPowerLevelsEventContent.users.plus(userId to powerLevel)
                matrixClient.api.rooms.sendStateEvent(
                    roomId = selectedRoomId,
                    oldPowerLevelsEventContent.copy(users = newUsers)
                )
                    .fold(
                        onSuccess = {
                            closeMemberOptions()
                        },
                        onFailure = {
                            if (it is CancellationException) {
                                return@launch
                            }
                            log.error(it) { "cannot set user $userId to role $powerLevel in room $selectedRoomId" }
                            error.value = i18n.settingsRoomMemberListChangePowerLevelError(roomUser.name)
                        })
            }
            changingPowerLevelDialogInput.value =
                changingPowerLevelDialogInput.value.copy(value = "", null)
        }
    }

    override fun onPowerLevelEntered(input: String) {
        val errorId = validateNewPowerLevelInput(
            input,
            maxPowerLevel = canSetPowerLevelToMax.value,
            i18n
        )
        changingPowerLevelDialogInput.value =
            changingPowerLevelDialogInput.value.copy(value = input, errorId = errorId)
    }

    private fun validateNewPowerLevelInput(
        input: String,
        maxPowerLevel: Int?,
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