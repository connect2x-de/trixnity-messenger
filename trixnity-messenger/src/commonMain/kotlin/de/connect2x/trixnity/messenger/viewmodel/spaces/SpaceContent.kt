package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.client.store.joinedMemberCount
import de.connect2x.trixnity.clientserverapi.model.room.GetHierarchy
import de.connect2x.trixnity.clientserverapi.model.room.GetHierarchy.Response.SpaceHierarchyRoomsChunk
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent.StateBaseEvent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.TopicEventContent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.getMatrixClient
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.get

internal suspend fun ViewModelContext.joinSpaceRoom(item: SpaceBrowserItem) {
    require(item.canJoin && !item.joined)
    val client = getMatrixClient(item.userId)
    client.api.room.joinRoom(item.roomId, via = item.via.takeIf { it.isNotEmpty() }).getOrThrow()
    client.waitForMembershipSync(item.roomId) { it == Membership.JOIN }
}

internal suspend fun ViewModelContext.rejectSpaceInvite(item: SpaceBrowserItem) {
    require(item.invited && !item.joined)
    val client = getMatrixClient(item.userId)
    check(client.room.getById(item.roomId).first()?.membership == Membership.INVITE)
    client.api.room.leaveRoom(item.roomId).getOrThrow()
    client.waitForMembershipSync(item.roomId) { it != Membership.INVITE }
}

private suspend fun MatrixClient.waitForMembershipSync(roomId: RoomId, matches: (Membership?) -> Boolean) {
    room.getById(roomId).first { matches(it?.membership) }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun ViewModelContext.getJoinedSpaceContent(userId: UserId, spaceId: RoomId): Flow<SpaceContent> {
    return matrixClients
        .map { it[userId] }
        .distinctUntilChanged()
        .flatMapLatest { client ->
            checkNotNull(client) { "Account is no longer available" }
            getJoinedSpaceChildren(client, spaceId, getSpaceParents(client, spaceId)) { space, rooms ->
                    check(space?.membership == Membership.JOIN) { "Space is no longer joined" }
                    SpaceContent(
                        items = rooms.map { it.toBrowserItem(userId) },
                        userId = userId,
                        avatarUrl = space.avatarUrl,
                    )
                }
                .distinctUntilChanged()
                .flatMapLatest { content ->
                    combine(
                        getNamedItems(content.items, client).flatMapLatest {
                            spaceChildStatistics(it.sortedByName(), client)
                        },
                        get<RoomName>().getRoomName(spaceId, client, formatted = false),
                    ) { items, name ->
                        content.copy(items = items, name = name)
                    }
                }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun ViewModelContext.getSpaceExploreContent(userId: UserId, spaceId: RoomId): Flow<SpaceContent> {
    return matrixClients
        .map { it[userId] }
        .distinctUntilChanged()
        .flatMapLatest { client ->
            checkNotNull(client) { "Account is no longer available" }
            val space = client.room.getById(spaceId)
            space
                .map { it?.membership }
                .distinctUntilChanged()
                .flatMapLatest { membership ->
                    when (membership) {
                        Membership.INVITE ->
                            space.filterNotNull().map { invitedSpace ->
                                val invitation = invitedSpace.toBrowserItem(userId)
                                SpaceContent(
                                    name = invitation.name,
                                    items = listOf(invitation),
                                    userId = userId,
                                    avatarUrl = invitedSpace.avatarUrl,
                                    invited = true,
                                )
                            }
                        Membership.JOIN -> getJoinedSpaceExploreContent(client, userId, spaceId)
                        Membership.LEAVE,
                        Membership.BAN,
                        Membership.KNOCK,
                        null -> error("Space is no longer joined")
                    }
                }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun ViewModelContext.getJoinedSpaceExploreContent(
    client: MatrixClient,
    userId: UserId,
    spaceId: RoomId,
): Flow<SpaceContent> {
    val children = client.room.getAllState(spaceId, ChildEventContent::class).flatten()
    val hierarchy =
        children
            .map { events -> events.mapValues { it.value?.content?.via } }
            .distinctUntilChanged()
            .flatMapLatest {
                loadSpaceHierarchy { from -> client.api.room.getHierarchy(spaceId, from = from, maxDepth = 1) }
            }
    val content =
        combine(client.room.getAll().flatten(), children, hierarchy, getSpaceParents(client, spaceId)) {
                rooms,
                childEvents,
                hierarchy,
                parents ->
                val space = rooms[spaceId]
                check(space?.membership == Membership.JOIN) { "Space is no longer joined" }
                val items = buildSpaceHierarchyItems(userId, spaceId, rooms, childEvents, hierarchy.rooms, parents)
                SpaceContent(
                    space.name?.explicitName ?: spaceId.full,
                    items,
                    loadingHierarchy = hierarchy.loading,
                    hierarchyFailed = hierarchy.failed,
                    userId = userId,
                    avatarUrl = space.avatarUrl,
                    memberCount =
                        space.joinedMemberCount ?: hierarchy.rooms.find { it.roomId == spaceId }?.joinedMembersCount,
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { content ->
                getNamedItems(content.items, client)
                    .flatMapLatest { spaceChildStatistics(it, client) }
                    .map { content.copy(items = it) }
            }
    return combine(
        content,
        get<RoomName>().getRoomName(spaceId, client, formatted = false),
        client.room.getState<TopicEventContent>(spaceId),
    ) { content, name, topic ->
        content.copy(
            name = name,
            topic = (topic?.content?.topic?.text?.plain ?: topic?.content?.legacy?.topic)?.takeIf { it.isNotBlank() },
        )
    }
}

internal data class SpaceHierarchy(
    val rooms: List<SpaceHierarchyRoomsChunk> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

internal fun loadSpaceHierarchy(fetch: suspend (String?) -> Result<GetHierarchy.Response>): Flow<SpaceHierarchy> {
    return flow {
        val rooms = linkedMapOf<RoomId, SpaceHierarchyRoomsChunk>()
        val seenTokens = mutableSetOf<String>()
        var from: String? = null
        emit(SpaceHierarchy())
        try {
            do {
                val batch = fetch(from).getOrThrow()
                batch.rooms.forEach { rooms[it.roomId] = it }
                from = batch.nextBatch
                check(from == null || seenTokens.add(from)) { "Repeated hierarchy pagination token" }
                emit(SpaceHierarchy(rooms.values.toList(), loading = from != null))
            } while (from != null)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emit(SpaceHierarchy(rooms.values.toList(), loading = false, failed = true))
        }
    }
}

internal fun spaceChildComparator(children: Map<String, StateBaseEvent<*>>): Comparator<SpaceBrowserItem> {
    fun order(item: SpaceBrowserItem): String? {
        return (children[item.roomId.full]?.content as? ChildEventContent)?.order?.takeIf {
            it.length <= 50 && it.all { char -> char in ' '..'~' }
        }
    }
    return compareBy<SpaceBrowserItem> { order(it) == null }
        .thenBy { order(it) }
        .thenBy { children[it.roomId.full]?.originTimestamp ?: Long.MAX_VALUE }
        .thenBy { it.roomId.full }
}
