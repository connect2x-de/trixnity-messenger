package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.RoomId

fun mentionedUsersStateFlow(
    content: String,
    roomId: RoomId,
    matrixClient: MatrixClient,
    coroutineScope: CoroutineScope
): Map<String, StateFlow<UserInfoElement>> =
    MatrixRegex.findUserMentions(content)
        .mapValues { (_, userId) ->
            matrixClient.user.getById(roomId, userId)
                .filterNotNull()
                .map {
                    it.toUserInfoElement()
                }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), UserInfoElement(""))
        }
