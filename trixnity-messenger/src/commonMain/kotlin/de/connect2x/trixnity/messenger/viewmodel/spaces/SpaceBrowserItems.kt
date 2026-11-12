package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.store.joinedMemberCount
import de.connect2x.trixnity.client.store.type
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.component.get

internal fun ViewModelContext.getNamedItems(
    items: List<SpaceBrowserItem>,
    client: MatrixClient,
): Flow<List<SpaceBrowserItem>> {
    if (items.isEmpty()) return flowOf(emptyList())
    return combine(
        items.map { item ->
            if (item.joined)
                get<RoomName>().getRoomName(item.roomId, client, formatted = false).map { item.copy(name = it) }
            else flowOf(item)
        }
    ) {
        it.toList()
    }
}

internal fun Room.toBrowserItem(userId: UserId): SpaceBrowserItem {
    return SpaceBrowserItem(
        userId,
        roomId,
        name?.explicitName ?: roomId.full,
        isSpace = type == CreateEventContent.RoomType.Space,
        joined = membership == Membership.JOIN,
        invited = membership == Membership.INVITE,
        canJoin = membership == Membership.INVITE,
        memberCount = joinedMemberCount,
        avatarUrl = avatarUrl,
    )
}

internal fun List<SpaceBrowserItem>.sortedByName(): List<SpaceBrowserItem> {
    return sortedWith(
        compareBy<SpaceBrowserItem> { it.name.lowercase() }.thenBy { it.userId.full }.thenBy { it.roomId.full }
    )
}
