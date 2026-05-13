package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.membership
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.clientserverapi.model.room.CreateRoom
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.InitialStateEvent
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.SearchUserViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface CreateNewChatNewSearchViewModel : CreateNewChatViewModel {
    val searchUserViewModel: SearchUserViewModel
    fun onUserClick(user: UserSearchResult)
}

class CreateNewChatNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val createNewChatViewModel: CreateNewChatViewModel,
) : CreateNewChatNewSearchViewModel,
    CreateNewChatViewModel by createNewChatViewModel,
    MatrixClientViewModelContext by viewModelContext {

    private val createNewRoomErrorFormatter = CreateNewRoomErrorFormatter(get())
    private val _isCreating = MutableStateFlow(false)
    override val isCreating: StateFlow<Boolean> = _isCreating

    override val searchUserViewModel = createNewChatViewModel.createNewRoomViewModel.searchUserViewModel

    // FIXME can we refactor the onUserClick to accept UserId? Breaking change?
    override fun onUserClick(user: UserSearchResult) {
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

    // FIXME this should be reused
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
