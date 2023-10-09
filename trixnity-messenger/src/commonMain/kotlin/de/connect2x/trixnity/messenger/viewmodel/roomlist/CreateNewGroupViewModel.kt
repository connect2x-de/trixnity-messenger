package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent


private val log = KotlinLogging.logger {}

interface CreateNewGroupViewModelFactory {
    fun newCreateNewGroupViewModel(
        viewModelContext: MatrixClientViewModelContext,
        createNewRoomViewModel: CreateNewRoomViewModel,
        onBack: () -> Unit,
        onGroupCreated: (String, RoomId) -> Unit,
    ): CreateNewGroupViewModel {
        return CreateNewGroupViewModelImpl(
            viewModelContext, createNewRoomViewModel, onBack, onGroupCreated
        )
    }
}

interface CreateNewGroupViewModel {
    val createNewRoomViewModel: CreateNewRoomViewModel
    val groupUsers: StateFlow<List<SearchUserElement>>
    val isPrivate: MutableStateFlow<Boolean>
    val isEncrypted: MutableStateFlow<Boolean>
    var optionalRoomName: MutableStateFlow<String>
    val canCreateNewGroup: StateFlow<Boolean>
    val error: StateFlow<String?>

    fun onUserClick(user: SearchUserElement)
    fun back()
    fun createNewGroup()
    fun errorDismiss()

    // IMPORTANT: has to be separate as the renderer will collapse when 2 collectAsState() references change at the same time
    fun removeUserFromList(user: SearchUserElement)
    fun removeUserFromGroup(user: SearchUserElement)
    fun addUserToList(user: SearchUserElement)
}

open class CreateNewGroupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val createNewRoomViewModel: CreateNewRoomViewModel,
    private val onBack: () -> Unit,
    private val onGroupCreated: (String, RoomId) -> Unit,
) : CreateNewGroupViewModel,
    MatrixClientViewModelContext by viewModelContext {
    override val isPrivate = MutableStateFlow(true)
    override val isEncrypted = MutableStateFlow(true)
    override var optionalRoomName = MutableStateFlow("")

    override val groupUsers = MutableStateFlow(listOf<SearchUserElement>())
    override val canCreateNewGroup: StateFlow<Boolean> = combine(isPrivate, isEncrypted) { private, encrypted ->
        !(private && !encrypted)
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val error: StateFlow<String?> = createNewRoomViewModel.error.asStateFlow()
    internal val foundUsers = createNewRoomViewModel.foundUsers.asStateFlow()

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun back() {
        onBack()
    }

    override fun createNewGroup() {
        if (canCreateNewGroup.value.not()) {
            log.warn { "cannot create new group, since canCreateNewGroup is false" }
            return
        }
        log.info { "create new group with ${groupUsers.value.joinToString { it.displayName }}" }
        val preset = if (isPrivate.value) {
            CreateRoom.Request.Preset.PRIVATE
        } else {
            CreateRoom.Request.Preset.PUBLIC
        }
        val encryption = if (isEncrypted.value) {
            listOf(Event.InitialStateEvent(content = EncryptionEventContent(), ""))
        } else {
            listOf()
        }
        val optionalName = optionalRoomName.value.ifBlank { null }

        coroutineScope.launch {
            matrixClient.api.rooms.createRoom(
                name = optionalName,
                preset = preset,
                isDirect = false,
                invite = groupUsers.value.map { it.userId }.toSet(),
                initialState = encryption,
            ).fold(
                onSuccess = { roomId ->
                    log.debug { "created room ${roomId.full}" }
                    onGroupCreated(accountName, roomId)
                },
                onFailure = {
                    log.error(it) { "Cannot create a group." }
                    createNewRoomViewModel.error.value = i18n.createNewGroupError()
                }
            )
        }
    }

    override fun errorDismiss() {
        createNewRoomViewModel.error.value = null
    }

    override fun onUserClick(user: SearchUserElement) {
        if (groupUsers.value.contains(user).not()) {
            groupUsers.value = groupUsers.value + user
            removeUserFromList(user)
        }
    }

    // IMPORTANT: has to be separate as the renderer will collapse when 2 collectAsState() references change at the same time
    override fun removeUserFromList(user: SearchUserElement) {
        coroutineScope.launch {
            delay(50)
            createNewRoomViewModel.foundUsers.value = createNewRoomViewModel.foundUsers.value - user
        }
    }

    override fun removeUserFromGroup(user: SearchUserElement) {
        groupUsers.value = groupUsers.value - user
        addUserToList(user)
    }

    override fun addUserToList(user: SearchUserElement) {
        coroutineScope.launch {
            delay(50)
            createNewRoomViewModel.foundUsers.value = createNewRoomViewModel.foundUsers.value + user
        }
    }
}

class PreviewCreateNewGroupViewModel : CreateNewGroupViewModel {
    override val createNewRoomViewModel: CreateNewRoomViewModel = PreviewCreateNewRoomViewModel()
    override val groupUsers: MutableStateFlow<List<SearchUserElement>> = MutableStateFlow(emptyList())
    override val isPrivate: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isEncrypted: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val canCreateNewGroup: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override var optionalRoomName: MutableStateFlow<String> = MutableStateFlow("")

    override fun onUserClick(user: SearchUserElement) {
    }

    override fun back() {
    }

    override fun createNewGroup() {
    }

    override fun errorDismiss() {
    }

    override fun removeUserFromList(user: SearchUserElement) {
    }

    override fun removeUserFromGroup(user: SearchUserElement) {
    }

    override fun addUserToList(user: SearchUserElement) {
    }

}
