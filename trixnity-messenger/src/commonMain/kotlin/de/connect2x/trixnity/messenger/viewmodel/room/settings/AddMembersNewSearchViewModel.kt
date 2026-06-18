package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface AddMembersNewSearchViewModel : AddMembersViewModel, PotentialMembersNewSearchViewModel {
    val groupUsersNewSearch: StateFlow<List<UserSearchResult>>

    fun onUserClick(user: UserSearchResult)

    fun removeUserFromList(user: UserSearchResult)

    fun removeUserFromGroup(user: UserSearchResult)

    fun addUserToList(user: UserSearchResult)
}

class AddMembersNewSearchViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onBack: () -> Unit,
    addMembersViewModel: AddMembersViewModel,
    private val potentialMembersNewSearchViewModel: PotentialMembersNewSearchViewModel,
) :
    MatrixClientViewModelContext by viewModelContext,
    AddMembersViewModel by addMembersViewModel,
    PotentialMembersNewSearchViewModel by potentialMembersNewSearchViewModel,
    AddMembersNewSearchViewModel {

    private val _groupUsersNewSearch = MutableStateFlow<List<UserSearchResult>>(emptyList())
    override val groupUsersNewSearch: StateFlow<List<UserSearchResult>> = _groupUsersNewSearch.asStateFlow()

    private val _isAddingMembers = MutableStateFlow(false)
    override val isAddingMembers: StateFlow<Boolean> = _isAddingMembers.asStateFlow()

    private val _error = MutableStateFlow(addMembersViewModel.error.value)
    override val error: StateFlow<String?> =
        combine(_error, addMembersViewModel.error) { error, wrappedError ->
                if (error == null && wrappedError == null) null else error.orEmpty() + wrappedError.orEmpty()
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val _errorCause = MutableStateFlow(addMembersViewModel.errorCause.value)
    override val errorCause: StateFlow<String?> =
        combine(_errorCause, addMembersViewModel.errorCause) { errorCause, wrappedErrorCause ->
                if (errorCause == null && wrappedErrorCause == null) null
                else errorCause.orEmpty() + wrappedErrorCause.orEmpty()
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override fun onUserClick(user: UserSearchResult) {
        removeUserFromList(user)
    }

    override fun removeUserFromList(user: UserSearchResult) {
        _groupUsersNewSearch.value += user
        potentialMembersNewSearchViewModel.userSearchViewModel.filterNotSearchResult(user)
    }

    override fun removeUserFromGroup(user: UserSearchResult) {
        addUserToList(user)
    }

    override fun addUserToList(user: UserSearchResult) {
        _groupUsersNewSearch.value -= user
        potentialMembersNewSearchViewModel.userSearchViewModel.unfilterNotSearchResult(user)
    }

    override val canAddMembers =
        groupUsersNewSearch
            .map { it.isNotEmpty() }
            .stateIn(coroutineScope, started = SharingStarted.WhileSubscribed(), false)

    override fun addMembers() {
        _isAddingMembers.value = true

        log.info { "add ${groupUsersNewSearch.value.joinToString { it.displayName ?: it.userId.full }} to group" }
        coroutineScope.launch {
            val failedInvitations = mutableListOf<Pair<UserSearchResult, Throwable>>()
            for (user in groupUsersNewSearch.value) { // CHANGE
                matrixClient.api.room
                    .inviteUser(roomId, user.userId)
                    .fold(
                        onSuccess = { log.debug { "user ${user.userId.full} was invited" } },
                        onFailure = {
                            log.error(it) { "Failed to invite user ${user.userId.full}" }
                            log.trace { it.stackTraceToString() }
                            failedInvitations.add(user to it)
                        },
                    )
            }
            when (failedInvitations.count()) {
                0 -> {
                    _isAddingMembers.value = false
                    onBack()
                }

                1 -> {
                    val throwable = failedInvitations.first().second
                    _error.value =
                        i18n.settingsRoomAddMembersErrorSingular(
                            failedInvitations.first().first.displayName ?: failedInvitations.first().first.userId.full
                        )
                    _errorCause.value =
                        when {
                            potentialMembersViewModel.offline.value -> i18n.settingsRoomAddMembersErrorOffline()

                            throwable.message != null -> throwable.message
                            else -> throwable.stackTraceToString().lines().first()
                        }
                }

                else -> {
                    val throwable = failedInvitations.first().second

                    _error.value =
                        i18n.settingsRoomAddMembersErrorPlural(
                            failedInvitations
                                .joinTo(
                                    StringBuilder(),
                                    limit = failedInvitations.size - 1,
                                    postfix =
                                        " " +
                                            i18n.settingsRoomAddMembersAnd() +
                                            " \"" +
                                            failedInvitations.last().first.displayName +
                                            "\"",
                                ) {
                                    "\"" + it.first.displayName + "\""
                                }
                                .toString()
                        )
                    _errorCause.value =
                        when {
                            potentialMembersViewModel.offline.value -> i18n.settingsRoomAddMembersErrorOffline()

                            throwable.message != null -> throwable.message
                            else -> throwable.stackTraceToString().lines().first()
                        }
                }
            }
            _isAddingMembers.value = false
        }
    }

    override fun errorDismiss() {
        _error.value = null
    }
}
