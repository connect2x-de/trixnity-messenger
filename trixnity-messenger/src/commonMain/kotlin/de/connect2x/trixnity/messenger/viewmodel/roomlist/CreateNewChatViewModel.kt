package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership


private val log = KotlinLogging.logger {}

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
    val error: StateFlow<String?>
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

    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries - HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)

    private val backCallback = BackCallback {
        cancel()
    }

    init {
        backHandler.register(backCallback)
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

    override fun onUserClick(user: SearchUserElement) {
        val userId = user.userId
        coroutineScope.launch {
            val existingRoomIds = createNewRoomViewModel.existingDirectRooms.value[userId]
            if (existingRoomIds?.isNotEmpty() == true &&
                existingRoomIds.any { matrixClient.room.getById(it).first() != null }
            ) {
                log.debug { "Check whether there is already existing room with $userId" }
                // check whether the user left the room; if so, do NOT re-use the room
                existingRoomIds.find {
                    val membership = matrixClient.user.getById(it, userId).first()?.membership
                    membership == Membership.JOIN || membership == Membership.INVITE || membership == Membership.KNOCK
                }?.let {
                    log.info { "go to existing room with $userId" }
                    createNewRoomViewModel.onRoomCreated(matrixClient.userId, it)
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
                createNewRoomViewModel.error.value = i18n.createNewChatError()
            }
        )
    }

}

class PreviewCreateNewChatViewModel : CreateNewChatViewModel {
    override val createNewRoomViewModel: CreateNewRoomViewModel = PreviewCreateNewRoomViewModel()

    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries - HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun onUserClick(user: SearchUserElement) {}
    override fun createGroup() {}
    override fun searchGroup() {}
    override fun errorDismiss() {}
    override fun cancel() {}

}
