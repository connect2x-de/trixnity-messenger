package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.flattenValues
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.type
import de.connect2x.trixnity.clientserverapi.model.room.GetHierarchy.Response.SpaceHierarchyRoomsChunk
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.StateBaseEvent
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal fun <T> getJoinedSpaceChildren(
    client: MatrixClient,
    spaceId: RoomId,
    parents: Flow<List<SpaceParent>>,
    transform: (Room?, Set<Room>) -> T,
): Flow<T> =
    combine(
        client.room.getAll().flattenValues(),
        client.room.getAllState(spaceId, ChildEventContent::class).flatten(),
        parents,
    ) { rooms, children, parentLinks ->
        val space = rooms.find { it.roomId == spaceId }
        val joinedRooms =
            if (space?.membership == Membership.JOIN) filterJoinedSpaceChildren(spaceId, rooms, children, parentLinks)
            else emptySet()
        transform(space, joinedRooms)
    }

private fun spaceChildIds(
    spaceId: RoomId,
    children: Map<String, StateBaseEvent<ChildEventContent>?>,
    parents: List<SpaceParent>,
): Set<RoomId> =
    (children.filterValues { it != null && it.content.via.isNotEmpty() }.keys.map(::RoomId) +
            parents.flatMap { it.children })
        .filter { it != spaceId }
        .toSet()

internal fun filterJoinedSpaceChildren(
    spaceId: RoomId,
    rooms: Collection<Room>,
    children: Map<String, StateBaseEvent<ChildEventContent>?>,
    parents: List<SpaceParent>,
): Set<Room> {
    val ids = spaceChildIds(spaceId, children, parents)
    return rooms.filterTo(mutableSetOf()) { it.membership == Membership.JOIN && it.roomId in ids }
}

internal fun buildSpaceHierarchyItems(
    userId: UserId,
    spaceId: RoomId,
    rooms: Map<RoomId, Room?>,
    childEvents: Map<String, StateBaseEvent<ChildEventContent>?>,
    hierarchy: List<SpaceHierarchyRoomsChunk>,
    parents: List<SpaceParent>,
): List<SpaceBrowserItem> {
    val discovered =
        hierarchy
            .filter {
                it.roomId != spaceId &&
                    (it.roomId.full !in childEvents || childEvents[it.roomId.full]?.content?.via?.isNotEmpty() == true)
            }
            .associateBy { it.roomId }
    val ids = spaceChildIds(spaceId, childEvents, parents) + discovered.keys
    val hierarchyChildren =
        hierarchy.find { it.roomId == spaceId }?.childrenState?.associateBy { it.stateKey }.orEmpty()
    val knownSpaces =
        discovered.mapValues { it.value.roomType == CreateEventContent.RoomType.Space } +
            rooms
                .filterValues { it?.createEventContent != null }
                .mapValues { it.value?.type == CreateEventContent.RoomType.Space }
    return ids.mapNotNull { id ->
            spaceHierarchyItem(
                userId,
                id,
                rooms[id],
                discovered[id],
                rooms,
                knownSpaces,
                childEvents[id.full]?.content?.via
                    ?: (hierarchyChildren[id.full]?.content as? ChildEventContent)?.via.orEmpty(),
            )
        }
        .sortedWith(
            spaceChildComparator(
                hierarchyChildren + childEvents.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
            )
        )
}

private fun spaceHierarchyItem(
    userId: UserId,
    id: RoomId,
    local: Room?,
    remote: SpaceHierarchyRoomsChunk?,
    rooms: Map<RoomId, Room?>,
    knownSpaces: Map<RoomId, Boolean>,
    via: Set<String>,
): SpaceBrowserItem? {
    val roomType = local?.type ?: remote?.roomType
    val item =
        local?.toBrowserItem(userId)
            ?: remote?.let {
                SpaceBrowserItem(
                    userId,
                    id,
                    it.name ?: id.full,
                    it.roomType == CreateEventContent.RoomType.Space,
                    joined = false,
                    avatarUrl = it.avatarUrl,
                )
            }
            ?: return null
    val result =
        item.copy(
            name = if (local?.membership != Membership.JOIN) remote?.name ?: item.name else item.name,
            isSpace = roomType == CreateEventContent.RoomType.Space,
            avatarUrl =
                if (local?.membership == Membership.JOIN || remote == null) item.avatarUrl else remote.avatarUrl,
            canJoin =
                canJoinSpaceChild(
                    local?.membership,
                    remote?.joinRule,
                    roomType,
                    remote?.allowedRoomIds.orEmpty().any { rooms[it]?.membership == Membership.JOIN },
                ),
            via = via,
            memberCount =
                if (local?.membership == Membership.JOIN) item.memberCount ?: remote?.joinedMembersCount
                else remote?.joinedMembersCount ?: item.memberCount,
        )
    if (!result.isSpace || remote == null) return result
    val children =
        remote.childrenState
            .filter { (it.content as? ChildEventContent)?.via?.isNotEmpty() == true }
            .map { RoomId(it.stateKey) }
            .toSet()
    return result.withChildStatistics(children, knownSpaces)
}
