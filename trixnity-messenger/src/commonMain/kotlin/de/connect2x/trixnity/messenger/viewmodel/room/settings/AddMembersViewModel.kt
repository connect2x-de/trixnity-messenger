package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface AddMembersViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        potentialMembersViewModel: PotentialMembersViewModel,
        onBack: () -> Unit,
    ): AddMembersViewModel {
        return AddMembersViewModelImpl(viewModelContext, roomId, potentialMembersViewModel, onBack)
    }

    companion object : AddMembersViewModelFactory
}

interface AddMembersViewModel {
    val potentialMembersViewModel: PotentialMembersViewModel
    val groupUsers: StateFlow<List<Search.SearchUserElement>>
    val canAddMembers: StateFlow<Boolean>
    val error: StateFlow<String?>
    val errorCause: StateFlow<String?>
    val isAddingMembers: StateFlow<Boolean>
    val undecryptableHistoryInfo: StateFlow<String?>

    fun onUserClick(user: Search.SearchUserElement)

    fun addMembers()

    fun errorDismiss()

    fun back()

    // IMPORTANT: has to be separate as the renderer will collapse when 2 collectAsState() references change at the same
    // time
    fun removeUserFromList(user: Search.SearchUserElement)

    fun removeUserFromGroup(user: Search.SearchUserElement)

    fun addUserToList(user: Search.SearchUserElement)
}

class AddMembersViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    override val potentialMembersViewModel: PotentialMembersViewModel,
    private val onBack: () -> Unit,
) : AddMembersViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback { onBack() }

    init {
        registerBackCallback(backCallback)
    }

    override fun back() {
        onBack()
    }

    override val groupUsers = potentialMembersViewModel.selectedUsers
    override val canAddMembers =
        groupUsers.map { it.isNotEmpty() }.stateIn(coroutineScope, started = SharingStarted.WhileSubscribed(), false)

    override val error = MutableStateFlow<String?>(null)
    override val errorCause = MutableStateFlow<String?>(null)

    override val isAddingMembers = MutableStateFlow(false)

    override val undecryptableHistoryInfo =
        combine(
                matrixClient.room.getById(roomId).filterNotNull(),
                matrixClient.room.getState<HistoryVisibilityEventContent>(roomId).mapNotNull {
                    it?.content?.historyVisibility
                },
            ) { room, historyVisibility ->
                if (room.encrypted) {
                    when (historyVisibility) {
                        HistoryVisibilityEventContent.HistoryVisibility.INVITED,
                        HistoryVisibilityEventContent.HistoryVisibility.SHARED,
                        HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE ->
                            i18n.addMembersUndecryptableHistoryBeforeInvite()

                        HistoryVisibilityEventContent.HistoryVisibility.JOINED ->
                            i18n.addMembersUndecryptableHistoryBeforeJoin()
                    }
                } else {
                    null
                }
            }
            .stateIn(coroutineScope, started = SharingStarted.WhileSubscribed(), null)

    override fun addMembers() {
        isAddingMembers.value = true

        log.info { "add ${groupUsers.value.joinToString { it.displayName }} to group" }
        coroutineScope.launch {
            val failedInvitations = mutableListOf<Pair<Search.SearchUserElement, Throwable>>()
            for (user in groupUsers.value) {
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
                    isAddingMembers.value = false
                    onBack()
                }

                1 -> {
                    val throwable = failedInvitations.first().second
                    error.value = i18n.settingsRoomAddMembersErrorSingular(failedInvitations.first().first.displayName)
                    errorCause.value =
                        when {
                            potentialMembersViewModel.offline.value -> i18n.settingsRoomAddMembersErrorOffline()

                            throwable.message != null -> throwable.message
                            else -> throwable.stackTraceToString().lines().first()
                        }
                }

                else -> {
                    val throwable = failedInvitations.first().second

                    error.value =
                        i18n.settingsRoomAddMembersErrorPlural(
                            failedInvitations
                                .joinTo(
                                    StringBuilder(),
                                    limit = failedInvitations.size - 1,
                                    suffix =
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
                    errorCause.value =
                        when {
                            potentialMembersViewModel.offline.value -> i18n.settingsRoomAddMembersErrorOffline()

                            throwable.message != null -> throwable.message
                            else -> throwable.stackTraceToString().lines().first()
                        }
                }
            }
            isAddingMembers.value = false
        }
    }

    override fun errorDismiss() {
        error.value = null
    }

    override fun onUserClick(user: Search.SearchUserElement) {
        if (groupUsers.value.contains(user).not()) {
            removeUserFromList(user)
        }
    }

    override fun removeUserFromList(user: Search.SearchUserElement) {
        potentialMembersViewModel.searchHandler.selectUser(user)
    }

    override fun removeUserFromGroup(user: Search.SearchUserElement) {
        addUserToList(user)
    }

    override fun addUserToList(user: Search.SearchUserElement) {
        potentialMembersViewModel.searchHandler.unselectUser(user)
    }

    private fun <T, A : Appendable> Iterable<T>.joinTo(
        buffer: A,
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        suffix: CharSequence = "",
        limit: Int = -1,
        truncated: CharSequence = "",
        transform: ((T) -> CharSequence)? = null,
    ): A {
        buffer.append(prefix)
        var count = 0
        for (element in this) {
            if (++count > 1 && count <= limit) buffer.append(separator)
            if (limit < 0 || count <= limit) {
                when {
                    transform != null -> buffer.append(transform(element))
                    element is CharSequence? -> buffer.append(element)
                    element is Char -> buffer.append(element)
                    else -> buffer.append(element.toString())
                }
            } else break
        }
        if (limit in 0 until count) buffer.append(truncated)
        buffer.append(suffix)
        return buffer
    }
}
