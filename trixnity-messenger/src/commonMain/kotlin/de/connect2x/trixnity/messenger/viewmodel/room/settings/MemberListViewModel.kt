package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.flattenNotNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface MemberListViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>
    ): MemberListViewModel {
        return MemberListViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            error = error
        )
    }

    companion object : MemberListViewModelFactory
}

interface MemberListViewModel {
    val memberListElementViewModels: StateFlow<List<Pair<UserId, MemberListElementViewModel>>>
    val membershipCounts: Map<Membership, StateFlow<Int?>>
    val showLoadingSpinner: StateFlow<Boolean>
    val error: StateFlow<String?>
}

open class MemberListViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    override val error: MutableStateFlow<String?>,
) : MatrixClientViewModelContext by viewModelContext, MemberListViewModel {

    override val showLoadingSpinner = MutableStateFlow(true)

    private val viewModels = mutableMapOf<UserId, MemberListElementViewModel>()

    override val memberListElementViewModels: StateFlow<List<Pair<UserId, MemberListElementViewModel>>> =
        combine(
            matrixClient.room.getState<PowerLevelsEventContent>(selectedRoomId).map { it?.content },
            matrixClient.room.getState<CreateEventContent>(selectedRoomId).filterNotNull(),
            matrixClient.user.getAll(selectedRoomId).flattenNotNull().map { it.values }
        ) { powerLevels, createEvent, roomUsers ->
            roomUsers.mapNotNull { roomUser ->
                if (roomUser.membership == Membership.JOIN ||
                    roomUser.membership == Membership.INVITE ||
                    roomUser.membership == Membership.BAN
                ) {
                    val userId = roomUser.userId
                    val memberListElementViewModel = viewModels.getOrPut(userId) {
                        get<MemberListElementViewModelFactory>()
                            .create(
                                viewModelContext = childContext("memberListElement-${roomUser.userId.full}"),
                                roomUser,
                                error = error,
                                selectedRoomId = selectedRoomId,
                            )
                    }
                    Pair(userId, memberListElementViewModel)
                } else null
            }.sortedByDescending { (userId, _) ->
                matrixClient.user.getPowerLevel(
                    userId,
                    createEvent.sender,
                    powerLevels
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override val membershipCounts: Map<Membership, StateFlow<Int?>> =
        Membership.entries.associateWith { membershipKind ->
            matrixClient.user.getAll(selectedRoomId).mapNotNull { users ->
                users
                    .count { (_, roomUser) ->
                        roomUser.firstOrNull { it != null }?.membership == membershipKind
                    }
            }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        }

    init {
        coroutineScope.launch {
            val roomFlow = matrixClient.room.getById(selectedRoomId)
            combine(roomFlow, memberListElementViewModels) { room, memberListElementViewModels ->
                val membersLoaded = room?.membersLoaded ?: false
                showLoadingSpinner.value = !membersLoaded || memberListElementViewModels.isEmpty()
                log.debug { "allMembersLoaded = $membersLoaded" }
            }.collect()
        }
    }
}

class PreviewMemberListViewModel : MemberListViewModel {
    override val memberListElementViewModels: MutableStateFlow<List<Pair<UserId, MemberListElementViewModel>>> =
        MutableStateFlow(emptyList())
    override val membershipCounts: Map<Membership, StateFlow<Int?>> = emptyMap()
    override val showLoadingSpinner: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

}
