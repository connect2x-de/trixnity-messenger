package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val userSearchTerm: MutableStateFlow<String>
    val foundUsers: MutableStateFlow<List<SearchUserElement>>
    val waitForUserResults: MutableStateFlow<Boolean>
    val offline: StateFlow<Boolean>
}

open class PotentialMembersViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    roomId: RoomId,
) : PotentialMembersViewModel, MatrixClientViewModelContext by viewModelContext {

    private val search = get<Search>()

    override val userSearchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val foundUsers: MutableStateFlow<List<SearchUserElement>> = MutableStateFlow(listOf())

    private val initialUsers: MutableStateFlow<List<SearchUserElement>> = MutableStateFlow(listOf())

    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override val offline: StateFlow<Boolean> = matrixClient.syncState.transform { emit(it == SyncState.ERROR) }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    private val currentMembers =
        matrixClient.user.getAll(roomId).flatten()
            .mapNotNull { it?.values?.filterNotNull().orEmpty() }
            .map { roomUsers ->
                roomUsers.filterNot { it.membership == Membership.LEAVE }.map { it.userId }
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, hashSetOf())

    init {
        coroutineScope.launch {
            // TODO initial: show users in already known rooms
            waitForUserResults.value = false
            searchLocalUsers()
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun searchLocalUsers() {
        userSearchTerm
            .onEach { if (it.isBlank()) foundUsers.value = initialUsers.value }
            .debounce(300)
            .filter { it.isNotBlank() }
            .collect {
                waitForUserResults.value = true
                foundUsers.value =
                    search.searchUsers(
                        matrixClient,
                        it,
                        100,
                        filterNot = { userId -> currentMembers.value.contains(userId) })
                waitForUserResults.value = false
            }
    }
}