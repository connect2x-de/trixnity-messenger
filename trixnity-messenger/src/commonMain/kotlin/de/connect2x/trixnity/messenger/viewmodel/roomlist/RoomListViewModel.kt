package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.multi.ProfileManager
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.UriHandler
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel.UserSyncStates
import de.connect2x.trixnity.messenger.viewmodel.util.ErrorType
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import io.ktor.http.Url
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flattenValues
import de.connect2x.trixnity.client.key
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.getAccountData
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.DirectEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent.RoomType
import de.connect2x.trixnity.core.model.events.m.room.Membership
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

interface RoomListViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        selectedRoomId: StateFlow<RoomId?>,
        onRoomSelected: (UserId, RoomId) -> Unit,
        onStartCreateNewRoom: (UserId) -> Unit,
        onUserSettingsSelected: () -> Unit,
        onShowAccounts: () -> Unit,
        onOpenAppInfo: () -> Unit,
        onSendLogs: () -> Unit,
        onAccountSelected: () -> Unit,
        onStartVerification: (UserId) -> Unit,
        onCloseRoom: () -> Unit
    ): RoomListViewModel {
        return RoomListViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            onRoomSelected = onRoomSelected,
            onCreateNewRoom = onStartCreateNewRoom,
            onUserSettingsSelected = onUserSettingsSelected,
            onShowAccounts = onShowAccounts,
            onOpenAppInfo = onOpenAppInfo,
            onSendLogs = onSendLogs,
            onAccountSelected = onAccountSelected,
            onStartVerification = onStartVerification,
            onCloseRoom = onCloseRoom
        )
    }

    companion object : RoomListViewModelFactory
}

interface RoomListViewModel {
    val selectedRoomId: StateFlow<RoomId?>
    val elements: StateFlow<List<RoomListElementViewModel>>
    val error: StateFlow<String?>
    val errorType: StateFlow<ErrorType>
    val initialSyncFinished: StateFlow<Boolean>
    val syncStates: StateFlow<UserSyncStates>

    val showSearch: MutableStateFlow<Boolean>
    val searchResultsEmpty: StateFlow<Boolean>
    val searchTerm: TextFieldViewModel

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
    fun verifyAccount(userId: UserId)

    /**
     * Close the current profile to allow other users (tenants) to use the app without exposing personal data.
     */
    fun closeProfile()

    data class UserSyncStates(
        val operationalFor: Set<UserId>,
        val failedFor: Set<UserId>,
    ) {
        val operationalForAll get() = operationalFor.isNotEmpty() && failedFor.isEmpty()
        val failedForAll get() = operationalFor.isEmpty() && failedFor.isNotEmpty()
        fun joinFailedToString() = failedFor.joinToString { it.full }
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RoomListViewModelImpl(
    viewModelContext: ViewModelContext,
    override val selectedRoomId: StateFlow<RoomId?>,
    private val onRoomSelected: (UserId, RoomId) -> Unit,
    private val onCreateNewRoom: (UserId) -> Unit,
    onUserSettingsSelected: () -> Unit,
    onShowAccounts: () -> Unit,
    onOpenAppInfo: () -> Unit,
    private val onSendLogs: () -> Unit,
    private val onAccountSelected: () -> Unit, // TODO provide userId as argument?
    private val onStartVerification: (userId: UserId) -> Unit,
    onCloseRoom: () -> Unit
) : ViewModelContext by viewModelContext, RoomListViewModel {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val profileManager = getOrNull<ProfileManager>()

    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error = _error.asStateFlow()
    private val _errorType = MutableStateFlow(ErrorType.JUST_DISMISS)
    override val errorType = _errorType.asStateFlow()
    private val errorSelectedRoom = MutableStateFlow<RoomId?>(null)

    override val elements: StateFlow<List<RoomListElementViewModel>>

    override val initialSyncFinished: StateFlow<Boolean>
    private val _syncState: StateFlow<Map<UserId, SyncState>>
    override val syncStates: StateFlow<UserSyncStates>

    override val showSearch = MutableStateFlow(false)
    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)
    override val searchResultsEmpty: StateFlow<Boolean>

    override val canCreateNewRoomWithAccount: StateFlow<Boolean>

    private val activeAccount: StateFlow<UserId?> =
        messengerSettings.map { it.base.selectedAccount }
            .stateIn(coroutineScope, WhileSubscribed(), null)
    private val i18n = get<I18n>()
    private val roomName = get<RoomName>()

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
            onShowAccounts = onShowAccounts,
        )

    private data class RoomListElementViewModelWrapper(
        val viewModel: RoomListElementViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val elementCache = mutableMapOf<RoomId, RoomListElementViewModelWrapper>()

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
                searchTerm.debounce { if (it.text.isBlank()) 0.milliseconds else 300.milliseconds },
            ) { allRoomIds, currentSearchTerm ->
                allRoomIds to currentSearchTerm.text.trim()
            }.flatMapLatest { (allRoomIds, currentSearchTerm) ->
                if (currentSearchTerm.isNotBlank()) {
                    allRoomNamesFlow.map { allRoomNames ->
                        allRoomNames.filter { (_, roomName) ->
                            roomName.contains(currentSearchTerm, ignoreCase = true)
                        }.keys
                    }
                } else flowOf(allRoomIds)
            }

        elements =
            combine(
                allRoomsFlow,
                directRoomsFlow,
                searchedRoomsFlow,
            ) { roomsWithMeta, _, searchedRooms ->
                data class SortableRoom(
                    val roomWithMatrixClient: RoomWithMatrixClient,
                    val sortTime: Instant?,
                )

                val relevantRooms = roomsWithMeta.values.asFlow()
                    .filter { (room, _) ->
                        val isSpace = room.createEventContent?.type == RoomType.Space
                        val includedInSearch = searchedRooms.contains(room.roomId)
                        val isDisplayed = !isSpace &&
                                (room.membership == Membership.INVITE || room.membership == Membership.JOIN || room.membership == Membership.LEAVE || room.membership == Membership.KNOCK) &&
                                includedInSearch
                        isDisplayed
                    }.onEach { log.trace { "filtered rooms: $it" } }
                    .map { roomWithMeta ->
                        // Use `map` to get the creation time here since `sortedByDescending` won't support suspended function calls.
                        val room = roomWithMeta.room
                        val lastRelevantEventTime = room.lastRelevantEventTimestamp
                        val sortTime =
                            when {
                                room.membership == Membership.INVITE -> Instant.DISTANT_FUTURE
                                room.membership == Membership.KNOCK -> Instant.DISTANT_FUTURE - 1.seconds
                                room.membership == Membership.LEAVE -> Instant.fromEpochMilliseconds(0)
                                lastRelevantEventTime == null -> roomWithMeta.matrixClient
                                    .room.getState<CreateEventContent>(room.roomId, "").first()
                                    ?.originTimestamp?.let { Instant.fromEpochMilliseconds(it) }

                                else -> lastRelevantEventTime
                            }
                        SortableRoom(roomWithMeta, sortTime)
                    }.toList()
                    .sortedByDescending { (_, sortTime) -> sortTime }
                    .asFlow()
                    .map { it.roomWithMatrixClient }
                    .toList()
                    .associate { it.room.roomId to it.matrixClient.userId }

                elementCache.mapNotNull { (key, wrapper) ->
                    if (relevantRooms[key] == null) {
                        wrapper.lifecycle.destroy()
                        key
                    } else null
                }.forEach { key -> elementCache.remove(key) }

                relevantRooms.map { (roomId, userId) ->
                    elementCache[roomId]?.viewModel
                        ?: run {
                            val lifecycleRegistry = LifecycleRegistry()
                            lifecycleRegistry.start()
                            viewModelContext.get<RoomListElementViewModelFactory>().create(
                                viewModelContext = childContextWithOwnLifecycle(
                                    lifecycle = lifecycleRegistry,
                                    userId = userId,
                                    name = roomId.full
                                ),
                                roomId,
                                onRoomSelected = { onRoomSelected(userId, roomId) },
                                onCloseRoom = { onCloseRoom() }
                            ).also {
                                elementCache[roomId] = RoomListElementViewModelWrapper(it, lifecycleRegistry)
                            }
                        }
                }.toList()
            }.stateIn(coroutineScope, WhileSubscribed(), listOf())

        searchResultsEmpty =
            combine(
                elements,
                allRoomsFlow
            ) { roomElements, allRooms -> allRooms.isNotEmpty() && roomElements.isEmpty() }
                .stateIn(coroutineScope, WhileSubscribed(), false)

        _syncState = matrixClients.flatMapLatest { matrixClients ->
            combine(matrixClients.map { (userId, matrixClient) ->
                matrixClient.syncState.map { userId to it }
            }) {
                it.toMap()
            }
        }.stateIn(coroutineScope, WhileSubscribed(), mapOf())

        syncStates = _syncState
            .debounce(3.seconds)
            .mapLatest {
                val ok = mutableSetOf<UserId>()
                val failed = mutableSetOf<UserId>()
                it.map { (userId, syncState) ->
                    when (syncState) {
                        SyncState.ERROR,
                        SyncState.TIMEOUT,
                            -> failed.add(userId)

                        else -> ok.add(userId)
                    }
                }
                UserSyncStates(
                    operationalFor = ok.toSet(),
                    failedFor = failed.toSet(),
                )
            }
            .stateIn(coroutineScope, WhileSubscribed(), UserSyncStates(setOf(), setOf()))

        var initialSyncFinishedOnce = false
        initialSyncFinished = _syncState
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

        // andle room navigation requests through the appUriScheme://matrix:roomid/<ID> scheme.
        // TODO Should be removed when better deeplink support is added
        coroutineScope.launch {
            this@RoomListViewModelImpl.get<UriHandler>().collect { uri ->
                val segments = Url(uri).rawSegments
                if (segments.size < 3 || segments[1] != "matrix:roomid") return@collect
                selectRoom(RoomId("!" + segments[2]))
            }
        }
        val backCallback = BackCallback(enabled = showSearch) {
            showSearch.value = false
        }
        registerBackCallback(backCallback)
    }

    private fun resetSearchWhenNotShown() {
        coroutineScope.launch {
            showSearch.drop(1).collect {
                if (it.not()) {
                    searchTerm.update("")
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
            val membership = matrixClient.room.getById(roomId).first()?.membership
            log.debug { "switch to room $roomId" }
            when (membership) {
                Membership.JOIN -> {
                    onRoomSelected(matrixClient.userId, roomId)
                }

                else -> {}
            }
        }
    }

    override val closeProfileNeeded: Boolean get() = profileManager?.isMultiProfileEnabled?.value == true

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

    override fun verifyAccount(userId: UserId) {
        onStartVerification(userId)
    }
}

class PreviewRoomListViewModel : RoomListViewModel {
    override val selectedRoomId: MutableStateFlow<RoomId?> = MutableStateFlow(null)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val errorType: MutableStateFlow<ErrorType> = MutableStateFlow(ErrorType.JUST_DISMISS)
    override val elements: MutableStateFlow<List<RoomListElementViewModel>> =
        MutableStateFlow(
            listOf(
                PreviewRoomListElementViewModel1(),
                PreviewRoomListElementViewModel2(),
                PreviewRoomListElementViewModel3(),
            )
        )

    override val syncStates: StateFlow<UserSyncStates> = MutableStateFlow(UserSyncStates(setOf(), setOf()))
    override val initialSyncFinished: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val showSearch: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val searchTerm = TextFieldViewModelImpl(maxLength = 1_000)
    override val searchResultsEmpty: StateFlow<Boolean> = MutableStateFlow(false)
    override val accountViewModel: AccountViewModel = PreviewAccountViewModel()
    override val canCreateNewRoomWithAccount: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val unverifiedAccounts: StateFlow<List<UserId>> = MutableStateFlow(listOf())
    override val closeProfileNeeded: Boolean = true

    override fun createNewRoom() {}
    override fun createNewRoomFor(userId: UserId) {}
    override fun selectRoom(roomId: RoomId) {}
    override fun errorDismiss() {}
    override fun sendLogs() {}
    override fun closeProfile() {}
    override fun verifyAccount(userId: UserId) {}
}
