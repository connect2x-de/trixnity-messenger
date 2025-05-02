package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.DefaultUserSearchHandler
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.core.component.get

interface PotentialMembersViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
    ): PotentialMembersViewModel {
        return PotentialMembersViewModelImpl(viewModelContext, roomId)
    }

    companion object : PotentialMembersViewModelFactory
}

interface PotentialMembersViewModel {
    val selectedUsers: MutableStateFlow<List<Search.SearchUserElement>>
    val searchHandler: UserSearchHandler
    val offline: StateFlow<Boolean>
    val error: MutableStateFlow<String?>

    fun selectUser(user: Search.SearchUserElement)
    fun unselectUser(userId: Search.SearchUserElement)
}

open class PotentialMembersViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
) : PotentialMembersViewModel, MatrixClientViewModelContext by viewModelContext {
    private val maxAvatarSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    override val selectedUsers: MutableStateFlow<List<Search.SearchUserElement>> = MutableStateFlow(emptyList())
    private val addedMembers =
        combine(
            matrixClient.user.getAll(roomId)
                .flatten()
                .mapNotNull { it.values.filterNotNull() },
            selectedUsers
        ) { roomUsers, selectedUsers ->
            val roomUserIds = roomUsers.filterNot { it.membership == Membership.LEAVE }.map { it.userId }
            val selectedUserIds = selectedUsers.map { it.userId }

            setOf<UserId>() + roomUserIds + selectedUserIds
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptySet())
    override val searchHandler: UserSearchHandler =
        DefaultUserSearchHandler(
            coroutineScope,
            get<Search>(),
            matrixClient,
            maxAvatarSize = maxAvatarSize,
            skippedUsers = addedMembers
        )
    override val offline: StateFlow<Boolean> = matrixClient.syncState.transform { emit(it == SyncState.ERROR) }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun selectUser(user: Search.SearchUserElement) {
        selectedUsers.value = selectedUsers.value + user
    }

    override fun unselectUser(user: Search.SearchUserElement) {
        selectedUsers.value = selectedUsers.value - user
    }
}
