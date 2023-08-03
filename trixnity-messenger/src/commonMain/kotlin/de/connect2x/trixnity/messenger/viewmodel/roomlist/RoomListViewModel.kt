package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.NamedMatrixClient
import de.connect2x.trixnity.messenger.matrixClientOrThrow
import de.connect2x.trixnity.messenger.util.I18n
import de.connect2x.trixnity.messenger.viewmodel.RoomName
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.namedMatrixClients
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.getOriginTimestamp
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.flatten
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.user
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
    fun newRoomListViewModel(
        viewModelContext: ViewModelContext,
        selectedRoomId: StateFlow<RoomId?>,
        onRoomSelected: (String, RoomId) -> Unit,
        onCreateNewRoom: (String) -> Unit,
        onUserSettingsSelected: () -> Unit,
        onOpenAppInfo: () -> Unit,
        onSendLogs: () -> Unit,
        onOpenAccountsOverview: () -> Unit,
    ): RoomListViewModel {
        return RoomListViewModelImpl(
            viewModelContext,
            selectedRoomId,
            onRoomSelected,
            onCreateNewRoom,
            onUserSettingsSelected,
            onOpenAppInfo,
            onSendLogs,
            onOpenAccountsOverview,
        )
    }
}

interface RoomListViewModel {
    val selectedRoomId: StateFlow<RoomId?>
    val error: StateFlow<String?>
    val errorType: StateFlow<ErrorType>
    val sortedRoomListElementViewModels: StateFlow<List<Pair<RoomId, RoomListElementViewModel>>>

    val syncStateError: StateFlow<Map<String, Boolean>>
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
    fun createNewRoomFor(accountName: String)
    fun selectRoom(roomId: RoomId)
    fun errorDismiss()
    fun sendLogs()
    fun openAccountsOverview()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RoomListViewModelImpl(
    viewModelContext: ViewModelContext,
    override val selectedRoomId: StateFlow<RoomId?>,
    private val onRoomSelected: (String, RoomId) -> Unit,
    private val onCreateNewRoom: (String) -> Unit,
    onUserSettingsSelected: () -> Unit,
    onOpenAppInfo: () -> Unit,
    private val onSendLogs: () -> Unit,
    private val onOpenAccountsOverview: () -> Unit,
) : ViewModelContext by viewModelContext, RoomListViewModel {

    private val messengerSettings = get<MessengerSettings>()

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error = _error.asStateFlow()
    private val _errorType = MutableStateFlow(ErrorType.JUST_DISMISS)
    override val errorType = _errorType.asStateFlow()
    private val errorSelectedRoom = MutableStateFlow<RoomId?>(null)

    override val sortedRoomListElementViewModels: StateFlow<List<Pair<RoomId, RoomListElementViewModel>>>

    private val syncState: StateFlow<Map<String, SyncState>>
    override val syncStateError: StateFlow<Map<String, Boolean>>
    override val allSyncError: StateFlow<Boolean>
    override val initialSyncFinished: StateFlow<Boolean>

    override val showSearch = MutableStateFlow(false)
    override val searchTerm = MutableStateFlow("")

    override val spaces: StateFlow<List<SpaceViewModel>>
    override val activeSpace: MutableStateFlow<RoomId?> = MutableStateFlow(null)
    override val showSpaces: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val canCreateNewRoomWithAccount: StateFlow<Boolean>

    private val activeAccount: MutableStateFlow<String?> = MutableStateFlow(messengerSettings.activeAccount)
    private val i18n = get<I18n>()
    private val roomName = get<RoomName>()
    private val initials = get<Initials>()

    override val accountViewModel =
        viewModelContext.get<AccountViewModelFactory>().newAccountViewModel(
            viewModelContext = childContext("accountViewModel"),
            onAccountSelected = { accountName ->
                activeAccount.value = accountName
                // reset the active space as it might filter rooms in another account where it is not even present
                activeSpace.value = null
            },
            onUserSettingsSelected = onUserSettingsSelected,
            onShowAppInfo = onOpenAppInfo,
        )

    private val roomListElementViewModels = mutableMapOf<RoomId, RoomListElementViewModel>()

    private data class RoomWithMeta(
        val room: Room,
        val namedMatrixClient: NamedMatrixClient,
    )

    private val selectedMatrixClients = combine(namedMatrixClients, activeAccount) { nmc, account ->
        if (account != null) {
            nmc.filter { it.accountName == account }
        } else {
            nmc
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

    private val allRoomsFlow: SharedFlow<Map<RoomId, RoomWithMeta>> =
        selectedMatrixClients.flatMapLatest { namedMatrixClients ->
            val allRoomsFlows = namedMatrixClients.map { namedMatrixClient ->
                val matrixClient = namedMatrixClient.matrixClientOrThrow()
                matrixClient.room.getAll()
                    .flatten()
                    .map { rooms ->
                        rooms.map { room ->
                            RoomWithMeta(room, namedMatrixClient)
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
            combine(selectedMatrixClients, activeSpace) { namedMatrixClients, activeSpace ->
                if (activeSpace != null) {
                    val roomsInThisSpaceFlow =
                        combine(namedMatrixClients
                            .map { namedMatrixClient ->
                                val matrixClient = namedMatrixClient.matrixClientOrThrow()
                                log.trace { "get rooms in space ${activeSpace.full} for account ${namedMatrixClient.accountName}" }
                                activeSpace.roomsInThisSpace(matrixClient)
                            }) { it.toList().flatten() }
                    val usersInThisSpaceFlow =
                        combine(namedMatrixClients.map { namedMatrixClient ->
                            val matrixClient = namedMatrixClient.matrixClientOrThrow()
                            matrixClient.user.getAll(activeSpace).map { it?.keys.orEmpty() }
                        }) { it.toList().flatten() }
                    combine(roomsInThisSpaceFlow, usersInThisSpaceFlow) { roomsInThisSpace, usersInThisSpace ->
                        log.trace { "$activeSpace: rooms in this space -> $roomsInThisSpace, users in this space -> ${usersInThisSpace.joinToString { it.full }}" }
                        ActiveSpaceInfo(roomsInThisSpace, usersInThisSpace)
                    }
                } else flowOf(null)
            }.flatMapLatest { it }

        val directRoomsFlow = selectedMatrixClients.flatMapLatest { namedMatrixClients ->
            combine(namedMatrixClients
                .map { namedMatrixClient ->
                    val matrixClient = namedMatrixClient.matrixClientOrThrow()
                    matrixClient.user.getAccountData<DirectEventContent>().map { it?.mappings.orEmpty() }
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
                    roomName.getRoomNameElement(
                        roomWithMeta.room,
                        roomWithMeta.namedMatrixClient.matrixClientOrThrow()
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
                            roomName.roomName.contains(currentSearchTerm, ignoreCase = true)
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
                    val roomWithMeta: RoomWithMeta,
                    val sortTime: Instant?,
                )
                roomsWithMeta.values.asFlow()
                    .filter { (room, namedMatrixClient) ->
                        val matrixClient = namedMatrixClient.matrixClientOrThrow()
                        val isRoom =
                            matrixClient.room.getState<CreateEventContent>(room.roomId)
                                .first()?.content?.type == RoomType.Room
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
                        isRoom &&
                                (room.membership == Membership.INVITE || room.membership == Membership.JOIN) &&
                                isInActiveSpace &&
                                includedInSearch
                    }.onEach { log.trace { "filtered rooms: $it" } }
                    .map { roomWithMeta -> // why map here? -> because we need the creation time and cannot call suspended functions in `sortedByDescending`
                        val room = roomWithMeta.room
                        val lastRelevantEventTime = room.lastRelevantEventTimestamp
                        val sortTime =
                            when {
                                room.membership == Membership.INVITE -> Instant.DISTANT_FUTURE
                                lastRelevantEventTime == null -> roomWithMeta.namedMatrixClient.matrixClientOrThrow()
                                    .room.getState<CreateEventContent>(room.roomId, "").first()
                                    .getOriginTimestamp()?.let { Instant.fromEpochMilliseconds(it) }

                                else -> lastRelevantEventTime
                            }
                        SortableRoom(roomWithMeta, sortTime)
                    }.toList()
                    .sortedByDescending { (_, sortTime) -> sortTime }
                    .asFlow()
                    .map { it.roomWithMeta }
                    .map { (room, namedMatrixClient) ->
                        val roomId = room.roomId
                        val existingViewModel = roomListElementViewModels[roomId]
                        if (existingViewModel != null) roomId to existingViewModel
                        else {
                            val roomListElementViewModel =
                                viewModelContext.get<RoomListElementViewModelFactory>().newRoomListElementViewModel(
                                    viewModelContext = childContext(
                                        "roomListElement-${roomId.full}",
                                        accountName = namedMatrixClient.accountName,
                                    ),
                                    roomId,
                                    onRoomSelected = { onRoomSelected(namedMatrixClient.accountName, roomId) },
                                )
                            roomListElementViewModels[roomId] = roomListElementViewModel
                            roomId to roomListElementViewModel
                        }
                    }.toList()
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())

        spaces = allRoomsFlow.flatMapLatest { allRooms ->
            combine( // TODO This is a heavy operation: SpaceViewModel should calculate room name.
                allRooms.values.asFlow()
                    .filter { (room, namedMatrixClient) ->
                        val isSpace =
                            namedMatrixClient.matrixClientOrThrow().room
                                .getState<CreateEventContent>(room.roomId)
                                .first()?.content?.type == RoomType.Space
                        isSpace && (room.membership == Membership.INVITE || room.membership == Membership.JOIN)
                    }.map { (space, namedMatrixClient) ->
                        roomName.getRoomNameElement(space, namedMatrixClient.matrixClientOrThrow())
                            .map { it.roomName }
                            .map { roomName ->
                                val isInvite = space.membership == Membership.INVITE
                                val spaceImage = if (isInvite) {
                                    null
                                } else {
                                    space.avatarUrl?.let { avatarUrl ->
                                        namedMatrixClient.matrixClientOrThrow().media
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

        syncState = namedMatrixClients.flatMapLatest { namedMatrixClients ->
            combine(namedMatrixClients.map { namedMatrixClient ->
                namedMatrixClient.matrixClientOrThrow().syncState.map { namedMatrixClient.accountName to it }
            }) {
                it.toMap()
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), mapOf())

        syncStateError = syncState.map {
            it.entries.associate { (accountName, syncState) ->
                accountName to (syncState == SyncState.ERROR)
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
            combine(accountViewModel.allAccounts, activeAccount) { allAccounts, activeAccount ->
                allAccounts.size == 1 || activeAccount != null
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
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
            onCreateNewRoom(activeAccount.value ?: accountViewModel.allAccounts.value[0].accountName)
        } else {
            log.warn { "This should be prevented: select an active account first, then create a room." }
        }
    }

    override fun createNewRoomFor(accountName: String) {
        onCreateNewRoom(accountName)
    }

    override fun selectRoom(roomId: RoomId) {
        coroutineScope.launch {
            val namedMatrixClient = allRoomsFlow.first()[roomId]?.namedMatrixClient
                ?: throw IllegalStateException("cannot find NamedMatrixClient for room $roomId")
            val matrixClient = namedMatrixClient.matrixClientOrThrow()
            val isInvite =
                matrixClient.room.getById(roomId).filterNotNull().map { it.membership == Membership.INVITE }.first()
            log.debug { "switch to room $roomId (isInvite: $isInvite)" }
            when {
                isInvite && syncState.value[namedMatrixClient.accountName] == SyncState.ERROR -> {
                    log.debug { "try to join room while not connected" }
                    _errorType.value = ErrorType.JUST_DISMISS
                    _error.value = i18n.roomListInvitationOffline()
                }

                isInvite -> {
                    log.debug { "try to join room $roomId" }
                    matrixClient.api.rooms.joinRoom(roomId).fold(
                        onSuccess = {
                            onRoomSelected(namedMatrixClient.accountName, roomId)
                        },
                        onFailure = {
                            log.error(it) { "Cannot join room." }
                            errorSelectedRoom.value = roomId
                            _errorType.value = ErrorType.WITH_ACTION
                            _error.value = i18n.roomListInvitationError()
                        }
                    )
                }

                else -> onRoomSelected(namedMatrixClient.accountName, roomId)
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
    override val sortedRoomListElementViewModels: MutableStateFlow<List<Pair<RoomId, RoomListElementViewModel>>> =
        MutableStateFlow(
            listOf(
                roomId1 to PreviewRoomListElementViewModel1(),
                roomId2 to PreviewRoomListElementViewModel2(),
                roomId3 to PreviewRoomListElementViewModel3(),
            )
        )
    override val syncStateError: MutableStateFlow<Map<String, Boolean>> = MutableStateFlow(mapOf())
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

    override fun createNewRoomFor(accountName: String) {
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
