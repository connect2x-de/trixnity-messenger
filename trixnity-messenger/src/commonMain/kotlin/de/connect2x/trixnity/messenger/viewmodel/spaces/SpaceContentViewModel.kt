package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.search.user.homeserver.getImage
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.get

data class SpaceBrowserItem(
    val userId: UserId,
    val roomId: RoomId,
    val name: String,
    val isSpace: Boolean,
    val joined: Boolean,
    val canJoin: Boolean = false,
    val via: Set<String> = emptySet(),
    val membershipActionState: SpaceMembershipActionState = SpaceMembershipActionState.Idle,
    internal val avatarUrl: String? = null,
    val invited: Boolean = false,
    val memberCount: Long? = null,
    val childCount: Int? = null,
    val subspaceCount: Int = 0,
    val unknownChildCount: Int = 0,
    val accountColor: Long? = null,
)

enum class SpaceMembershipActionState {
    Idle,
    Joining,
    Rejecting,
    Failed;

    val isBusy: Boolean
        get() =
            when (this) {
                Idle,
                Failed -> false
                Joining,
                Rejecting -> true
            }
}

data class SpaceContent(
    val name: String? = null,
    val items: List<SpaceBrowserItem> = emptyList(),
    val loadingHierarchy: Boolean = false,
    val hierarchyFailed: Boolean = false,
    val userId: UserId? = null,
    internal val avatarUrl: String? = null,
    val invited: Boolean = false,
    val topic: String? = null,
    val memberCount: Long? = null,
)

sealed interface SpaceContentState {
    data object Loading : SpaceContentState

    data object Error : SpaceContentState

    data class Ready(val content: SpaceContent) : SpaceContentState
}

interface SpaceContentViewModel {
    val spaceImage: Flow<ByteArray?>

    fun avatar(userId: UserId, roomId: RoomId): Flow<ByteArray?>

    val state: StateFlow<SpaceContentState>

    fun select(item: SpaceBrowserItem)

    fun join(item: SpaceBrowserItem)

    fun reject(item: SpaceBrowserItem)

    fun refresh()

    fun back()
}

interface SpaceContentViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        userId: UserId?,
        spaceId: RoomId?,
        onSelect: (SpaceBrowserItem) -> Unit,
        onBack: () -> Unit,
        loadHierarchy: Boolean = true,
    ): SpaceContentViewModel =
        SpaceContentViewModelImpl(
            viewModelContext = viewModelContext,
            userId = userId,
            spaceId = spaceId,
            onSelect = onSelect,
            onBack = onBack,
            loadHierarchy = loadHierarchy,
        )

    companion object : SpaceContentViewModelFactory
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class SpaceContentViewModelImpl(
    viewModelContext: ViewModelContext,
    private val userId: UserId?,
    private val spaceId: RoomId?,
    private val onSelect: (SpaceBrowserItem) -> Unit,
    private val onBack: () -> Unit,
    private val loadHierarchy: Boolean = true,
) : ViewModelContext by viewModelContext, SpaceContentViewModel {
    private val membershipActions = MutableStateFlow<Map<Pair<UserId, RoomId>, SpaceMembershipActionState>>(emptyMap())
    private val refresh = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val account =
        if (spaceId == null && userId == null) {
            get<MatrixMessengerSettingsHolder>().map { it.base.selectedAccount }.distinctUntilChanged()
        } else {
            flowOf(requireNotNull(userId))
        }

    private val contentState =
        combine(account, refresh) { account, _ -> account }
            .flatMapLatest { account ->
                val content =
                    when {
                        spaceId == null -> getSpaceOverview(account)
                        loadHierarchy -> getSpaceExploreContent(requireNotNull(account), spaceId)
                        else -> getJoinedSpaceContent(requireNotNull(account), spaceId)
                    }
                content
                    .map<SpaceContent, SpaceContentState> { SpaceContentState.Ready(it) }
                    .onStart { emit(SpaceContentState.Loading) }
                    .catch {
                        log.warn { "Could not load spaces: $it" }
                        emit(SpaceContentState.Error)
                    }
            }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
                SpaceContentState.Loading,
            )

    override val spaceImage: Flow<ByteArray?> =
        if (userId == null) flowOf(null)
        else contentState.map { (it as? SpaceContentState.Ready)?.content?.avatarUrl }.loadAvatar(userId)

    override fun avatar(userId: UserId, roomId: RoomId): Flow<ByteArray?> =
        contentState
            .map { state ->
                (state as? SpaceContentState.Ready)
                    ?.content
                    ?.items
                    ?.find { it.userId == userId && it.roomId == roomId }
                    ?.avatarUrl
            }
            .loadAvatar(userId)

    private fun Flow<String?>.loadAvatar(userId: UserId): Flow<ByteArray?> =
        distinctUntilChanged().flatMapLatest { url ->
            if (url == null) return@flatMapLatest flowOf(null)
            flow {
                    coroutineScope {
                        emitAll(
                            getImage(
                                url,
                                getMatrixClient(userId),
                                get<MatrixMessengerConfiguration>().downloadLimits.thumbnail,
                                this,
                            )
                        )
                    }
                }
                .catch {
                    log.warn { "Could not load space avatar: $it" }
                    emit(null)
                }
        }

    override val state: StateFlow<SpaceContentState> =
        combine(contentState, membershipActions, get<MatrixMessengerSettingsHolder>()) {
                state,
                membershipActions,
                settings ->
                when (state) {
                    SpaceContentState.Loading,
                    SpaceContentState.Error -> state
                    is SpaceContentState.Ready -> {
                        val accounts = settings.base.accounts
                        val items =
                            state.content.items.map { item ->
                                val actionState =
                                    membershipActions[item.userId to item.roomId] ?: SpaceMembershipActionState.Idle
                                item.copy(
                                    accountColor =
                                        if (accounts.size > 1) accounts[item.userId]?.base?.displayColor else null,
                                    membershipActionState =
                                        if (item.joined) SpaceMembershipActionState.Idle else actionState,
                                )
                            }
                        SpaceContentState.Ready(state.content.copy(items = items))
                    }
                }
            }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
                SpaceContentState.Loading,
            )

    override fun join(item: SpaceBrowserItem) {
        val current = findCurrentItem(item) ?: return
        if (!current.canJoin || current.joined) return
        runMembershipAction(current, SpaceMembershipActionState.Joining) {
            joinSpaceRoom(current)
            if (current.roomId == spaceId) onSelect(current.copy(joined = true, invited = false, canJoin = false))
            else waitForJoinedItemInContent(current)
        }
    }

    private suspend fun waitForJoinedItemInContent(item: SpaceBrowserItem) {
        contentState.first { state ->
            (state as? SpaceContentState.Ready)?.content?.items?.any {
                it.userId == item.userId && it.roomId == item.roomId && it.joined
            } == true
        }
    }

    override fun reject(item: SpaceBrowserItem) {
        val current = findCurrentItem(item) ?: return
        if (!current.invited || current.joined) return
        runMembershipAction(current, SpaceMembershipActionState.Rejecting) {
            rejectSpaceInvite(current)
            if (current.roomId == spaceId) onBack()
        }
    }

    private fun runMembershipAction(
        item: SpaceBrowserItem,
        pending: SpaceMembershipActionState,
        action: suspend () -> Unit,
    ) {
        val key = item.userId to item.roomId
        val previous = membershipActions.getAndUpdate { current ->
            if (current[key]?.isBusy == true) current else current + (key to pending)
        }
        if (previous[key]?.isBusy == true) return
        coroutineScope.launch {
            try {
                check(
                    withTimeoutOrNull(30.seconds) {
                        action()
                        true
                    } == true
                ) {
                    "Timed out completing membership action: $pending"
                }
                membershipActions.update { it - key }
            } catch (error: CancellationException) {
                membershipActions.update { it - key }
                throw error
            } catch (error: Exception) {
                log.warn { "Could not complete membership action $pending: $error" }
                membershipActions.update { it + (key to SpaceMembershipActionState.Failed) }
            }
        }
    }

    private fun findCurrentItem(item: SpaceBrowserItem): SpaceBrowserItem? =
        (state.value as? SpaceContentState.Ready)?.content?.items?.find {
            it.userId == item.userId && it.roomId == item.roomId
        }

    override fun select(item: SpaceBrowserItem) {
        val current = findCurrentItem(item) ?: return
        if (current.joined || current.invited) onSelect(current)
    }

    override fun refresh() {
        coroutineScope.launch { refresh.emit(Unit) }
    }

    override fun back() = onBack()
}
