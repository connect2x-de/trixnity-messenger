package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.util.DefaultUserSearchHandler
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.util.nonInvitableMemberIds
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import org.koin.core.component.get

interface PotentialMembersViewModelFactory {
    fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): PotentialMembersViewModel {
        return PotentialMembersViewModelImpl(viewModelContext, roomId)
    }

    companion object : PotentialMembersViewModelFactory
}

interface PotentialMembersViewModel {
    val searchHandler: UserSearchHandler
    val selectedUsers: StateFlow<List<Search.SearchUserElement>>
    val offline: StateFlow<Boolean>
    val error: MutableStateFlow<String?>
}

open class PotentialMembersViewModelImpl(viewModelContext: MatrixClientViewModelContext, roomId: RoomId) :
    PotentialMembersViewModel, MatrixClientViewModelContext by viewModelContext {
    override val searchHandler: UserSearchHandler =
        DefaultUserSearchHandler(
            coroutineScope = coroutineScope,
            search = get<Search>(),
            client = matrixClient,
            filterNotUsers = nonInvitableMemberIds(matrixClient, roomId),
        )
    override val selectedUsers: StateFlow<List<Search.SearchUserElement>> = searchHandler.selectedUsers
    override val offline: StateFlow<Boolean> =
        matrixClient.syncState
            .transform { emit(it == SyncState.ERROR) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
}
