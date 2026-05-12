package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.membership
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.getAccountData
import de.connect2x.trixnity.clientserverapi.model.room.CreateRoom
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.InitialStateEvent
import de.connect2x.trixnity.core.model.events.m.DirectEventContent
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface CreateNewChatViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        createNewRoomViewModel: CreateNewRoomViewModel,
        onCreateGroup: (UserId) -> Unit,
        onSearchGroup: (UserId) -> Unit,
        onCancel: () -> Unit,
    ): CreateNewChatViewModel =
        CreateNewChatViewModelImpl(
            viewModelContext = viewModelContext,
            createNewRoomViewModel = createNewRoomViewModel,
            onCreateGroup = onCreateGroup,
            onSearchGroup = onSearchGroup,
            onCancel = onCancel,
        )

    companion object : CreateNewChatViewModelFactory
}

interface CreateNewChatViewModel {
    val createNewRoomViewModel: CreateNewRoomViewModel
    val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility>
    val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?>
    val isCreating: StateFlow<Boolean>
    val error: StateFlow<String?>
    val errorDetails: StateFlow<String?>
    fun onUserClick(user: SearchUserElement)
    fun createGroup()
    fun searchGroup()
    fun errorDismiss()
    fun cancel()
}

open class CreateNewChatViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val createNewRoomViewModel: CreateNewRoomViewModel,
    private val onCreateGroup: (UserId) -> Unit,
    private val onSearchGroup: (UserId) -> Unit,
    private val onCancel: () -> Unit,
) : CreateNewChatViewModel,
    MatrixClientViewModelContext by viewModelContext {
    private val createNewRoomErrorFormatter = CreateNewRoomErrorFormatter(get())

    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries - HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)
    private val _isCreating = MutableStateFlow(false)
    override val isCreating: StateFlow<Boolean> = _isCreating

    private val backCallback = BackCallback {
        cancel()
    }

    init {
        registerBackCallback(backCallback)
        coroutineScope.launch {
            getAllDirectRooms()
        }
    }

    override fun createGroup() {
        onCreateGroup(userId)
    }

    override fun searchGroup() {
        onSearchGroup(userId)
    }

    override fun errorDismiss() {
        createNewRoomViewModel.error.value = null
    }

    override val error: StateFlow<String?> = createNewRoomViewModel.error.asStateFlow()
    override val errorDetails: StateFlow<String?> = createNewRoomViewModel.errorDetails.asStateFlow()

    override fun onUserClick(user: SearchUserElement) {
        val userId = user.userId
        coroutineScope.launch {
            val existingRoomIds = createNewRoomViewModel.existingDirectRooms.value[userId]
            if (
                existingRoomIds?.isNotEmpty() == true &&
                existingRoomIds.any {
                    val room = matrixClient.room.getById(it).first()
                    room != null && (room.membership == Membership.JOIN || room.membership == Membership.INVITE)
                }
            ) {
                log.debug { "Check whether there is already existing room with $userId" }
                // check whether the user left the room; if so, do NOT re-use the room
                existingRoomIds.find {
                    val membership = matrixClient.user.getById(it, userId).first()?.membership
                    (membership == Membership.JOIN || membership == Membership.INVITE || membership == Membership.KNOCK)
                            && (matrixClient.user.getAll(it).firstOrNull()?.size ?: 0) == 2
                }?.let { roomId ->
                    log.info { "go to existing room with $userId" }
                    createNewRoomViewModel.onRoomCreated(matrixClient.userId, roomId)
                } ?: run {
                    createNewRoom(userId)
                }
            } else {
                createNewRoom(userId)
            }
        }
    }

    override fun cancel() {
        onCancel()
    }

    private suspend fun getAllDirectRooms() {
        matrixClient.user.getAccountData<DirectEventContent>().collect {
            createNewRoomViewModel.existingDirectRooms.value = it?.mappings ?: emptyMap()
        }
    }

    private suspend fun createNewRoom(userId: UserId) {
        if (_isCreating.getAndUpdate { true }) {
            log.warn { "group creation is already in progress" }
            return
        }

        log.info { "create new room with $userId" }
        val encryption = listOf(InitialStateEvent(EncryptionEventContent(), ""))
        val historyVisibility = optionalRoomHistoryVisibility.value?.let {
            listOf(InitialStateEvent(content = HistoryVisibilityEventContent(it), ""))
        } ?: emptyList()
        matrixClient.api.room.createRoom(
            isDirect = true,
            invite = setOf(userId),
            initialState = encryption + historyVisibility,
            preset = CreateRoom.Request.Preset.TRUSTED_PRIVATE
        ).fold(
            onSuccess = { roomId ->
                log.debug { "created room ${roomId.full}" }
                createNewRoomViewModel.onRoomCreated(matrixClient.userId, roomId)
            },
            onFailure = {
                log.error(it) { "Cannot create room." }
                createNewRoomViewModel.error.value = createNewRoomErrorFormatter.error(it, isChat = true)
                createNewRoomViewModel.errorDetails.value = createNewRoomErrorFormatter.errorDetails(it)
            }
        )

        _isCreating.value = false
    }

}

class PreviewCreateNewChatViewModel : CreateNewChatViewModel {
    override val createNewRoomViewModel: CreateNewRoomViewModel = PreviewCreateNewRoomViewModel()

    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries - HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)
    override val isCreating: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorDetails: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun onUserClick(user: SearchUserElement) {}
    override fun createGroup() {}
    override fun searchGroup() {}
    override fun errorDismiss() {}
    override fun cancel() {}

}
