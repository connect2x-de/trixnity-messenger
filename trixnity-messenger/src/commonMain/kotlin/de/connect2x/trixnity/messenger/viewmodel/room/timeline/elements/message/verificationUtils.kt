package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStep

internal fun MatrixClient.verificationStartedBy(
    content: VerificationStep,
    roomId: RoomId,
    coroutineScope: CoroutineScope,
    initials: Initials,
): Flow<UserInfoElement?> =
    content.relatesTo?.eventId?.let { relatedEventId ->
        room.getTimelineEvent(roomId, relatedEventId).filterNotNull().map {
            user.getById(roomId, it.sender).filterNotNull().first().toUserInfoElement(
                coroutineScope,
                this,
                initials,
                avatarSize().toLong(),
            )
        }
    } ?: flowOf(null)
