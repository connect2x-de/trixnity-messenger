package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.messenger.RoomsKt")

suspend fun MatrixMessengerWithRoot.rejectTheInvitationToRoomAndBlock(roomId: RoomId) =
    with(root) {
        withTimeout(10.seconds) {
            log.info { "reject the invitation at $roomId" }
            log.debug { "found room $roomId, now reject the invitation" }
            findRoomWithId(roomId).rejectInvitationAndBlockInviter()
            stack
                .waitFor(RootRouter.Wrapper.Main::class)
                .viewModel
                .roomListRouterStack
                .waitFor(RoomListRouter.Wrapper.List::class)
                .viewModel
                .elements
                .first { it.count { it.roomId == roomId } == 1 }
            Unit
        }
    }

suspend fun MatrixMessengerWithRoot.acceptInvitationToRoom(roomId: RoomId) =
    with(root) {
        withTimeout(10.seconds) {
            log.info { "accept the invitation to room $roomId" }
            val roomListElementViewModel = findRoomWithId(roomId)
            roomListElementViewModel.isInvite.first { it == true }
            roomListElementViewModel.acceptInvitation()
            val roomName = roomListElementViewModel.roomName.first { it?.startsWith("invitation")?.not() == true }
            log.info { "accepted invitation to room $roomId -> check whether room is open" }
            val timelineViewModel =
                stack
                    .waitFor(RootRouter.Wrapper.Main::class)
                    .viewModel
                    .roomRouterStack
                    .waitFor(RoomRouter.Wrapper.View::class)
                    .viewModel
                    .timelineStack
                    .waitFor(TimelineRouter.Wrapper.View::class)
                    .viewModel
            timelineViewModel.roomHeaderViewModel.roomHeaderInfo
                .filter { !it.isLeave }
                .map { it.roomName }
                .first { it == roomName }
        }
    }

suspend fun MatrixMessengerWithRoot.leaveRoom(roomId: RoomId) =
    with(root) {
        withTimeout(15.seconds) {
            log.info { "leave room $roomId" }
            val roomName = findRoomWithId(roomId).roomName.first { it != null }
            val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
            val roomListViewModel =
                mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
            roomListViewModel.selectRoom(roomId)
            val timelineViewModel =
                mainViewModel.roomRouterStack
                    .waitFor(RoomRouter.Wrapper.View::class)
                    .viewModel
                    .timelineStack
                    .waitFor(TimelineRouter.Wrapper.View::class)
                    .viewModel
            timelineViewModel.leaveRoom()
            log.debug { "left room $roomId" }
            mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.None::class)
            roomListViewModel.elements.first { roomListElements -> roomListElements.count { it.roomId == roomId } == 1 }
            log.debug { "left room is no longer in room list" }
        }
    }

suspend fun MatrixMessengerWithRoot.findRoomWithId(roomId: RoomId) =
    with(root) {
        withTimeout(10.seconds) {
            log.info { "try to find the room $roomId" }
            val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
            val roomListRouterStack = mainViewModel.roomListRouterStack
            val roomListElements =
                roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel.elements.first {
                    roomListElements ->
                    log.debug { "found ${roomListElements.size} rooms" }
                    roomListElements.any {
                        log.trace { "found ${it.roomId}" }
                        it.roomId == roomId
                    }
                }
            roomListElements.first()
        }
    }

suspend fun MatrixMessengerWithRoot.getAllRooms(username: String) =
    with(root) {
        di.get<MatrixClients>()
            .value
            .entries
            .find { (userId, _) -> userId.localpart == username }
            ?.let { (_, matrixClient) ->
                matrixClient.room.getAll().first().values.map { it.first() }.also { log.debug { "found rooms: $it" } }
            } ?: emptyList()
    }
