package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface SpaceRoomListViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        space: RoomListSpace,
        selectedRoomId: StateFlow<RoomId?>,
        exploreSelected: StateFlow<Boolean>,
        onOpenExplore: () -> Unit,
        onSelect: (SpaceBrowserItem) -> Unit,
        onBack: () -> Unit,
        onRoomSelected: (UserId, RoomId) -> Unit,
        onStartCreateNewRoom: (UserId) -> Unit,
        onUserSettingsSelected: () -> Unit,
        onShowAccounts: () -> Unit,
        onOpenAppInfo: () -> Unit,
        onSendLogs: () -> Unit,
        onAccountSelected: (UserId) -> Unit,
        onSpaceUnavailable: () -> Unit,
        onStartVerification: (UserId) -> Unit,
        onCloseRoom: () -> Unit,
    ): SpaceRoomListViewModel {
        return SpaceRoomListViewModelImpl(
            viewModelContext = viewModelContext,
            space = space,
            exploreSelected = exploreSelected,
            onOpenExplore = onOpenExplore,
            onBack = onBack,
            content =
                viewModelContext
                    .get<SpaceContentViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext("Content"),
                        userId = space.userId,
                        spaceId = space.roomId,
                        onSelect = onSelect,
                        onBack = onBack,
                        loadHierarchy = false,
                    ),
            rooms =
                RoomListViewModelImpl(
                    viewModelContext = viewModelContext.childContext("Rooms"),
                    selectedRoomId = selectedRoomId,
                    onRoomSelected = onRoomSelected,
                    onCreateNewRoom = onStartCreateNewRoom,
                    onUserSettingsSelected = onUserSettingsSelected,
                    onShowAccounts = onShowAccounts,
                    onOpenAppInfo = onOpenAppInfo,
                    onSendLogs = onSendLogs,
                    onAccountSelected =
                        viewModelContext.spaceAccountSelection(space.roomId, onAccountSelected, onSpaceUnavailable),
                    onStartVerification = onStartVerification,
                    onCloseRoom = onCloseRoom,
                    roomListSource = SpaceRoomListSource(space),
                ),
        )
    }

    companion object : SpaceRoomListViewModelFactory
}

interface SpaceRoomListViewModel {
    val content: SpaceContentViewModel
    val rooms: RoomListViewModel
    val exploreSelected: StateFlow<Boolean>

    fun openExplore()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class SpaceRoomListViewModelImpl(
    viewModelContext: ViewModelContext,
    space: RoomListSpace,
    override val exploreSelected: StateFlow<Boolean>,
    private val onOpenExplore: () -> Unit,
    onBack: () -> Unit,
    override val content: SpaceContentViewModel,
    override val rooms: RoomListViewModel,
) : SpaceRoomListViewModel {
    override fun openExplore() = onOpenExplore()

    init {
        with(viewModelContext) {
            coroutineScope.launch {
                matrixClients
                    .map { it[space.userId] }
                    .distinctUntilChanged()
                    .flatMapLatest { client -> client?.room?.getById(space.roomId) ?: flowOf(null) }
                    .first { it?.membership != Membership.JOIN }
                onBack()
            }
        }
    }
}

private fun ViewModelContext.spaceAccountSelection(
    spaceId: RoomId,
    onSelected: (UserId) -> Unit,
    onUnavailable: () -> Unit,
): () -> Unit {
    val settings = get<MatrixMessengerSettingsHolder>()
    return {
        val userId = settings.value.base.selectedAccount
        coroutineScope.launch {
            val membership = userId?.let { matrixClients.value[it]?.room?.getById(spaceId)?.first()?.membership }
            if (settings.value.base.selectedAccount == userId) {
                if (userId != null && membership == Membership.JOIN) onSelected(userId) else onUnavailable()
            }
        }
    }
}
