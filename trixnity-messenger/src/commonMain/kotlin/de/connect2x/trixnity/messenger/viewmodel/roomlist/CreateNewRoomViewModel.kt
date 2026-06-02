package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.util.DefaultUserSearchHandler
import de.connect2x.trixnity.messenger.util.PreviewUserSearchHandler
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.search.PreviewUserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.component.get

interface CreateNewRoomViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onRoomCreated: (UserId, RoomId) -> Unit,
    ): CreateNewRoomViewModel {
        return CreateNewRoomViewModelImpl(viewModelContext, onRoomCreated)
    }

    companion object : CreateNewRoomViewModelFactory
}

interface CreateNewRoomViewModel {
    val searchHandler: UserSearchHandler
    val userSearchViewModel: UserSearchViewModel
    val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>>
    val error: MutableStateFlow<String?>
    val errorDetails: MutableStateFlow<String?>
    val onRoomCreated: (UserId, RoomId) -> Unit
}

open class CreateNewRoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val onRoomCreated: (UserId, RoomId) -> Unit,
) : CreateNewRoomViewModel, MatrixClientViewModelContext by viewModelContext {
    override val searchHandler: UserSearchHandler =
        DefaultUserSearchHandler(coroutineScope, get<Search>(), matrixClient)
    override val userSearchViewModel: UserSearchViewModel = get<UserSearchViewModelFactory>().create(viewModelContext)
    override val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>> = MutableStateFlow(emptyMap())
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorDetails: MutableStateFlow<String?> = MutableStateFlow(null)
}

class PreviewCreateNewRoomViewModel : CreateNewRoomViewModel {
    override val searchHandler: UserSearchHandler = PreviewUserSearchHandler
    override val userSearchViewModel: UserSearchViewModel = PreviewUserSearchViewModel()
    override val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>> = MutableStateFlow(emptyMap())
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorDetails: MutableStateFlow<String?> = MutableStateFlow(null)
    override val onRoomCreated: (UserId, RoomId) -> Unit = { _, _ -> }
}
