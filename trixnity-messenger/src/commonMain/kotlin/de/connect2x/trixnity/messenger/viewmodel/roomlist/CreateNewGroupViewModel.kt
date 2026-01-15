package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.Search.SearchUserElement
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom.Request.Preset.PRIVATE
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom.Request.Preset.PUBLIC
import net.folivo.trixnity.clientserverapi.model.rooms.DirectoryVisibility
import net.folivo.trixnity.core.model.events.InitialStateEvent
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface CreateNewGroupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        createNewRoomViewModel: CreateNewRoomViewModel,
        onBack: () -> Unit,
    ): CreateNewGroupViewModel {
        return CreateNewGroupViewModelImpl(
            viewModelContext, createNewRoomViewModel, onBack
        )
    }

    companion object : CreateNewGroupViewModelFactory
}

interface CreateNewGroupViewModel {
    val createNewRoomViewModel: CreateNewRoomViewModel
    val groupUsers: StateFlow<List<SearchUserElement>>
    val isPrivate: StateFlow<Boolean>
    fun setIsPrivate(isPrivate: Boolean)
    val directoryVisibilityIsPublic: StateFlow<Boolean>
    fun setDirectoryVisibilityIsPublic(setDirectoryVisibilityIsPublic: Boolean)
    val isEncrypted: MutableStateFlow<Boolean>
    val isCreating: StateFlow<Boolean>
    val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility>
    val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?>
    val optionalRoomName: TextFieldViewModel
    val optionalGroupTopic: TextFieldViewModel
    val canCreateNewGroup: StateFlow<Boolean>
    val error: StateFlow<String?>
    val errorDetails: StateFlow<String?>

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
) : CreateNewGroupViewModel,
    MatrixClientViewModelContext by viewModelContext {
    private val createNewRoomErrorFormatter = CreateNewRoomErrorFormatter(get())

    override val directoryVisibilityIsPublic = MutableStateFlow(false)
    override val isPrivate = MutableStateFlow(true)

    override fun setIsPrivate(isPrivate: Boolean) {
        this.isPrivate.value = isPrivate
        if (isPrivate)
            directoryVisibilityIsPublic.value = false
    }

    override fun setDirectoryVisibilityIsPublic(setDirectoryVisibilityIsPublic: Boolean) {
        if (!isPrivate.value) this.directoryVisibilityIsPublic.value = setDirectoryVisibilityIsPublic
    }

    override val isEncrypted = MutableStateFlow(true)
    private val _isCreating = MutableStateFlow(false)
    override val isCreating: StateFlow<Boolean> = _isCreating
    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)
    override val optionalRoomName = TextFieldViewModelImpl(maxLength = 1_000)
    override var optionalGroupTopic = TextFieldViewModelImpl(maxLength = 20_000)

    override val groupUsers = createNewRoomViewModel.searchHandler.selectedUsers
    override val canCreateNewGroup: StateFlow<Boolean> = combine(isPrivate, isEncrypted) { private, encrypted ->
        !(private && !encrypted)
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override val error: StateFlow<String?> = createNewRoomViewModel.error.asStateFlow()
    override val errorDetails: StateFlow<String?> = createNewRoomViewModel.errorDetails.asStateFlow()

    private val backCallback = BackCallback {
        back()
    }

    init {
        registerBackCallback(backCallback)
    }

    override fun back() {
        onBack()
    }

    override fun createNewGroup() {
        if (canCreateNewGroup.value.not()) {
            log.warn { "cannot create new group, since canCreateNewGroup is false" }
            return
        }
        if (_isCreating.getAndUpdate { true }) {
            log.warn { "group creation is already in progress" }
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
        val directoryVisibility =
            if (directoryVisibilityIsPublic.value) DirectoryVisibility.PUBLIC else DirectoryVisibility.PRIVATE
        coroutineScope.launch {
            matrixClient.api.room.createRoom(
                name = optionalName,
                topic = optionalTopic,
                preset = preset,
                isDirect = false,
                invite = groupUsers.value.map { it.userId }.toSet(),
                initialState = encryption + historyVisibility,
                visibility = directoryVisibility
            ).fold(
                onSuccess = { roomId ->
                    log.debug { "created room ${roomId.full}" }
                    createNewRoomViewModel.onRoomCreated(userId, roomId)
                },
                onFailure = {
                    log.error(it) { "Cannot create a group." }
                    createNewRoomViewModel.error.value = createNewRoomErrorFormatter.error(it, isChat = false)
                    createNewRoomViewModel.errorDetails.value = createNewRoomErrorFormatter.errorDetails(it)
                }
            )
        }.invokeOnCompletion { _isCreating.value = false }
    }

    override fun errorDismiss() {
        createNewRoomViewModel.error.value = null
    }

    override fun onUserClick(user: SearchUserElement) {
        if (groupUsers.value.contains(user).not()) {
            removeUserFromList(user)
        }
    }

    override fun removeUserFromList(user: SearchUserElement) {
        coroutineScope.launch {
            createNewRoomViewModel.searchHandler.selectUser(user)
        }
    }

    override fun removeUserFromGroup(user: SearchUserElement) {
        addUserToList(user)
    }

    override fun addUserToList(user: SearchUserElement) {
        coroutineScope.launch {
            createNewRoomViewModel.searchHandler.unselectUser(user)
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
    override fun setIsPrivate(isPrivate: Boolean) {}
    override val directoryVisibilityIsPublic: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun setDirectoryVisibilityIsPublic(setDirectoryVisibilityIsPublic: Boolean) {}
    override val isEncrypted: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isCreating: StateFlow<Boolean> = MutableStateFlow(false)
    override val availableRoomHistoryVisibilities: List<HistoryVisibilityEventContent.HistoryVisibility> =
        HistoryVisibilityEventContent.HistoryVisibility.entries
    override val optionalRoomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility?> =
        MutableStateFlow(null)
    override val canCreateNewGroup: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorDetails: MutableStateFlow<String?> = MutableStateFlow(null)
    override val optionalRoomName = TextFieldViewModelImpl(maxLength = 1_000)
    override val optionalGroupTopic = TextFieldViewModelImpl(maxLength = 20_000)

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
