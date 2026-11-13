package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.flattenNotNull
import de.connect2x.trixnity.client.notification
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.type
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.messenger.util.getNotificationDisplayCount
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
internal fun ViewModelContext.getSpaceOverview(
    selectedAccount: UserId?,
    loadDetails: Boolean = true,
): Flow<SpaceContent> {
    return matrixClients
        .map { allClients -> allClients.filterKeys { selectedAccount == null || it == selectedAccount } }
        .distinctUntilChanged()
        .flatMapLatest { selectedClients ->
            if (selectedClients.isEmpty()) flowOf(SpaceContent())
            else
                combine(
                    selectedClients.map { (userId, matrixClient) ->
                        getAccountSpaceOverview(userId, matrixClient, loadDetails)
                    }
                ) { spacesByAccount ->
                    SpaceContent(
                        items = spacesByAccount.flatMap { it }.sortedByName().sortedByDescending { it.invited }
                    )
                }
        }
}

/**
 * Derives hierarchy links and optional child counts from one local room snapshot. Without details, parent claims are
 * read only from joined spaces, since ordinary rooms cannot affect space roots.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun ViewModelContext.getAccountSpaceOverview(
    userId: UserId,
    matrixClient: MatrixClient,
    loadDetails: Boolean,
): Flow<List<SpaceBrowserItem>> {
    return matrixClient.room.getAll().flatten().flatMapLatest { roomsById ->
        val localRooms = roomsById.values.filterNotNull()
        val localSpaces =
            localRooms
                .filter {
                    it.type == CreateEventContent.RoomType.Space &&
                        (it.membership == Membership.JOIN || it.membership == Membership.INVITE)
                }
                .map { it.toBrowserItem(userId) }
        val joinedRoomIds =
            localRooms
                .filter {
                    it.membership == Membership.JOIN && (loadDetails || it.type == CreateEventContent.RoomType.Space)
                }
                .map { it.roomId }
                .toSet()
        val childLinkFlows = localSpaces.filter { it.joined }.map { matrixClient.getSpaceChildLinks(it.roomId) }
        val parentLinksFlow =
            getSpaceParents(matrixClient, joinedRoomIds).map { validatedParents ->
                validatedParents.associate { parent -> parent.roomId to parent.children }
            }
        val isSpaceByRoomId =
            if (loadDetails)
                localRooms
                    .filter { it.createEventContent != null }
                    .associate { it.roomId to (it.type == CreateEventContent.RoomType.Space) }
            else emptyMap()
        combine(childLinkFlows + parentLinksFlow) { hierarchyLinkSnapshots ->
                val childrenBySpace = localSpaces.associate { it.roomId to mutableSetOf<RoomId>() }
                hierarchyLinkSnapshots.forEach { childLinksBySpace ->
                    childLinksBySpace.forEach { (spaceId, childIds) -> childrenBySpace[spaceId]?.addAll(childIds) }
                }
                childrenBySpace
            }
            .distinctUntilChanged()
            .flatMapLatest { childrenBySpace ->
                localSpaces
                    .topLevelOverviewItems(childrenBySpace, isSpaceByRoomId, loadDetails)
                    .withNotificationCounts(matrixClient, childrenBySpace, localRooms)
            }
            .flatMapLatest { topLevelSpaces -> getNamedItems(topLevelSpaces, matrixClient) }
    }
}

private fun List<SpaceBrowserItem>.withNotificationCounts(
    matrixClient: MatrixClient,
    childrenBySpace: Map<RoomId, Set<RoomId>>,
    localRooms: List<Room>,
): Flow<List<SpaceBrowserItem>> {
    if (isEmpty()) return flowOf(emptyList())
    val activeRoomIds =
        localRooms
            .filter { it.membership == Membership.JOIN || it.membership == Membership.INVITE }
            .map { it.roomId }
            .toSet()
    return combine(
        map { space ->
            val childRoomIds =
                childrenBySpace[space.roomId].orEmpty().filter { it != space.roomId && it in activeRoomIds }
            if (!space.joined || childRoomIds.isEmpty()) flowOf(space)
            else
                combine(childRoomIds.map { matrixClient.notification.getCount(it) }) { counts ->
                    space.copy(notificationCount = getNotificationDisplayCount(counts.sum()))
                }
        }
    ) { spaces ->
        spaces.toList()
    }
}

private fun MatrixClient.getSpaceChildLinks(spaceId: RoomId): Flow<Map<RoomId, Set<RoomId>>> {
    return room.getAllState(spaceId, ChildEventContent::class).flattenNotNull().map { childEvents ->
        val childIds =
            childEvents
                .filterValues { it.content.via.isNotEmpty() }
                .keys
                .filter { RoomId.isValid(it) }
                .map(::RoomId)
                .toSet()
        mapOf(spaceId to childIds)
    }
}

private fun List<SpaceBrowserItem>.topLevelOverviewItems(
    childrenBySpace: Map<RoomId, Set<RoomId>>,
    isSpaceByRoomId: Map<RoomId, Boolean>,
    loadDetails: Boolean,
): List<SpaceBrowserItem> {
    val hierarchyRoots = spaceHierarchyRoots(childrenBySpace)
    return filter { it.roomId in hierarchyRoots.topLevel }
        .map { localSpace ->
            val space = localSpace.copy(isInLoop = localSpace.roomId in hierarchyRoots.loopMembers)
            if (loadDetails && space.joined)
                space.withChildStatistics(childrenBySpace.getValue(space.roomId), isSpaceByRoomId)
            else space
        }
}
