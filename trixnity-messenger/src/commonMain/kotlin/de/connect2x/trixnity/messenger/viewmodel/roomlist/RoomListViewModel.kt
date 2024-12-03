package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.UrlHandler
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTrigger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.room
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
        onAccountSelected: () -> Unit,
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
            onAccountSelected
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

    val accountViewModel: AccountViewModel
    val canCreateNewRoomWithAccount: StateFlow<Boolean>

    /**
     * Whether the UI should show means of closing the current profile.
     */
    val closeProfileNeeded: Boolean

    val unverifiedAccounts: StateFlow<List<UserId>>

    fun createNewRoom()
    fun createNewRoomFor(userId: UserId)
    fun selectRoom(roomId: RoomId)
    fun errorDismiss()
    fun sendLogs()
    fun openAccountsOverview()
    fun verifyAccount(userId: UserId)

    /**
     * Close the current profile to allow other users (tenants) to use the app without exposing personal data.
     */
    fun closeProfile()
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
    private val onAccountSelected: () -> Unit, // TODO provide userId as argument?
) : ViewModelContext by viewModelContext, RoomListViewModel {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val profileManager = getOrNull<ProfileManager>()

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

    override val canCreateNewRoomWithAccount: StateFlow<Boolean>

    private val activeAccount: StateFlow<UserId?> =
        messengerSettings.map { it.base.selectedAccount }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    private val i18n = get<I18n>()
    private val roomName = get<RoomName>()
    private val initials = get<Initials>()

    override val unverifiedAccounts = viewModelContext.matrixClients
        .flatMapLatest { clients ->
            combine(
                clients.entries.map {
                    it.value.key.getTrustLevel(it.value.userId, it.value.deviceId)
                        .map { trustLevel -> it.value.userId to trustLevel }
                }
            ) { pairs ->
                pairs.filter { it.second.isVerified.not() }
                    .map { it.first }
            }
        }.combine(activeAccount) { unverifiedAccounts, activeAccount ->
            if (activeAccount != null) {
                unverifiedAccounts.find {
                    it == activeAccount
                }?.let { listOf(it) } ?: listOf()
            } else unverifiedAccounts
        }
        .stateIn(coroutineScope, WhileSubscribed(), listOf())

    override val accountViewModel =
        viewModelContext.get<AccountViewModelFactory>().create(
            viewModelContext = childContext("accountViewModel"),
            onAccountSelected = { onAccountSelected() },
            onUserSettingsSelected = onUserSettingsSelected,
            onShowAppInfo = onOpenAppInfo,
        )

    private val roomListElementViewModels = mutableMapOf<RoomId, RoomListElementViewModel>()

    private val selfVerificationTrigger = get<SelfVerificationTrigger>()

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
    }.stateIn(coroutineScope, WhileSubscribed(), listOf())

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
        }.shareIn(coroutineScope, WhileSubscribed(), 1)

    init {
        val directRoomsFlow = selectedMatrixClients.flatMapLatest { selectedMatrixClients ->
            combine(
                selectedMatrixClients
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
            }.shareIn(coroutineScope, WhileSubscribed(), 1)

        val searchedRoomsFlow =
            combine(
                allRoomsFlow.map { it.keys },
                searchTerm.debounce { if (it.isBlank()) 0.milliseconds else 300.milliseconds },
            ) { allRoomIds, currentSearchTerm ->
                allRoomIds to currentSearchTerm
            }.flatMapLatest { (allRoomIds, currentSearchTerm) ->
                if (currentSearchTerm.isNotBlank()) {
                    allRoomNamesFlow.map { allRoomNames ->
                        allRoomNames.filter { (_, roomName) ->
                            roomName.contains(currentSearchTerm, ignoreCase = true)
                        }.keys
                    }
                } else flowOf(allRoomIds)
            }

        sortedRoomListElementViewModels =
            combine(
                allRoomsFlow,
                directRoomsFlow,
                searchedRoomsFlow,
            ) { roomsWithMeta, directRooms, searchedRooms ->
                data class SortableRoom(
                    val roomWithMatrixClient: RoomWithMatrixClient,
                    val sortTime: Instant?,
                )
                roomsWithMeta.values.asFlow()
                    .filter { (room, _) ->
                        val isSpace = room.createEventContent?.type == RoomType.Space
                        val includedInSearch = searchedRooms.contains(room.roomId)
                        val isDisplayed = !isSpace &&
                                (room.membership == Membership.INVITE || room.membership == Membership.JOIN) &&
                                includedInSearch
                        isDisplayed
                    }.onEach { log.trace { "filtered rooms: $it" } }
                    .map<RoomWithMatrixClient, SortableRoom> { roomWithMeta ->
                        // Use `map` to get the creation time here since `sortedByDescending` won't support suspended function calls.
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
                            viewModel = viewModel,
                        )
                    }.toList()
            }.stateIn(coroutineScope, WhileSubscribed(), listOf())

        syncState = matrixClients.flatMapLatest { matrixClients ->
            combine(matrixClients.map { (userId, matrixClient) ->
                matrixClient.syncState.map { userId to it }
            }) {
                it.toMap()
            }
        }.stateIn(coroutineScope, WhileSubscribed(), mapOf())

        syncStateError = syncState.map {
            it.entries.associate { (userId, syncState) ->
                userId to (syncState == SyncState.ERROR)
            }
        }
            .debounce(3.seconds)
            .stateIn(coroutineScope, WhileSubscribed(), mapOf())
        allSyncError = syncStateError.map {
            it.all { (_, error) -> error }
        }.stateIn(coroutineScope, WhileSubscribed(), false)
        var initialSyncFinishedOnce = false
        initialSyncFinished = syncState
            .filterNot { it.isEmpty() }
            .map { initialSyncFinishedOnce.not() && it.values.all { syncState -> syncState == SyncState.RUNNING } }
            .map {
                log.debug { "all syncs RUNNING: $it" }
                if (it) initialSyncFinishedOnce = true
                it
            }
            .stateIn(coroutineScope, WhileSubscribed(), false)

        resetSearchWhenNotShown()

        canCreateNewRoomWithAccount =
            combine(accountViewModel.accounts, activeAccount) { allAccounts, activeAccount ->
                allAccounts.size == 1 || activeAccount != null
            }.stateIn(coroutineScope, Eagerly, false) // Has to be `Eagerly` as it is used as a helper.

        // Handle room navigation requests through the timmy://localhost/room/<ID> scheme.
        coroutineScope.launch {
            get<UrlHandler>().collect {
                val segments = it.rawSegments
                if (segments.size < 3 || segments[1] != "room") return@collect
                selectRoom(RoomId(segments[2]))
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
                ?: return@launch log.error { "cannot find NamedMatrixClient for room $roomId" }
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
                    matrixClient.api.room.joinRoom(roomId).fold(
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

    override val closeProfileNeeded: Boolean = getOrNull<MatrixMultiMessengerConfiguration>()
        ?.multiProfile ?: false

    override fun closeProfile() {
        log.debug { "close profile" }
        coroutineScope.launch {
            profileManager?.closeProfile()
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

    override fun verifyAccount(userId: UserId) {
        coroutineScope.launch {
            selfVerificationTrigger.invoke(userId)
        }
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
    override val accountViewModel: AccountViewModel = PreviewAccountViewModel()
    override val canCreateNewRoomWithAccount: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val unverifiedAccounts: StateFlow<List<UserId>> = MutableStateFlow(listOf())
    override val closeProfileNeeded: Boolean = true

    override fun createNewRoom() {}
    override fun createNewRoomFor(userId: UserId) {}
    override fun selectRoom(roomId: RoomId) {}
    override fun errorDismiss() {}
    override fun sendLogs() {}
    override fun openAccountsOverview() {}
    override fun closeProfile() {}
    override fun verifyAccount(userId: UserId) {}
}
