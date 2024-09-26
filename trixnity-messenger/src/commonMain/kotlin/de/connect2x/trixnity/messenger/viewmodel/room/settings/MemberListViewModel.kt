package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.*
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
    val membershipCounts: StateFlow<Map<Membership, Int>>
    val showLoadingSpinner: StateFlow<Boolean>
    val error: StateFlow<String?>
}

open class MemberListViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    override val error: MutableStateFlow<String?>,
) : MatrixClientViewModelContext by viewModelContext, MemberListViewModel {

    override val showLoadingSpinner = matrixClient.room.getById(selectedRoomId).map {
        it?.membersLoaded != true
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    private val viewModels = mutableMapOf<UserId, MemberListElementViewModel>()

    private val allUsers = matrixClient.user.getAll(selectedRoomId).flattenNotNull().map { it.values }
        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

    override val memberListElementViewModels: StateFlow<List<Pair<UserId, MemberListElementViewModel>>> =
        combine(
            matrixClient.room.getState<PowerLevelsEventContent>(selectedRoomId).map { it?.content },
            matrixClient.room.getState<CreateEventContent>(selectedRoomId).filterNotNull(),
            allUsers
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

    override val membershipCounts: StateFlow<Map<Membership, Int>> =
        allUsers.map { users ->
            Membership.entries.associateWith { membershipKind ->
                users.count { roomUser ->
                    roomUser.membership == membershipKind
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyMap())
}


class PreviewMemberListViewModel : MemberListViewModel {
    override val memberListElementViewModels: MutableStateFlow<List<Pair<UserId, MemberListElementViewModel>>> =
        MutableStateFlow(emptyList())
    override val membershipCounts: StateFlow<Map<Membership, Int>> = MutableStateFlow(emptyMap())
    override val showLoadingSpinner: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
}
