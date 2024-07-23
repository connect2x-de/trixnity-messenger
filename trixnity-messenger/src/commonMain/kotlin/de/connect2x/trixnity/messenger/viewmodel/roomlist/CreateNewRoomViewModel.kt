package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.util.DefaultUserSearchHandler
import de.connect2x.trixnity.messenger.util.PreviewUserSearchHandler
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

interface CreateNewRoomViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext
    ): CreateNewRoomViewModel {
        return CreateNewRoomViewModelImpl(viewModelContext)
    }

    companion object : CreateNewRoomViewModelFactory
}

interface CreateNewRoomViewModel {
    val searchHandler: UserSearchHandler
    val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>>
    val error: MutableStateFlow<String?>
}

open class CreateNewRoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext
) : CreateNewRoomViewModel, MatrixClientViewModelContext by viewModelContext {
    override val searchHandler: UserSearchHandler =
        DefaultUserSearchHandler(coroutineScope, get<Search>(), matrixClient)
    override val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>> = MutableStateFlow(emptyMap())
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
}

class PreviewCreateNewRoomViewModel : CreateNewRoomViewModel {
    override val searchHandler: UserSearchHandler = PreviewUserSearchHandler
    override val existingDirectRooms: MutableStateFlow<Map<UserId, Set<RoomId>?>> = MutableStateFlow(emptyMap())
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
}
