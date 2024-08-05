package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.benasher44.uuid.uuid4
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface BannedMemberListViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>
    ): BannedMemberListViewModel =
        BannedMemberListViewModelImpl(
            viewModelContext,
            selectedRoomId,
            error
        )

    companion object : BannedMemberListViewModelFactory
}

interface BannedMemberListViewModel {
    val bannedMemberListElementViewModels: StateFlow<List<Pair<UserId, BannedMemberListElementViewModel>>>
    val showLoadingSpinner: StateFlow<Boolean>
    val error: StateFlow<String?>
}

class BannedMemberListViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    override val error: MutableStateFlow<String?>
) : MatrixClientViewModelContext by viewModelContext, BannedMemberListViewModel {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val bannedMemberListElementViewModels: StateFlow<List<Pair<UserId, BannedMemberListElementViewModel>>> =
        matrixClient.user.getAll(selectedRoomId).flattenNotNull().map { it.values }
            .mapLatest { roomUsers ->
                roomUsers.mapNotNull { roomUser ->
                    if (roomUser.membership == Membership.BAN) {
                        val userId = roomUser.userId
                        val bannedMemberListElementViewModel = get<BannedMemberListElementViewModelFactory>()
                            .create(
                                viewModelContext = childContext("bannedMemberListElement-${uuid4()}"),
                                selectedRoomId,
                                roomUser,
                                error
                            )
                        Pair(userId, bannedMemberListElementViewModel)
                    } else null
                }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), listOf())
    override val showLoadingSpinner: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        coroutineScope.launch {
            combine(
                matrixClient.room.getById(selectedRoomId),
                bannedMemberListElementViewModels
            ) { room, bannedMemberListElementViewModels ->
                val membersLoaded = room?.membersLoaded ?: false
                showLoadingSpinner.value = !membersLoaded || bannedMemberListElementViewModels.isEmpty()
                log.debug { "allBannedMembersLoaded = $membersLoaded" }
            }.collect()
        }
    }
}

class PreviewBannedMemberListViewModel : BannedMemberListViewModel {
    override val bannedMemberListElementViewModels: MutableStateFlow<List<Pair<UserId, BannedMemberListElementViewModel>>> =
        MutableStateFlow(listOf())
    override val showLoadingSpinner: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

}
