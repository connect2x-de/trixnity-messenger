package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.start
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
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

    override val showLoadingSpinner = matrixClient.room.getById(selectedRoomId).map {
        it?.membersLoaded != true
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)

    private class MemberListElementViewModelWrapper(
        val viewModel: MemberListElementViewModel,
        val lifecycle: LifecycleRegistry,
    )

    private val elementCache = mutableMapOf<UserId, MemberListElementViewModelWrapper>()

    private val allUsers = matrixClient.user.getAll(selectedRoomId).flattenNotNull().map { it.values }
        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

    override val elements: StateFlow<List<MemberListElementViewModel>> =
        combine(
            matrixClient.room.getState<PowerLevelsEventContent>(selectedRoomId).map { it?.content },
            matrixClient.room.getState<CreateEventContent>(selectedRoomId).filterNotNull(),
            allUsers
        ) { powerLevels, createEvent, roomUsers ->
            val relevantRoomUsers = roomUsers.filter { roomUser ->
                roomUser.membership == Membership.JOIN ||
                        roomUser.membership == Membership.INVITE ||
                        roomUser.membership == Membership.BAN
            }.sortedByDescending { roomUser ->
                matrixClient.user.getPowerLevel(
                    roomUser.userId,
                    createEvent.sender,
                    powerLevels
                )
            }.associateBy { it.userId }

            elementCache.mapNotNull { (userId, wrapper) ->
                if (relevantRoomUsers[userId] == null) {
                    wrapper.lifecycle.destroy()
                    userId
                } else null
            }.forEach { key -> elementCache.remove(key) }

            relevantRoomUsers.map { (userId, roomUser) ->
                elementCache[userId]?.viewModel
                    ?: run {
                        val lifecycle = LifecycleRegistry()
                        lifecycle.start()
                        get<MemberListElementViewModelFactory>()
                            .create(
                                viewModelContext = childContextWithOwnLifecycle(lifecycle),
                                roomUser,
                                selectedRoomId = selectedRoomId,
                                onOpenUserProfile = onOpenUserProfile
                            ).also {
                                elementCache[userId] = MemberListElementViewModelWrapper(it, lifecycle)
                            }
                    }
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
    override val elements: MutableStateFlow<List<MemberListElementViewModel>> =
        MutableStateFlow(emptyList())
    override val membershipCounts: StateFlow<Map<Membership, Int>> = MutableStateFlow(emptyMap())
    override val showLoadingSpinner: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
}
