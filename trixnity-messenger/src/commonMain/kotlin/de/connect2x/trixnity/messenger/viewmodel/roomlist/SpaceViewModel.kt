package de.connect2x.trixnity.messenger.viewmodel.roomlist

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getAllState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent

private val log = KotlinLogging.logger { }

data class SpaceViewModel(
    val roomId: RoomId,
    val name: String,
    val image: ByteArray?,
    val initials: String,
)

internal fun RoomId.roomsInThisSpace(matrixClient: MatrixClient): Flow<List<RoomId>> {
    log.debug { "rooms in space: $this" }
    return matrixClient.room.getAllState<ChildEventContent>(this).map { stateEvents ->
        stateEvents?.map { (child, _) ->
            RoomId(child)
        } ?: listOf()
    }
}