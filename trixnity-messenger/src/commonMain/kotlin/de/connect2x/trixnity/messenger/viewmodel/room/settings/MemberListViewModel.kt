package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.client.flattenNotNull
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.membership
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.get

interface MemberListViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>,
        onOpenUserProfile: (UserId) -> Unit,
    ): MemberListViewModel =
        MemberListViewModelImpl(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            error = error,
            onOpenUserProfile = onOpenUserProfile,
        )

    companion object : MemberListViewModelFactory
}

interface MemberListViewModel {
    val filterByMemberships: MutableStateFlow<Set<Membership>>
    val elements: StateFlow<List<MemberListElementViewModel>>
    val membershipCounts: StateFlow<Map<Membership, Int>>
    val showLoadingSpinner: StateFlow<Boolean>
    val error: StateFlow<String?>
}

open class MemberListViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    override val error: MutableStateFlow<String?>,
    private val onOpenUserProfile: (UserId) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, MemberListViewModel {

    override val showLoadingSpinner =
        matrixClient.room
            .getById(selectedRoomId)
            .map { it?.membersLoaded != true }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    private class MemberListElementViewModelWrapper(
        val viewModel: MemberListElementViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val elementCache = mutableMapOf<UserId, MemberListElementViewModelWrapper>()

    private val allUsers =
        matrixClient.user
            .getAll(selectedRoomId)
            .flattenNotNull()
            .map { it.values }
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

    override val filterByMemberships: MutableStateFlow<Set<Membership>> =
        MutableStateFlow(setOf(Membership.JOIN, Membership.KNOCK, Membership.INVITE, Membership.BAN))

    override val elements: StateFlow<List<MemberListElementViewModel>> =
        combine(
                matrixClient.room.getState<PowerLevelsEventContent>(selectedRoomId).map { it?.content },
                matrixClient.room.getState<CreateEventContent>(selectedRoomId).filterNotNull(),
                allUsers,
                filterByMemberships,
            ) { powerLevels, createEvent, roomUsers, filterByMemberships ->
                val relevantRoomUsers =
                    roomUsers
                        .filter { roomUser -> filterByMemberships.contains(roomUser.membership) }
                        .sortedWith(
                            compareBy<RoomUser> { roomUser ->
                                    when (roomUser.membership) {
                                        Membership.JOIN -> 1
                                        Membership.KNOCK -> 2
                                        Membership.INVITE -> 3
                                        Membership.BAN -> 4
                                        Membership.LEAVE -> 5
                                    }
                                }
                                .thenByDescending { roomUser ->
                                    matrixClient.user.getPowerLevel(roomUser.userId, createEvent, powerLevels)
                                }
                        )
                        .associateBy { it.userId }

                elementCache
                    .mapNotNull { (userId, wrapper) ->
                        if (relevantRoomUsers[userId] == null) {
                            wrapper.lifecycle.destroy()
                            userId
                        } else null
                    }
                    .forEach { key -> elementCache.remove(key) }

                relevantRoomUsers.map { (userId, roomUser) ->
                    elementCache[userId]?.viewModel
                        ?: run {
                            val lifecycle = LifecycleRegistry()
                            lifecycle.start()
                            get<MemberListElementViewModelFactory>()
                                .create(
                                    viewModelContext = childContextWithOwnLifecycle(roomUser.userId.full, lifecycle),
                                    roomUser,
                                    selectedRoomId = selectedRoomId,
                                    onOpenUserProfile = onOpenUserProfile,
                                )
                                .also { elementCache[userId] = MemberListElementViewModelWrapper(it, lifecycle) }
                        }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    override val membershipCounts: StateFlow<Map<Membership, Int>> =
        allUsers
            .map { users ->
                Membership.entries.associateWith { membershipKind ->
                    users.count { roomUser -> roomUser.membership == membershipKind }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyMap())
}

class PreviewMemberListViewModel : MemberListViewModel {
    override val filterByMemberships: MutableStateFlow<Set<Membership>> = MutableStateFlow(emptySet())
    override val elements: MutableStateFlow<List<MemberListElementViewModel>> = MutableStateFlow(emptyList())
    override val membershipCounts: StateFlow<Map<Membership, Int>> = MutableStateFlow(emptyMap())
    override val showLoadingSpinner: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
}
