package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom.Request.Preset.PRIVATE
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom.Request.Preset.PUBLIC
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent


private val log = KotlinLogging.logger {}

interface CreateNewGroupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        createNewRoomViewModel: CreateNewRoomViewModel,
        onBack: () -> Unit,
        onGroupCreated: (UserId, RoomId) -> Unit,
    ): CreateNewGroupViewModel {
        return CreateNewGroupViewModelImpl(
            viewModelContext, createNewRoomViewModel, onBack, onGroupCreated
        )
    }

    companion object : CreateNewGroupViewModelFactory
}

interface CreateNewGroupViewModel {
    val createNewRoomViewModel: CreateNewRoomViewModel
    val groupUsers: StateFlow<List<SearchUserElement>>
    val isPrivate: MutableStateFlow<Boolean>
    val isEncrypted: MutableStateFlow<Boolean>
    val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility>
    val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?>
    val optionalRoomName: TextFieldViewModel
    val optionalGroupTopic: TextFieldViewModel
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

    fun changeEncryptionStatus(newEncryptionStatus: Boolean)
    fun changeOptionalHistoryVisibility(newHistoryVisibility: HistoryVisibilityEventContent.HistoryVisibility)
    fun historyVisibilityCanBeChangedTo(newHistoryVisibility: HistoryVisibilityEventContent.HistoryVisibility): Boolean
}

open class CreateNewGroupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val createNewRoomViewModel: CreateNewRoomViewModel,
    private val onBack: () -> Unit,
    private val onGroupCreated: (UserId, RoomId) -> Unit,
) : CreateNewGroupViewModel,
    MatrixClientViewModelContext by viewModelContext {
    override val isPrivate = MutableStateFlow(true)
    override val isEncrypted = MutableStateFlow(true)
    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)
    override val optionalRoomName = TextFieldViewModelImpl()
    override var optionalGroupTopic = TextFieldViewModelImpl()

    override val groupUsers = MutableStateFlow(listOf<SearchUserElement>())
    override val canCreateNewGroup: StateFlow<Boolean> = combine(isPrivate, isEncrypted) { private, encrypted ->
        !(private && !encrypted)
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val error: StateFlow<String?> = createNewRoomViewModel.error.asStateFlow()

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
        val preset = when (isPrivate.value) {
            true -> PRIVATE
            false -> PUBLIC
        }
        val encryption = when (isEncrypted.value) {
            true -> listOf(InitialStateEvent(content = EncryptionEventContent(), ""))
            false -> emptyList()
        }
        val historyVisibility = optionalRoomHistoryVisibility.value?.let {
            listOf(InitialStateEvent(content = HistoryVisibilityEventContent(it), ""))
        } ?: emptyList()
        val optionalName = optionalRoomName.value.text.ifBlank { null }
        val optionalTopic = optionalGroupTopic.value.text.ifBlank { null }
        coroutineScope.launch {
            matrixClient.api.room.createRoom(
                name = optionalName,
                topic = optionalTopic,
                preset = preset,
                isDirect = false,
                invite = groupUsers.value.map { it.userId }.toSet(),
                initialState = encryption + historyVisibility,
            ).fold(
                onSuccess = { roomId ->
                    log.debug { "created room ${roomId.full}" }
                    onGroupCreated(userId, roomId)
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
            groupUsers.value += user
            removeUserFromList(user)
        }
    }

    // IMPORTANT: has to be separate as the renderer will collapse when 2 collectAsState() references change at the same time
    override fun removeUserFromList(user: SearchUserElement) {
        coroutineScope.launch {
            delay(50)
            createNewRoomViewModel.searchHandler.foundUsers.value -= user
        }
    }

    override fun removeUserFromGroup(user: SearchUserElement) {
        groupUsers.value -= user
        addUserToList(user)
    }

    override fun addUserToList(user: SearchUserElement) {
        coroutineScope.launch {
            delay(50)
            createNewRoomViewModel.searchHandler.foundUsers.value += user
        }
    }

    override fun changeEncryptionStatus(newEncryptionStatus: Boolean) {
        if (newEncryptionStatus &&
            optionalRoomHistoryVisibility.value == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
        ) {
            optionalRoomHistoryVisibility.value = HistoryVisibilityEventContent.HistoryVisibility.SHARED
        }
        isEncrypted.value = newEncryptionStatus
    }

    override fun changeOptionalHistoryVisibility(newHistoryVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        if (newHistoryVisibility == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
            && isEncrypted.value
        ) {
            log.error { "Cannot change room history visibility to 'WORLD_READABLE because the room is encrypted" }
        } else optionalRoomHistoryVisibility.value = newHistoryVisibility
    }

    override fun historyVisibilityCanBeChangedTo(newHistoryVisibility: HistoryVisibilityEventContent.HistoryVisibility): Boolean {
        return !(newHistoryVisibility == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE && isEncrypted.value)
    }
}

class PreviewCreateNewGroupViewModel : CreateNewGroupViewModel {
    override val createNewRoomViewModel: CreateNewRoomViewModel = PreviewCreateNewRoomViewModel()
    override val groupUsers: MutableStateFlow<List<SearchUserElement>> = MutableStateFlow(emptyList())
    override val isPrivate: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isEncrypted: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)
    override val canCreateNewGroup: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val optionalRoomName = TextFieldViewModelImpl()
    override val optionalGroupTopic = TextFieldViewModelImpl()

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

    override fun changeEncryptionStatus(newEncryptionStatus: Boolean) {
        if (newEncryptionStatus && optionalRoomHistoryVisibility.value == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE) {
            optionalRoomHistoryVisibility.value = HistoryVisibilityEventContent.HistoryVisibility.INVITED
        }
        isEncrypted.value = newEncryptionStatus
    }

    override fun changeOptionalHistoryVisibility(newHistoryVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        if (newHistoryVisibility == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE && isEncrypted.value) {
            log.error { "Cannot change room history visibility to 'WORLD_READABLE because the room is encrypted" }
        } else optionalRoomHistoryVisibility.value = newHistoryVisibility
    }

    override fun historyVisibilityCanBeChangedTo(newHistoryVisibility: HistoryVisibilityEventContent.HistoryVisibility): Boolean {
        return !(newHistoryVisibility == HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE && isEncrypted.value)
    }
}
