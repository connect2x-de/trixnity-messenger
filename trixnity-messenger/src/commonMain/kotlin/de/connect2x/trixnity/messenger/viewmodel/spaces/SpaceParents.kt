package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.GetPowerLevelImpl
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.ClientEvent.StateBaseEvent
import de.connect2x.trixnity.core.model.events.StateEventContent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.PowerLevelsEventContent
import de.connect2x.trixnity.core.model.events.m.room.get
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.core.model.events.m.space.ParentEventContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal data class SpaceParentCandidate(val childId: RoomId, val parentEvent: StateBaseEvent<ParentEventContent>)

internal data class SpaceParent(val roomId: RoomId, val children: Set<RoomId>)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun getSpaceParents(client: MatrixClient, onlyParent: RoomId? = null): Flow<List<SpaceParent>> {
    return client.room
        .getAll()
        .flatten()
        .map { rooms ->
            rooms.values.filterNotNull().filter { it.membership == Membership.JOIN }.map { it.roomId }.toSet()
        }
        .distinctUntilChanged()
        .flatMapLatest { getSpaceParents(client, it, onlyParent) }
}

/**
 * Validates parent claims from the supplied joined rooms without opening another room subscription. Only joined parents
 * are returned. [onlyParent] restricts claims to the space currently being viewed, or leaves all parents eligible when
 * null.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun getSpaceParents(
    client: MatrixClient,
    joinedRoomIds: Set<RoomId>,
    onlyParent: RoomId? = null,
): Flow<List<SpaceParent>> {
    if (joinedRoomIds.isEmpty()) return flowOf(emptyList())
    return combine(joinedRoomIds.map { client.getSpaceParentCandidates(it, onlyParent) }) {
            it.flatMap { candidates -> candidates }
        }
        .distinctUntilChanged()
        .flatMapLatest { parentCandidates ->
            val candidatesByParent = parentCandidates.groupBy { RoomId(it.parentEvent.stateKey) }
            if (candidatesByParent.isEmpty()) return@flatMapLatest flowOf(emptyList())
            combine(candidatesByParent.map { (parentId, candidates) -> getSpaceParent(client, parentId, candidates) }) {
                it.filterNotNull()
            }
        }
}

private fun MatrixClient.getSpaceParentCandidates(
    childId: RoomId,
    onlyParent: RoomId?,
): Flow<List<SpaceParentCandidate>> {
    return room.getAllState(childId, ParentEventContent::class).flatten().map { events ->
        events.values
            .filterNotNull()
            .filter {
                it.content.via.isNotEmpty() &&
                    RoomId.isValid(it.stateKey) &&
                    it.stateKey != childId.full &&
                    (onlyParent == null || it.stateKey == onlyParent.full)
            }
            .map { SpaceParentCandidate(childId, it) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun getSpaceParent(
    client: MatrixClient,
    parent: RoomId,
    candidates: List<SpaceParentCandidate>,
): Flow<SpaceParent?> {
    return client.room
        .getById(parent)
        .map { it?.membership == Membership.JOIN }
        .distinctUntilChanged()
        .flatMapLatest { isJoined ->
            if (!isJoined) return@flatMapLatest flowOf(null)
            combine(
                listOf(
                    client.room.getState<CreateEventContent>(parent),
                    client.room.getState<PowerLevelsEventContent>(parent),
                ) +
                    candidates
                        .map { it.childId }
                        .distinct()
                        .map { child -> client.room.getState<ChildEventContent>(parent, child.full) } +
                    candidates
                        .map { it.parentEvent.sender }
                        .distinct()
                        .map { sender -> client.room.getState<MemberEventContent>(parent, sender.full) }
            ) { events ->
                val state = events.filterNotNull()
                val missingMembership = candidates.any { (child, event) ->
                    state.event<ChildEventContent>(child.full)?.content?.via?.isNotEmpty() != true &&
                        state.event<MemberEventContent>(event.sender.full) == null
                }
                if (missingMembership) client.user.loadMembers(parent, wait = false)
                val valid = filterValidSpaceParents(candidates, state)
                if (valid.isEmpty()) null else SpaceParent(parent, valid.map { it.childId }.toSet())
            }
        }
}

internal fun filterValidSpaceParents(
    candidates: List<SpaceParentCandidate>,
    state: List<StateBaseEvent<*>>,
): List<SpaceParentCandidate> {
    val events = state.asReversed().associateBy { it.content::class to it.stateKey }
    val create = events[CreateEventContent::class to ""]
    val power = events[PowerLevelsEventContent::class to ""]
    return candidates.filter { (child, event) ->
        isValidSpaceParent(
            child,
            event,
            listOfNotNull(
                create,
                power,
                events[ChildEventContent::class to child.full],
                events[MemberEventContent::class to event.sender.full],
            ),
        )
    }
}

/**
 * Checks a claim against the claimed parent's [state]. The parent must be a space with either a reciprocal child link
 * or a joined claim sender allowed to send child events there.
 *
 * See [parent relationship validation](https://spec.matrix.org/v1.19/client-server-api/#mspaceparent-relationships).
 */
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
    if (state.event<MemberEventContent>(parent.sender.full)?.content?.membership != Membership.JOIN) return false
    val power = state.event<PowerLevelsEventContent>()?.content
    val required =
        power?.events?.get(ChildEventContent::class) ?: power?.stateDefault ?: PowerLevelsEventContent.STATE_DEFAULT
    return GetPowerLevelImpl()(parent.sender, create, power) >= required
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified C : StateEventContent> List<StateBaseEvent<*>>.event(
    stateKey: String = ""
): StateBaseEvent<C>? {
    return firstOrNull { it.stateKey == stateKey && it.content is C } as? StateBaseEvent<C>
}
