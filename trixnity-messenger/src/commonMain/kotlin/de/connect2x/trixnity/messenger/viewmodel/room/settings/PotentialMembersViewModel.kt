package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.util.DefaultUserSearchHandler
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.core.component.get

interface PotentialMembersViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId
    ): PotentialMembersViewModel {
        return PotentialMembersViewModelImpl(viewModelContext, roomId)
    }

    companion object : PotentialMembersViewModelFactory
}

interface PotentialMembersViewModel {
    val searchHandler: UserSearchHandler
    val offline: StateFlow<Boolean>
    val error: MutableStateFlow<String?>
}

open class PotentialMembersViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
) : PotentialMembersViewModel, MatrixClientViewModelContext by viewModelContext {
    private val maxAvatarSize = get<MatrixMessengerConfiguration>().maxMediaSizeInMemory
    override val searchHandler: UserSearchHandler =
        DefaultUserSearchHandler(
            coroutineScope,
            get<Search>(),
            matrixClient,
            maxAvatarSize = maxAvatarSize
        ) { userId ->
            currentMembers.value.contains(userId)
        }
    override val offline: StateFlow<Boolean> = matrixClient.syncState.transform { emit(it == SyncState.ERROR) }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    private val currentMembers =
        matrixClient.user.getAll(roomId).flatten()
            .mapNotNull { it?.values?.filterNotNull().orEmpty() }
            .map { roomUsers ->
                roomUsers.filterNot { it.membership == Membership.LEAVE }.map { it.userId }
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, hashSetOf())
}
