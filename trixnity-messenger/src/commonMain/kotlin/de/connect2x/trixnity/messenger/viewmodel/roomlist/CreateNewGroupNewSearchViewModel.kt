package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.clientserverapi.model.room.CreateRoom
import de.connect2x.trixnity.clientserverapi.model.room.DirectoryVisibility
import de.connect2x.trixnity.core.model.events.InitialStateEvent
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import org.koin.core.component.get

/**
 * Create a new group, using the new search.
 *
 * **Attention**: Be aware to not use properties or methods from [CreateNewGroupViewModel] that reference
 * [de.connect2x.trixnity.messenger.util.Search.SearchUserElement]. The replacements of those are using
 * [UserSearchResult].
 */
interface CreateNewGroupNewSearchViewModel : CreateNewGroupViewModel {
    val userSearchViewModel: UserSearchViewModel
    val groupUsersNewSearch: StateFlow<List<UserSearchResult>>

    fun onUserClick(user: UserSearchResult)

    fun removeUserFromList(user: UserSearchResult)

    fun removeUserFromGroup(user: UserSearchResult)

    fun addUserToList(user: UserSearchResult)
}

class CreateNewGroupNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    createNewGroupViewModel: CreateNewGroupViewModel,
) :
    CreateNewGroupNewSearchViewModel,
    CreateNewGroupViewModel by createNewGroupViewModel,
    MatrixClientViewModelContext by viewModelContext {
    override val userSearchViewModel: UserSearchViewModel =
        createNewGroupViewModel.createNewRoomViewModel.userSearchViewModel

    private val _groupUsersNewSearch = MutableStateFlow<List<UserSearchResult>>(emptyList())
    override val groupUsersNewSearch: StateFlow<List<UserSearchResult>> = _groupUsersNewSearch.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    override val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val createNewRoomErrorFormatter = CreateNewRoomErrorFormatter(get())

    override fun onUserClick(user: UserSearchResult) {
        removeUserFromList(user)
    }

    override fun removeUserFromList(user: UserSearchResult) {
        _groupUsersNewSearch.value += user
        userSearchViewModel.filterNotSearchResult(user)
    }

    override fun removeUserFromGroup(user: UserSearchResult) {
        addUserToList(user)
    }

    override fun addUserToList(user: UserSearchResult) {
        _groupUsersNewSearch.value -= user
        userSearchViewModel.unfilterNotSearchResult(user)
    }

    override fun createNewGroup() {
        if (canCreateNewGroup.value.not()) {
            log.warn { "cannot create new group, since canCreateNewGroup is false" }
            return
        }
        if (_isCreating.getAndUpdate { true }) {
            log.warn { "group creation is already in progress" }
            return
        }
        log.info {
            "create new group with ${groupUsersNewSearch.value.joinToString { it.displayName ?: it.userId.full }}"
        }
        val preset =
            when (isPrivate.value) {
                true -> CreateRoom.Request.Preset.PRIVATE
                false -> CreateRoom.Request.Preset.PUBLIC
            }
        val encryption =
            when (isEncrypted.value) {
                true -> listOf(InitialStateEvent(content = EncryptionEventContent(), ""))
                false -> emptyList()
            }
        val historyVisibility =
            optionalRoomHistoryVisibility.value?.let {
                listOf(InitialStateEvent(content = HistoryVisibilityEventContent(it), ""))
            } ?: emptyList()
        val optionalName = optionalRoomName.value.text.ifBlank { null }
        val optionalTopic = optionalGroupTopic.value.text.ifBlank { null }
        val directoryVisibility =
            if (directoryVisibilityIsPublic.value) DirectoryVisibility.PUBLIC else DirectoryVisibility.PRIVATE
        coroutineScope
            .launch {
                matrixClient.api.room
                    .createRoom(
                        name = optionalName,
                        topic = optionalTopic,
                        preset = preset,
                        isDirect = false,
                        invite = groupUsersNewSearch.value.map { it.userId }.toSet(), // CHANGE
                        initialState = encryption + historyVisibility,
                        visibility = directoryVisibility,
                    )
                    .fold(
                        onSuccess = { roomId ->
                            log.debug { "created room ${roomId.full}" }
                            createNewRoomViewModel.onRoomCreated(userId, roomId)
                        },
                        onFailure = {
                            log.error(it) { "Cannot create a group." }
                            createNewRoomViewModel.error.value = createNewRoomErrorFormatter.error(it, isChat = false)
                            createNewRoomViewModel.errorDetails.value = createNewRoomErrorFormatter.errorDetails(it)
                        },
                    )
            }
            .invokeOnCompletion { _isCreating.value = false }
    }
}
