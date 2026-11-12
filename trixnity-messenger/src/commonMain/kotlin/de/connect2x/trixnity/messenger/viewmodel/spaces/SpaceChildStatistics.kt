package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.type
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.space.ChildEventContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun SpaceBrowserItem.withChildStatistics(
    children: Set<RoomId>,
    knownSpaces: Map<RoomId, Boolean>,
): SpaceBrowserItem {
    return copy(
        childCount = children.size,
        subspaceCount = children.count { knownSpaces[it] == true },
        unknownChildCount = children.count { it !in knownSpaces },
    )
}

internal fun spaceChildStatistics(items: List<SpaceBrowserItem>, client: MatrixClient): Flow<List<SpaceBrowserItem>> {
    val spaces = items.filter { it.isSpace && it.joined }
    if (spaces.isEmpty()) return flowOf(items)
    val children =
        combine(
            spaces.map { space ->
                client.room.getAllState(space.roomId, ChildEventContent::class).flatten().map { events ->
                    space.roomId to
                        events.filterValues { it != null && it.content.via.isNotEmpty() }.keys.map(::RoomId).toSet()
                }
            }
        ) {
            it.toMap()
        }
    val knownSpaces =
        client.room
            .getAll()
            .flatten()
            .map { rooms ->
                rooms
                    .filterValues { it?.createEventContent != null }
                    .mapValues { it.value?.type == CreateEventContent.RoomType.Space }
            }
            .distinctUntilChanged()
    return combine(children, knownSpaces) { links, types ->
            val knownTypes = items.associate { it.roomId to it.isSpace } + types
            items.map { item -> links[item.roomId]?.let { item.withChildStatistics(it, knownTypes) } ?: item }
        }
        .distinctUntilChanged()
}
