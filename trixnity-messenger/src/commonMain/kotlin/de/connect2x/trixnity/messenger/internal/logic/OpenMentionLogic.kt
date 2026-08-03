package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.push
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.JoinRoomActionRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.UserProfileRoute
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import kotlinx.coroutines.flow.firstOrNull

internal interface OpenMentionLogic {
    suspend fun openMention(userId: UserId, timelineElementMention: TimelineElementMention)
}

internal fun OpenMentionLogic(
    selectedRoomIdLogic: SelectedRoomIdLogic,
    routeNavigation: RouteNavigation,
    matrixClients: MatrixClients,
): OpenMentionLogic {
    return OpenMentionLogicImpl(
        selectedRoomIdLogic = selectedRoomIdLogic,
        routeNavigation = routeNavigation,
        matrixClients = matrixClients,
    )
}

internal class OpenMentionLogicImpl(
    private val selectedRoomIdLogic: SelectedRoomIdLogic,
    private val routeNavigation: RouteNavigation,
    private val matrixClients: MatrixClients,
) : OpenMentionLogic {
    override suspend fun openMention(userId: UserId, timelineElementMention: TimelineElementMention) {
        when (timelineElementMention) {
            is TimelineElementMention.User -> {
                val otherUserId = timelineElementMention.user.userId

                // TODO: find out where the mentioned userId is located instead of assuming the mention source
                val roomId =
                    selectedRoomIdLogic.selectedRoomId
                        ?: run {
                            log.warn { "Could not open User Profile $otherUserId, no room selected" }
                            return
                        }

                log.debug { "Opening User Profile $otherUserId" }
                openUserProfile(userId, roomId, otherUserId)
            }

            is TimelineElementMention.Room -> {
                log.debug { "Opening Room ${timelineElementMention.room.roomId}" }
                val roomId = timelineElementMention.room.roomId
                selectRoom(userId, roomId, timelineElementMention.room.via)
            }

            is TimelineElementMention.Event -> {
                log.debug { "Opening Room ${timelineElementMention.room.roomId}" }
                val roomId = timelineElementMention.room.roomId
                val eventId = timelineElementMention.event.eventId
                selectRoom(userId, roomId, timelineElementMention.room.via)
                // TODO: implement and open event view
                log.warn { "EventView to display $eventId not implemented yet" }
            }
        }
    }

    private fun openUserProfile(sourceUserId: UserId, roomId: RoomId, userId: UserId) {
        routeNavigation.updateNavigation { replace<UserProfileRoute>(UserProfileRoute(sourceUserId, roomId, userId)) }
    }

    private suspend fun selectRoom(userId: UserId, roomId: RoomId, via: Set<String>?) {
        val matrixClient = checkNotNull(matrixClients.value[userId]) { "cannot find MatrixClient for $userId" }
        val membership = matrixClient.room.getById(roomId).firstOrNull()?.membership

        // TODO (fhilgers): show preview of timeline when room is world_readable
        if (membership.shouldShowJoinRoomAction()) {
            routeNavigation.updateNavigation { push<JoinRoomActionRoute>(JoinRoomActionRoute(userId, roomId, via)) }
        } else {
            routeNavigation.updateNavigation { openRoom(userId, roomId) }
        }
    }

    private fun Membership?.shouldShowJoinRoomAction(): Boolean {
        return when (this) {
            Membership.JOIN,
            Membership.LEAVE,
            Membership.BAN -> false

            Membership.INVITE,
            Membership.KNOCK,
            null -> true
        }
    }

    private companion object {
        private val log = Logger("de.connect2x.trixnity.messenger.internal.logic.OpenMentionLogic")
    }
}
