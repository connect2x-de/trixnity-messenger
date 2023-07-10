package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

interface CreateNewRoomViewModelFactory {
    fun newCreateNewRoomViewModel(
        viewModelContext: MatrixClientViewModelContext,
    ): CreateNewRoomViewModel {
        return CreateNewRoomViewModelImpl(viewModelContext)
    }
}

interface CreateNewRoomViewModel {
    val userSearchTerm: MutableStateFlow<String>
    val foundUsers: MutableStateFlow<List<SearchUserElement>>
    val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>>
    val waitForUserResults: MutableStateFlow<Boolean>
    val error: MutableStateFlow<String?>
}

open class CreateNewRoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
) : CreateNewRoomViewModel, MatrixClientViewModelContext by viewModelContext {
    protected val search = get<Search>()

    override val userSearchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val foundUsers: MutableStateFlow<List<SearchUserElement>> = MutableStateFlow(listOf())

    private val initialUsers: MutableStateFlow<List<SearchUserElement>> = MutableStateFlow(listOf())
    override val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>> = MutableStateFlow(emptyMap())

    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            initialUsers()
            searchLocalUsers()
        }
    }

    private fun initialUsers() {
        waitForUserResults.value = true
        initialUsers.value = listOf() // TODO show users in already known rooms
        waitForUserResults.value = false
    }

    @OptIn(FlowPreview::class)
    protected open suspend fun searchLocalUsers() {
        userSearchTerm
            .onEach { if (it.isBlank()) foundUsers.value = initialUsers.value }
            .debounce(300)
            .filter { it.isNotBlank() }
            .collect {
                waitForUserResults.value = true
                foundUsers.value =
                    search.searchUsers(matrixClient, it, 100)
                waitForUserResults.value = false
            }
    }

}

class PreviewCreateNewRoomViewModel : CreateNewRoomViewModel {
    override val userSearchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val foundUsers: MutableStateFlow<List<SearchUserElement>> = MutableStateFlow(emptyList())
    override val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>> = MutableStateFlow(emptyMap())
    override val waitForUserResults: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
}