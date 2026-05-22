package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.UserSearchHandler
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout

private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.messenger.CreateRoomsKt")

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
suspend fun MatrixMessengerWithRoot.createChatWithUser(username: String) =
    with(root) {
        withTimeout(15.seconds) {
            log.info { "create a chat with user '$username'" }
            val roomListRouterStack = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel.roomListRouterStack
            val roomListViewModel = roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
            roomListViewModel.createNewRoom()
            val createNewChatViewModel =
                roomListRouterStack.waitFor(RoomListRouter.Wrapper.CreateNewChat::class).viewModel
            createNewChatViewModel.onUserClick(
                searchForUser(username, createNewChatViewModel.createNewRoomViewModel.searchHandler)
            )
            log.debug { "chat should have been created -> check to find it in the list" }
            val sortedRoomListElementViewModels =
                roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel.elements
            sortedRoomListElementViewModels
                .flatMapLatest { roomListElements ->
                    combine(
                        roomListElements
                            .filter { !it.isLeave.debounce(100.milliseconds).filterNotNull().first() }
                            .map { it.roomName }
                    ) { names ->
                        log.debug { "roomNames: ${names.joinToString { it ?: "<unknown>" }}" }
                        names.any { it == username }
                    }
                }
                .first { it }
            log.debug { "found room -> return" }
            sortedRoomListElementViewModels.value
                .filter { !it.isLeave.debounce(100.milliseconds).filterNotNull().first() }
                .first { it.roomName.value == username }
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun MatrixMessengerWithRoot.createGroupWithUsers(groupName: String, vararg usernames: String) =
    with(root) {
        withTimeout(20.seconds) {
            log.info { "create a group '$groupName' with users '${usernames.joinToString { it }}'" }
            val roomListRouterStack = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel.roomListRouterStack
            val roomListViewModel = roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
            roomListViewModel.createNewRoom()
            roomListRouterStack.waitFor(RoomListRouter.Wrapper.CreateNewChat::class).viewModel.createGroup()
            val createNewGroupViewModel =
                roomListRouterStack.waitFor(RoomListRouter.Wrapper.CreateNewGroup::class).viewModel
            usernames.forEach { username ->
                log.debug { "search for user '$username'" }
                createNewGroupViewModel.onUserClick(
                    searchForUser(username, createNewGroupViewModel.createNewRoomViewModel.searchHandler)
                )
            }
            createNewGroupViewModel.optionalRoomName.update(groupName)
            createNewGroupViewModel.createNewGroup()
            log.debug { "group '$groupName' should have been created -> check to find it in the list" }
            val sortedRoomListElementViewModels =
                roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel.elements
            sortedRoomListElementViewModels
                .flatMapLatest { roomListElements ->
                    combine(roomListElements.map { it.roomName }) { roomNames ->
                        log.debug { "roomNames: ${roomNames.joinToString { it ?: "<unknown>" }}" }
                        roomNames.any { it == groupName }
                    }
                }
                .first { it }
            log.debug { "found group -> return" }
            sortedRoomListElementViewModels.value.first { it.roomName.value == groupName }
        }
    }

private suspend fun searchForUser(username: String, searchHandler: UserSearchHandler): Search.SearchUserElement {
    return withTimeout(5.seconds) {
        var foundUser: Search.SearchUserElement? = null
        while (foundUser == null) {
            log.debug { "search for user '$username'" }
            searchHandler.searchTerm.update("")
            delay(100.milliseconds) // for distinctUntilChanged() to recognize the change
            searchHandler.searchTerm.update(username)
            searchHandler.waitForUserResults.first { it }
            searchHandler.waitForUserResults.first { it.not() }
            val found = select {
                async { searchHandler.foundUsers.first { it.isNotEmpty() } }.onAwait { it }
                async { delay(1.seconds) }.onAwait { emptyList() }
            }
            foundUser = found.find { it.displayName == username }
            delay(500.milliseconds)
        }
        foundUser
    }
}
