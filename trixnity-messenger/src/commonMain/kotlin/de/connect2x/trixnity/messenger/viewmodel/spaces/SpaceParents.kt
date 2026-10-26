package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.user.GetPowerLevelImpl
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.StateBaseEvent
import de.connect2x.trixnity.core.model.events.StateEventContent
import de.connect2x.trixnity.core.model.events.m.room.AvatarEventContent
import de.connect2x.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.NameEventContent
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.model.events.m.room.get
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull

internal data class SpaceParent(
    val roomId: RoomId,
    val children: Set<RoomId>,
    val via: Set<String>,
    val state: List<StateBaseEvent<*>>,
) {
    fun toBrowserItem(userId: UserId, rooms: Map<RoomId, Room?>): SpaceBrowserItem {
        val membership = rooms[roomId]?.membership
        val rules = state.event<JoinRulesEventContent>()?.content
        val rule = rules?.joinRule
        val allowed =
            rules?.allow.orEmpty().any {
                it.type == JoinRulesEventContent.AllowCondition.AllowConditionType.RoomMembership &&
                    rooms[it.roomId]?.membership == Membership.JOIN
            }
        return SpaceBrowserItem(
            userId,
            roomId,
            state.event<NameEventContent>()?.content?.name?.takeIf { it.isNotBlank() }
                ?: state.event<CanonicalAliasEventContent>()?.content?.alias?.full
                ?: roomId.full,
            isSpace = true,
            joined = membership == Membership.JOIN,
            canJoin = canJoinSpaceChild(membership, rule, CreateEventContent.RoomType.Space, allowed),
            via = via,
            avatarUrl = state.event<AvatarEventContent>()?.content?.url,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun getSpaceParents(client: MatrixClient, onlyParent: RoomId? = null): Flow<List<SpaceParent>> =
    client.room
        .getAll()
        .flatten()
        .map { rooms ->
            rooms.values.filterNotNull().filter { it.membership == Membership.JOIN }.map { it.roomId }.toSet()
        }
        .distinctUntilChanged()
        .flatMapLatest { rooms ->
            if (rooms.isEmpty()) flowOf(emptyList())
            else
                combine(
                    rooms.map { child ->
                        client.room.getAllState(child, ParentEventContent::class).flatten().map { events ->
                            events.values
                                .filterNotNull()
                                .filter {
                                    !it.content.via.isNullOrEmpty() &&
                                        RoomId.isValid(it.stateKey) &&
                                        it.stateKey != child.full &&
                                        (onlyParent == null || it.stateKey == onlyParent.full)
                                }
                                .map { child to it }
                        }
                    }
                ) {
                    it.flatMap { links -> links }
                }
        }
        .distinctUntilChanged()
        .flatMapLatest { links ->
            val parents = links.groupBy { RoomId(it.second.stateKey) }
            if (parents.isEmpty()) flowOf(emptyList())
            else
                combine(
                    parents.map { (parent, candidates) ->
                        getParentState(client, parent).map { state ->
                            val valid = candidates.filter { (child, event) -> isValidSpaceParent(child, event, state) }
                            if (valid.isEmpty()) null
                            else
                                SpaceParent(
                                    parent,
                                    valid.map { it.first }.toSet(),
                                    valid.flatMap { it.second.content.via }.toSet(),
                                    state,
                                )
                        }
                    }
                ) {
                    it.filterNotNull()
                }
        }

@OptIn(ExperimentalCoroutinesApi::class)
private fun getParentState(client: MatrixClient, parent: RoomId): Flow<List<StateBaseEvent<*>>> =
    client.room
        .getById(parent)
        .map { it?.membership == Membership.JOIN }
        .distinctUntilChanged()
        .flatMapLatest { joined ->
            if (joined)
                combine(
                    client.room.getState<CreateEventContent>(parent),
                    client.room.getState<PowerLevelsEventContent>(parent),
                    client.room.getAllState(parent, ChildEventContent::class).flatten(),
                    client.room.getAllState(parent, MemberEventContent::class).flatten(),
                ) { create, power, children, members ->
                    listOfNotNull(create, power) + children.values.filterNotNull() + members.values.filterNotNull()
                }
            else
                flow<List<StateBaseEvent<*>>> {
                        emit(withTimeoutOrNull(15.seconds) { client.api.room.getState(parent).getOrThrow() }.orEmpty())
                    }
                    .onStart { emit(emptyList()) }
                    .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        }

internal fun isValidSpaceParent(
    child: RoomId,
    parent: StateBaseEvent<ParentEventContent>,
    state: List<StateBaseEvent<*>>,
): Boolean {
    if (parent.content.via.isEmpty() || child.full == parent.stateKey) return false
    val create = state.event<CreateEventContent>() ?: return false
    if (create.content.type != CreateEventContent.RoomType.Space) return false
    // Removing via unlinks a child without removing its state event.
    if (state.event<ChildEventContent>(child.full)?.content?.via?.isNotEmpty() == true) return true
    // A parent claim is authorized in the child room. Without a reciprocal link, its sender must be able
    // to send m.space.child in the parent: https://spec.matrix.org/v1.19/client-server-api/#mspaceparent-relationships
    if (state.event<MemberEventContent>(parent.sender.full)?.content?.membership != Membership.JOIN) return false
    val power = state.event<PowerLevelsEventContent>()?.content
    val required =
        power?.events?.get(ChildEventContent::class) ?: power?.stateDefault ?: PowerLevelsEventContent.STATE_DEFAULT
    return GetPowerLevelImpl()(parent.sender, create, power) >= required
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified C : StateEventContent> List<StateBaseEvent<*>>.event(
    stateKey: String = ""
): StateBaseEvent<C>? = firstOrNull { it.stateKey == stateKey && it.content is C } as? StateBaseEvent<C>
