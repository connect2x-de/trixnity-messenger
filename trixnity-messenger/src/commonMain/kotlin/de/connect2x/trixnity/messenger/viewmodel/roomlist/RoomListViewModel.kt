package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.*
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.utils.toByteArray
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

interface RoomListViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        selectedRoomId: StateFlow<RoomId?>,
        onRoomSelected: (UserId, RoomId) -> Unit,
        onStartCreateNewRoom: (UserId) -> Unit,
        onUserSettingsSelected: () -> Unit,
        onOpenAppInfo: () -> Unit,
        onSendLogs: () -> Unit,
        onOpenAccountsOverview: () -> Unit,
    ): RoomListViewModel {
        return RoomListViewModelImpl(
            viewModelContext,
            selectedRoomId,
            onRoomSelected,
            onStartCreateNewRoom,
            onUserSettingsSelected,
            onOpenAppInfo,
            onSendLogs,
            onOpenAccountsOverview,
        )
    }

    companion object : RoomListViewModelFactory
}

interface RoomListViewModel {
    val selectedRoomId: StateFlow<RoomId?>
    val error: StateFlow<String?>
    val errorType: StateFlow<ErrorType>
    val sortedRoomListElementViewModels: StateFlow<List<RoomListElement>>

    val syncStateError: StateFlow<Map<UserId, Boolean>>
    val allSyncError: StateFlow<Boolean>
    val initialSyncFinished: StateFlow<Boolean>

    val showSearch: MutableStateFlow<Boolean>
    val searchTerm: MutableStateFlow<String>

    val spaces: StateFlow<List<SpaceViewModel>>
    val activeSpace: MutableStateFlow<RoomId?>
    val showSpaces: MutableStateFlow<Boolean>

    val accountViewModel: AccountViewModel
    val canCreateNewRoomWithAccount: StateFlow<Boolean>
    fun createNewRoom()
    fun createNewRoomFor(userId: UserId)
    fun selectRoom(roomId: RoomId)
    fun errorDismiss()
    fun sendLogs()
    fun openAccountsOverview()
}

data class RoomListElement(
    val roomId: RoomId,
    val isDirect: Boolean,
    val isInvite: Boolean,
    val viewModel: RoomListElementViewModel
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RoomListViewModelImpl(
    viewModelContext: ViewModelContext,
    override val selectedRoomId: StateFlow<RoomId?>,
    private val onRoomSelected: (UserId, RoomId) -> Unit,
    private val onCreateNewRoom: (UserId) -> Unit,
    onUserSettingsSelected: () -> Unit,
    onOpenAppInfo: () -> Unit,
    private val onSendLogs: () -> Unit,
    private val onOpenAccountsOverview: () -> Unit,
) : ViewModelContext by viewModelContext, RoomListViewModel {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error = _error.asStateFlow()
    private val _errorType = MutableStateFlow(ErrorType.JUST_DISMISS)
    override val errorType = _errorType.asStateFlow()
    private val errorSelectedRoom = MutableStateFlow<RoomId?>(null)

    override val sortedRoomListElementViewModels: StateFlow<List<RoomListElement>>

    private val syncState: StateFlow<Map<UserId, SyncState>>
    override val syncStateError: StateFlow<Map<UserId, Boolean>>
    override val allSyncError: StateFlow<Boolean>
    override val initialSyncFinished: StateFlow<Boolean>

    override val showSearch = MutableStateFlow(false)
    override val searchTerm = MutableStateFlow("")

    override val spaces: StateFlow<List<SpaceViewModel>>
    override val activeSpace: MutableStateFlow<RoomId?> = MutableStateFlow(null)
    override val showSpaces: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val canCreateNewRoomWithAccount: StateFlow<Boolean>

    private val activeAccount: StateFlow<UserId?> =
        messengerSettings.map { it.selectedAccount }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    private val i18n = get<I18n>()
    private val roomName = get<RoomName>()
    private val initials = get<Initials>()

    override val accountViewModel =
        viewModelContext.get<AccountViewModelFactory>().create(
            viewModelContext = childContext("accountViewModel"),
            onAccountSelected = {
                // reset the active space as it might filter rooms in another account where it is not even present
                activeSpace.value = null
            },
            onUserSettingsSelected = onUserSettingsSelected,
            onShowAppInfo = onOpenAppInfo,
        )

    private val roomListElementViewModels = mutableMapOf<RoomId, RoomListElementViewModel>()

    private data class RoomWithMatrixClient(
        val room: Room,
        val matrixClient: MatrixClient,
    )

    private val selectedMatrixClients = combine(matrixClients, activeAccount) { matrixClients, activeAccount ->
        if (activeAccount != null) {
            listOfNotNull(matrixClients[activeAccount])
        } else {
            matrixClients.values.toList()
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

    private val allRoomsFlow: SharedFlow<Map<RoomId, RoomWithMatrixClient>> =
        selectedMatrixClients.flatMapLatest { selectedMatrixClients ->
            val allRoomsFlows = selectedMatrixClients.map { selectedMatrixClient ->
                selectedMatrixClient.room.getAll()
                    .flattenValues()
                    .map { rooms ->
                        rooms.map { room ->
                            RoomWithMatrixClient(room, selectedMatrixClient)
                        }
                    }
            }
            combine(allRoomsFlows) { combinedRooms ->
                combinedRooms.toList().flatten().associateBy { it.room.roomId }
            }
        }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

    init {
        data class ActiveSpaceInfo(
            val roomsInSpace: List<RoomId>,
            val usersInSpace: List<UserId>,
        )

        val activeSpaceInfoFlow =
            combine(selectedMatrixClients, activeSpace) { selectedMatrixClients, activeSpace ->
                if (activeSpace != null) {
                    val roomsInThisSpaceFlow =
                        combine(selectedMatrixClients
                            .map { selectedMatrixClient ->
                                log.trace { "get rooms in space ${activeSpace.full} for account ${selectedMatrixClient.userId}" }
                                activeSpace.roomsInThisSpace(selectedMatrixClient)
                            }) { it.toList().flatten() }
                    val usersInThisSpaceFlow =
                        combine(selectedMatrixClients.map { selectedMatrixClient ->
                            selectedMatrixClient.user.getAll(activeSpace).map { it.keys }
                        }) { it.toList().flatten() }
                    combine(roomsInThisSpaceFlow, usersInThisSpaceFlow) { roomsInThisSpace, usersInThisSpace ->
                        log.trace { "$activeSpace: rooms in this space -> $roomsInThisSpace, users in this space -> ${usersInThisSpace.joinToString { it.full }}" }
                        ActiveSpaceInfo(roomsInThisSpace, usersInThisSpace)
                    }
                } else flowOf(null)
            }.flatMapLatest { it }

        val directRoomsFlow = selectedMatrixClients.flatMapLatest { selectedMatrixClients ->
            combine(selectedMatrixClients
                .map { selectedMatrixClient ->
                    selectedMatrixClient.user.getAccountData<DirectEventContent>().map { it?.mappings.orEmpty() }
                }) { allDirectEventContents ->
                allDirectEventContents
                    .flatMap { it.entries }
                    .groupBy { it.key }
                    .mapValues { (_, allRooms) ->
                        allRooms.map { it.value.orEmpty() }.flatten()
                    }
            }
        }

        val allRoomNamesFlow = // This is a heavy operation! Use it with care!
            allRoomsFlow.flatMapLatest { allRooms ->
                combine(allRooms.map { (roomId, roomWithMeta) ->
                    roomName.getRoomName(
                        roomWithMeta.room,
                        roomWithMeta.matrixClient
                    ).map { roomId to it }
                }) { allRoomNames ->
                    allRoomNames.toMap()
                }
            }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

        val searchedRoomsFlow =
            combine(
                allRoomsFlow.map { it.keys },
                searchTerm.debounce { if (it.isBlank()) 0.milliseconds else 300.milliseconds }
            ) { allRoomIds, currentSearchTerm ->
                allRoomIds to currentSearchTerm
            }.flatMapLatest { (allRoomIds, currentSearchTerm) ->
                if (currentSearchTerm.isNotBlank()) {
                    allRoomNamesFlow.map { allRoomNames ->
                        allRoomNames.filter { (_, roomName) ->
                            roomName.contains(currentSearchTerm, ignoreCase = true)
                        }.keys
                    }
                } else {
                    flowOf(allRoomIds)
                }
            }

        sortedRoomListElementViewModels =
            combine(
                allRoomsFlow,
                activeSpaceInfoFlow,
                directRoomsFlow,
                searchedRoomsFlow
            ) { roomsWithMeta, activeSpaceInfo, directRooms, searchedRooms ->
                data class SortableRoom(
                    val roomWithMatrixClient: RoomWithMatrixClient,
                    val sortTime: Instant?,
                )
                roomsWithMeta.values.asFlow()
                    .filter { (room, _) ->
                        val isSpace = room.createEventContent?.type == RoomType.Space
                        val isInActiveSpace =
                            if (activeSpaceInfo != null) {
                                activeSpaceInfo.roomsInSpace.contains(room.roomId) ||
                                        (room.isDirect && directRooms.entries.find {
                                            it.value.contains(room.roomId)
                                        }?.let { otherUser ->
                                            activeSpaceInfo.usersInSpace.any { roomUser -> roomUser == otherUser.key }
                                        } ?: false)
                            } else true
                        val includedInSearch = searchedRooms.contains(room.roomId)
                        !isSpace &&
                                (room.membership == Membership.INVITE || room.membership == Membership.JOIN) &&
                                isInActiveSpace &&
                                includedInSearch
                    }.onEach { log.trace { "filtered rooms: $it" } }
                    .map<RoomWithMatrixClient, SortableRoom> { roomWithMeta -> // why map here? -> because we need the creation time and cannot call suspended functions in `sortedByDescending`
                        val room = roomWithMeta.room
                        val lastRelevantEventTime = room.lastRelevantEventTimestamp
                        val sortTime =
                            when {
                                room.membership == Membership.INVITE -> Instant.DISTANT_FUTURE
                                lastRelevantEventTime == null -> roomWithMeta.matrixClient
                                    .room.getState<CreateEventContent>(room.roomId, "").first()
                                    ?.originTimestamp?.let { Instant.fromEpochMilliseconds(it) }

                                else -> lastRelevantEventTime
                            }
                        SortableRoom(roomWithMeta, sortTime)
                    }.toList<SortableRoom>()
                    .sortedByDescending<SortableRoom, Instant> { (_, sortTime) -> sortTime }
                    .asFlow<SortableRoom>()
                    .map<SortableRoom, RoomWithMatrixClient> { it.roomWithMatrixClient }
                    .map { (room, matrixClient) ->
                        val roomId = room.roomId
                        val existingViewModel = roomListElementViewModels[roomId]
                        val viewModel = if (existingViewModel != null) existingViewModel else {
                            val roomListElementViewModel =
                                viewModelContext.get<RoomListElementViewModelFactory>().create(
                                    viewModelContext = childContext(
                                        "roomListElement-${roomId.full}",
                                        userId = matrixClient.userId,
                                    ),
                                    roomId,
                                    onRoomSelected = { onRoomSelected(matrixClient.userId, roomId) },
                                )
                            roomListElementViewModels[roomId] = roomListElementViewModel
                            roomListElementViewModel
                        }
                        RoomListElement(
                            roomId = roomId,
                            isDirect = room.isDirect,
                            isInvite = room.membership == Membership.INVITE,
                            viewModel = viewModel
                        )
                    }.toList()
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

        spaces = allRoomsFlow.flatMapLatest { allRooms ->
            combine( // TODO This is a heavy operation: SpaceViewModel should calculate room name.
                allRooms.values.asFlow()
                    .filter { (room, _) ->
                        val isSpace = room.createEventContent?.type == RoomType.Space
                        isSpace && (room.membership == Membership.INVITE || room.membership == Membership.JOIN)
                    }.map { (space, matrixClient) ->
                        roomName.getRoomName(space, matrixClient)
                            .map { roomName ->
                                val isInvite = space.membership == Membership.INVITE
                                val spaceImage = if (isInvite) {
                                    null
                                } else {
                                    space.avatarUrl?.let { avatarUrl ->
                                        matrixClient.media
                                            .getThumbnail(avatarUrl, avatarSize().toLong(), avatarSize().toLong())
                                            .fold(
                                                onSuccess = { it.toByteArray() },
                                                onFailure = {
                                                    log.error(it) { "Cannot load avatar of the space ${space.roomId}." }
                                                    null
                                                }
                                            )
                                    }
                                }
                                SpaceViewModel(
                                    space.roomId,
                                    roomName,
                                    spaceImage,
                                    initials.compute(roomName),
                                )
                            }
                    }.toList()
            ) { spaces ->
                spaces.toList()
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

        syncState = matrixClients.flatMapLatest { matrixClients ->
            combine(matrixClients.map { (userId, matrixClient) ->
                matrixClient.syncState.map { userId to it }
            }) {
                it.toMap()
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), mapOf())

        syncStateError = syncState.map {
            it.entries.associate { (userId, syncState) ->
                userId to (syncState == SyncState.ERROR)
            }
        }
            .debounce(3.seconds)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), mapOf())
        allSyncError = syncStateError.map {
            it.all { (_, error) -> error }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
        var initialSyncFinishedOnce = false
        initialSyncFinished = syncState
            .filterNot { it.isEmpty() }
            .map { initialSyncFinishedOnce.not() && it.values.all { syncState -> syncState == SyncState.RUNNING } }
            .map {
                log.debug { "all syncs RUNNING: $it" }
                if (it) initialSyncFinishedOnce = true
                it
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

        resetSpacesWhenNotShown()
        resetSearchWhenNotShown()

        canCreateNewRoomWithAccount =
            combine(accountViewModel.accounts, activeAccount) { allAccounts, activeAccount ->
                allAccounts.size == 1 || activeAccount != null
            }.stateIn(coroutineScope, SharingStarted.Eagerly, false) // has to eager as it is used as a helper
    }

    private fun resetSpacesWhenNotShown() {
        coroutineScope.launch {
            showSpaces.drop(1).collect {
                if (it.not()) {
                    activeSpace.value = null
                }
            }
        }
    }

    private fun resetSearchWhenNotShown() {
        coroutineScope.launch {
            showSearch.drop(1).collect {
                if (it.not()) {
                    searchTerm.value = ""
                }
            }
        }
    }

    override fun createNewRoom() {
        if (canCreateNewRoomWithAccount.value) {
            onCreateNewRoom(activeAccount.value ?: accountViewModel.accounts.value[0].userId)
        } else {
            log.warn { "This should be prevented: select an active account first, then create a room." }
        }
    }

    override fun createNewRoomFor(userId: UserId) {
        onCreateNewRoom(userId)
    }

    override fun selectRoom(roomId: RoomId) {
        coroutineScope.launch {
            val matrixClient = allRoomsFlow.first()[roomId]?.matrixClient
                ?: throw IllegalStateException("cannot find NamedMatrixClient for room $roomId")
            val isInvite =
                matrixClient.room.getById(roomId).filterNotNull().map { it.membership == Membership.INVITE }.first()
            log.debug { "switch to room $roomId (isInvite: $isInvite)" }
            when {
                isInvite && syncState.value[matrixClient.userId] == SyncState.ERROR -> {
                    log.debug { "try to join room while not connected" }
                    _errorType.value = ErrorType.JUST_DISMISS
                    _error.value = i18n.roomListInvitationOffline()
                }

                isInvite -> {
                    log.debug { "try to join room $roomId" }
                    matrixClient.api.rooms.joinRoom(roomId).fold(
                        onSuccess = {
                            onRoomSelected(matrixClient.userId, roomId)
                        },
                        onFailure = {
                            log.error(it) { "Cannot join room." }
                            errorSelectedRoom.value = roomId
                            _errorType.value = ErrorType.WITH_ACTION
                            _error.value = i18n.roomListInvitationError()
                        }
                    )
                }

                else -> onRoomSelected(matrixClient.userId, roomId)
            }
        }
    }

    override fun errorDismiss() {
        _error.value = null
        errorSelectedRoom.value = null
    }

    override fun sendLogs() {
        onSendLogs()
    }

    override fun openAccountsOverview() {
        onOpenAccountsOverview()
    }
}


class PreviewRoomListViewModel : RoomListViewModel {
    override val selectedRoomId: MutableStateFlow<RoomId?> = MutableStateFlow(null)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorType: MutableStateFlow<ErrorType> = MutableStateFlow(ErrorType.JUST_DISMISS)
    val roomId1 = RoomId("1", "localhost")
    val roomId2 = RoomId("2", "localhost")
    val roomId3 = RoomId("3", "localhost")
    override val sortedRoomListElementViewModels: MutableStateFlow<List<RoomListElement>> =
        MutableStateFlow(
            listOf(
                RoomListElement(roomId1, true, false, PreviewRoomListElementViewModel1()),
                RoomListElement(roomId2, false, false, PreviewRoomListElementViewModel2()),
                RoomListElement(roomId3, true, false, PreviewRoomListElementViewModel3()),
            )
        )
    override val syncStateError: MutableStateFlow<Map<UserId, Boolean>> = MutableStateFlow(mapOf())
    override val allSyncError: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val initialSyncFinished: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val showSearch: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val searchTerm: MutableStateFlow<String> = MutableStateFlow("")
    override val activeSpace: MutableStateFlow<RoomId?> = MutableStateFlow(null)
    override val spaces: MutableStateFlow<List<SpaceViewModel>> = MutableStateFlow(listOf())
    override val showSpaces: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val accountViewModel: AccountViewModel = PreviewAccountViewModel()
    override val canCreateNewRoomWithAccount: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun createNewRoom() {
    }

    override fun createNewRoomFor(userId: UserId) {
    }

    override fun selectRoom(roomId: RoomId) {
    }

    override fun errorDismiss() {
    }

    override fun sendLogs() {
    }

    override fun openAccountsOverview() {
    }
}
