@file:OptIn(ExperimentalCoroutinesApi::class)

package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.createRoot
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.VerificationRequest
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationDoneTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsOverviewViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaRouter
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

class MatrixMessengerWithRoot(
    delegate: MatrixMessenger,
    val root: RootViewModel = delegate.createRoot()
) : MatrixMessenger by delegate

suspend fun MatrixMessengerWithRoot.login(
    serverUrl: String,
    username: String,
    password: String,
    recoveryKey: String? = null,
    otherMessenger: MatrixMessengerWithRoot? = null,
): String? = with(root) {
    log.debug { " +- ADD ACCOUNT" }
    addMatrixAccountViaPassword(serverUrl, username, password)
    log.debug { " +- try login" }
    val main = stack.waitFor(RootRouter.Wrapper.Main::class)
    log.info { " +- main view" }
    val mainViewModel = main.viewModel
    mainViewModel.accountSetupRouterStack.waitFor(AccountSetupRouter.Wrapper.ShowAccountSetup::class).viewModel.closeAccountSetup()
    mainViewModel.accountSetupRouterStack.waitFor(AccountSetupRouter.Wrapper.None::class)
    val verification = withTimeoutOrNull(15.seconds) {
        mainViewModel.selfVerificationStack.toFlow().first { childStack ->
            log.debug { " active: ${childStack.active.instance}" }
            childStack.active.instance is SelfVerificationRouter.Wrapper.CrossSigningBootstrap
        }.active.instance
    }
    if (verification == null) {
        mainViewModel.openSelfVerification(UserId("@$username:localhost:8008"))
        val verificationView = mainViewModel.selfVerificationStack.waitFor(SelfVerificationRouter.Wrapper.View::class)
        if (recoveryKey != null) {
            selfVerify(verificationView, recoveryKey)
            mainViewModel.selfVerificationStack.waitFor(SelfVerificationRouter.Wrapper.None::class)
            log.info { "self verification done successfully" }
        } else {
            if (otherMessenger != null) {
                selfVerify(verificationView, mainViewModel, otherMessenger.root)
            } else {
                log.error { "cannot self verify without recovery key or other device" }
                throw IllegalStateException("cannot self verify without recovery key or other device")
            }
        }
        return recoveryKey
    } else {
        return bootstrap(verification, username, password)
    }
}

suspend fun MatrixMessengerWithRoot.createNewAccount(
    serverUrl: String,
    username: String,
    password: String,
    recoveryKey: String? = null,
): String? = with(root) {
    val accountsOverviewViewModel = openAccountsOverview()
    accountsOverviewViewModel.createNewAccount()
    val thisRecoveryKey = login(serverUrl, username, password, recoveryKey)
    val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
    mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.Wrapper.None::class)
    return thisRecoveryKey ?: recoveryKey
}

suspend fun MatrixMessengerWithRoot.deleteAccount(username: String) = with(root) {
    val accountsOverviewViewModel = openAccountsOverview()
    val userId = di.get<MatrixClients>().value.keys.find { it.localpart == username }
    checkNotNull(userId)
    accountsOverviewViewModel.removeAccount(userId)
    stack.waitFor(RootRouter.Wrapper.RemoveMatrixAccount::class)
    val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
    mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.Wrapper.None::class)
    log.debug { " +- delete account finished" }
}

suspend fun MatrixMessengerWithRoot.verifyAccountsArePresent(vararg usernames: String) = with(root) {
    val accountsOverviewViewModel = openAccountsOverview()
    withTimeout(5.seconds) {
        eventually(4.seconds) {
            accountsOverviewViewModel.accounts
                .map { accounts -> accounts.map { it.userId.localpart } }
                .first { it.containsAll(usernames.toList()) }
        }
        accountsOverviewViewModel.close()
        stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
            .roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
    }
}

suspend fun MatrixMessengerWithRoot.registerAccountWithToken(serverUrl: String, registrationToken: String) =
    with(root) {
        withTimeout(10.seconds) {
            val addMatrixAccountViewModel =
                stack.waitFor(RootRouter.Wrapper.AddMatrixAccount::class).viewModel
            addMatrixAccountViewModel.serverUrl.update(serverUrl)
            val registerMethod = addMatrixAccountViewModel.serverDiscoveryState
                .filterIsInstance<AddMatrixAccountViewModel.ServerDiscoveryState.Success>().first()
                .addMatrixAccountMethods.filterIsInstance<AddMatrixAccountMethod.Register>().first()
            addMatrixAccountViewModel.selectAddMatrixAccountMethod(registerMethod)
            val registerMatrixAccountViewModel =
                stack.waitFor(RootRouter.Wrapper.RegisterMatrixAccount::class).viewModel
            registerMatrixAccountViewModel.username.update("user1")
            registerMatrixAccountViewModel.password.update("user1password")
            registerMatrixAccountViewModel.canRegisterNewUser.first { it }
            registerMatrixAccountViewModel.register()
            authorizeUia(registrationToken)

            val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
            mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel.accountViewModel.accounts.first { it.isNotEmpty() }
            mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.Wrapper.None::class)
        }
    }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
suspend fun MatrixMessengerWithRoot.createChatWithUser(username: String) = with(root) {
    withTimeout(15.seconds) {
        log.info { "create a chat with user '$username'" }
        val roomListRouterStack = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel.roomListRouterStack
        val roomListViewModel = roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.createNewRoom()
        val createNewChatViewModel = roomListRouterStack.waitFor(RoomListRouter.Wrapper.CreateNewChat::class).viewModel
        log.debug { "search for user '$username'" }
        val searchHandler = createNewChatViewModel.createNewRoomViewModel.searchHandler
        searchHandler.searchTerm.update(username)
        searchHandler.waitForUserResults.first { it }
        searchHandler.waitForUserResults.first { it.not() }
        val users = searchHandler.foundUsers.first { users -> users.any { it.displayName == username } }
        createNewChatViewModel.onUserClick(users.first())
        log.debug { "chat should have been created -> check to find it in the list" }
        val sortedRoomListElementViewModels = roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
            .viewModel.elements
        sortedRoomListElementViewModels.flatMapLatest { roomListElements ->
            combine(roomListElements.filter { !it.isLeave.debounce(100.milliseconds).filterNotNull().first() }
                .map { it.roomName }) { names ->
                log.debug { "roomNames: ${names.joinToString { it ?: "<unknown>" }}" }
                names.any { it == username }
            }
        }.first { it }
        log.debug { "found room -> return" }
        sortedRoomListElementViewModels.value.filter {
            !it.isLeave.debounce(100.milliseconds).filterNotNull().first()
        }
            .first { it.roomName.value == username }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun MatrixMessengerWithRoot.createGroupWithUsers(groupName: String, vararg usernames: String) = with(root) {
    withTimeout(20.seconds) {
        log.info { "create a group '$groupName' with users '${usernames.joinToString { it }}'" }
        val roomListRouterStack = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel.roomListRouterStack
        val roomListViewModel = roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.createNewRoom()
        roomListRouterStack.waitFor(RoomListRouter.Wrapper.CreateNewChat::class).viewModel.createGroup()
        val createNewGroupViewModel =
            roomListRouterStack.waitFor(RoomListRouter.Wrapper.CreateNewGroup::class).viewModel
        val searchHandler = createNewGroupViewModel.createNewRoomViewModel.searchHandler
        usernames.forEach { username ->
            log.debug { "search for user '$username'" }
            searchHandler.searchTerm.update(username)
            searchHandler.waitForUserResults.first { it.not() }
            val users = searchHandler.foundUsers.first { users -> users.any { it.displayName == username } }
            createNewGroupViewModel.onUserClick(users.first())
        }
        createNewGroupViewModel.optionalRoomName.update(groupName)
        createNewGroupViewModel.createNewGroup()
        log.debug { "group '$groupName' should have been created -> check to find it in the list" }
        val sortedRoomListElementViewModels = roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
            .viewModel.elements
        sortedRoomListElementViewModels.flatMapLatest { roomListElements ->
            combine(roomListElements.map { it.roomName }) { roomNames ->
                log.debug { "roomNames: ${roomNames.joinToString { it ?: "<unknown>" }}" }
                roomNames.any { it == groupName }
            }
        }.first { it }
        log.debug { "found group -> return" }
        sortedRoomListElementViewModels.value.first { it.roomName.value == groupName }
    }
}

suspend fun MatrixMessengerWithRoot.rejectTheInvitationToRoomAndBlock(roomId: RoomId) = with(root) {
    withTimeout(10.seconds) {
        log.info { "reject the invitation at $roomId" }
        log.debug { "found room $roomId, now reject the invitation" }
        findRoomWithId(roomId).rejectInvitationAndBlockInviter()
        stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
            .roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
            .elements.first { it.count { it.roomId == roomId } == 1 }
        Unit
    }
}

suspend fun MatrixMessengerWithRoot.acceptInvitationToRoom(roomId: RoomId) = with(root) {
    withTimeout(10.seconds) {
        log.info { "accept the invitation to room $roomId" }
        val roomListElementViewModel = findRoomWithId(roomId)
        roomListElementViewModel.isInvite.first { it == true }
        roomListElementViewModel.acceptInvitation()
        val roomName = roomListElementViewModel.roomName.first { it?.startsWith("invitation")?.not() == true }
        log.info { "accepted invitation to room $roomId -> check whether room is open" }
        val timelineViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
            .roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
            .timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
        timelineViewModel.roomHeaderViewModel.roomHeaderInfo.filter { !it.isLeave }
            .map { it.roomName }.first { it == roomName }
    }
}

suspend fun MatrixMessengerWithRoot.leaveRoom(roomId: RoomId) = with(root) {
    withTimeout(15.seconds) {
        log.info { "leave room $roomId" }
        val roomName = findRoomWithId(roomId).roomName.first { it != null }
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        val roomListViewModel = mainViewModel
            .roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.selectRoom(roomId)
        val timelineViewModel = mainViewModel
            .roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
            .timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
        timelineViewModel.roomHeaderViewModel.roomHeaderInfo.map { it.roomName }.first { it == roomName }
        timelineViewModel.leaveRoom()
        log.debug { "left room $roomId" }
        mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.None::class)
        roomListViewModel.elements.first { roomListElements ->
            roomListElements.count { it.roomId == roomId } == 1
        }
        log.debug { "left room is no longer in room list" }
    }
}

suspend fun MatrixMessengerWithRoot.findRoomWithId(roomId: RoomId) = with(root) {
    withTimeout(10.seconds) {
        log.info { "try to find the room $roomId" }
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        val roomListRouterStack = mainViewModel.roomListRouterStack
        val roomListElements = roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
            .viewModel.elements.first { roomListElements ->
                log.debug { "found ${roomListElements.size} rooms" }
                roomListElements.any {
                    log.trace { "found ${it.roomId}" }
                    it.roomId == roomId
                }
            }
        roomListElements.first()
    }
}

suspend fun MatrixMessengerWithRoot.getAllRooms(username: String) = with(root) {
    di.get<MatrixClients>().value.entries.find { (userId, _) -> userId.localpart == username }
        ?.let { (_, matrixClient) ->
            matrixClient.room.getAll().first().values.map { it.first() }
                .also { log.debug { "found rooms: $it" } }
        } ?: emptyList()

}

suspend fun MatrixMessengerWithRoot.sendMessage(roomId: RoomId, message: String) = with(root) {
    withTimeout(20.seconds) {
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        val roomListViewModel =
            mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.selectRoom(roomId)
        val roomViewModel = mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
        val timelineViewModel = roomViewModel.timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
        val inputAreaViewModel = timelineViewModel.inputAreaViewModel
        val job = launch { inputAreaViewModel.isSendEnabled.collect {} }
        inputAreaViewModel.textField.update(message)
        log.debug { "wait for send to be enabled" }
        inputAreaViewModel.isSendEnabled.first { it }
        inputAreaViewModel.sendMessage()
        timelineViewModel.jumpToEndOfTimeline() // TODO remove?
        findTimelineElement<TimelineElementHolderViewModel>(roomViewModel) { vm ->
            vm is TextBased.Text && vm.body == message
        }
        job.cancel()
    }
}

suspend fun MatrixMessengerWithRoot.findMessage(roomId: RoomId, message: String): Boolean = with(root) {
    withTimeoutOrNull(10.seconds) {
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        val roomListViewModel =
            mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.selectRoom(roomId)
        val roomViewModel = mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
        findTimelineElement<TimelineElementHolderViewModel>(roomViewModel) { vm ->
            vm is TextBased.Text && vm.body == message
        }
        true
    } ?: false
}

suspend fun MatrixMessengerWithRoot.logout(userId: UserId) = with(root) {
    withTimeout(20.seconds) {
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        val roomListViewModel =
            mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.openAccountsOverview()
        val accountsOverviewViewModel =
            mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.AccountsOverview::class).viewModel
        accountsOverviewViewModel.removeAccount(userId)
        stack.waitFor(RootRouter.Wrapper.RemoveMatrixAccount::class)
        // since we do not have multiple accounts here, we have to wait for this view and not the MainView
        // (the last account was deleted, so we expect the user to log in with a new account)
        stack.waitFor(RootRouter.Wrapper.AddMatrixAccount::class)
        log.debug { " +- logout finished" }
    }
}

private suspend fun RootViewModel.openAccountsOverview(): AccountsOverviewViewModel {
    val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    val roomListViewModel =
        mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
    roomListViewModel.openAccountsOverview()
    return mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.AccountsOverview::class).viewModel
}

private suspend fun RootViewModel.addMatrixAccountViaPassword(
    serverUrl: String,
    username: String,
    password: String,
) {
    val addMatrixAccount = stack.waitFor(RootRouter.Wrapper.AddMatrixAccount::class)
    val addMatrixAccountViewModel = addMatrixAccount.viewModel
    addMatrixAccountViewModel.serverUrl.update(serverUrl)
    val registerMethod = addMatrixAccountViewModel.serverDiscoveryState
        .filterIsInstance<AddMatrixAccountViewModel.ServerDiscoveryState.Success>().first()
        .addMatrixAccountMethods.filterIsInstance<AddMatrixAccountMethod.Password>().first()
    addMatrixAccountViewModel.selectAddMatrixAccountMethod(registerMethod)
    val passwordLoginViewModel =
        stack.waitFor(RootRouter.Wrapper.PasswordLogin::class).viewModel
    addMatrixAccountViewModel.serverUrl.update(serverUrl)
    passwordLoginViewModel.username.update(username)
    passwordLoginViewModel.password.update(password)
    passwordLoginViewModel.canLogin.first { it }
    passwordLoginViewModel.tryLogin()
}

private suspend fun MatrixMessengerWithRoot.bootstrap(
    verification: SelfVerificationRouter.Wrapper,
    username: String,
    password: String,
): String? {
    log.info { "  +- bootstrap" }
    val crossSigningBootstrapViewModel =
        (verification as SelfVerificationRouter.Wrapper.CrossSigningBootstrap).viewModel
    crossSigningBootstrapViewModel.startCrossSigningBootstrap()

    crossSigningBootstrapViewModel.isBootstrapRunning.first { it.not() }
    val createdRecoveryKey = crossSigningBootstrapViewModel.recoveryKey.first { it != null }
    log.info { "user '$username' with password '$password' has recovery key '$createdRecoveryKey'" }

    crossSigningBootstrapViewModel.close()
    log.info { "   - bootstrap finished" }
    return createdRecoveryKey
}

private suspend fun selfVerify(
    verification: SelfVerificationRouter.Wrapper,
    recoveryKey: String,
) {
    log.info { "  +- self verification with recovery key" }
    val selfVerificationViewModel =
        (verification as SelfVerificationRouter.Wrapper.View).viewModel
    selfVerificationViewModel.waitForAvailableVerificationMethods()
    selfVerificationViewModel.selfVerificationMethods.first { it.any { selfVerificationMethod -> selfVerificationMethod is SelfVerificationMethod.AesHmacSha2RecoveryKey } }
    selfVerificationViewModel.launchVerification(
        selfVerificationViewModel.selfVerificationMethods.value.find {
            it is SelfVerificationMethod.AesHmacSha2RecoveryKey
        } ?: throw IllegalStateException("Can only use recovery key method"))
    selfVerificationViewModel.verifyWithRecoveryKey(recoveryKey)
}

private suspend fun selfVerify(
    verification: SelfVerificationRouter.Wrapper,
    mainViewModel: MainViewModel,
    otherMessenger: RootViewModel,
) {
    log.info { "  +- self verification with other device" }
    val selfVerificationViewModel =
        (verification as SelfVerificationRouter.Wrapper.View).viewModel
    selfVerificationViewModel.waitForAvailableVerificationMethods()
    selfVerificationViewModel.selfVerificationMethods.first { it.any { selfVerificationMethod -> selfVerificationMethod is SelfVerificationMethod.CrossSignedDeviceVerification } }
    selfVerificationViewModel.launchVerification(selfVerificationViewModel.selfVerificationMethods.value.find {
        it is SelfVerificationMethod.CrossSignedDeviceVerification
    } ?: throw IllegalStateException("can only use device verification"))

    mainViewModel.selfVerificationStack.waitFor(SelfVerificationRouter.Wrapper.None::class)
    val verificationViewModel =
        mainViewModel.deviceVerificationRouterStack.waitFor(VerificationRouter.Wrapper.Verification::class).viewModel
    verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Wait::class)

    val otherMainViewModel = otherMessenger.stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    val otherVerificationViewModel =
        otherMainViewModel.deviceVerificationRouterStack.waitFor(VerificationRouter.Wrapper.Verification::class).viewModel
    val otherVerificationStepRequestViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Request::class).viewModel
    otherVerificationStepRequestViewModel.next()

    val selectVerificationMethodViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.SelectVerificationMethod::class).viewModel
    selectVerificationMethodViewModel.verificationMethods.size shouldBe 1
    selectVerificationMethodViewModel.acceptVerificationMethod(selectVerificationMethodViewModel.verificationMethods[0].first)

    val otherAcceptSasStartViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.AcceptSasStart::class).viewModel
    otherAcceptSasStartViewModel.accept()

    val verificationStepCompareViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.CompareEmojisOrNumbers::class).viewModel
    val otherVerificationStepCompareViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.CompareEmojisOrNumbers::class).viewModel
    verificationStepCompareViewModel.emojis shouldBeEqual otherVerificationStepCompareViewModel.emojis

    verificationStepCompareViewModel.accept()
    otherVerificationStepCompareViewModel.accept()

    val verificationStepSuccessViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Success::class).viewModel
    val otherVerificationStepSuccessViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Success::class).viewModel
    verificationStepSuccessViewModel.ok()
    otherVerificationStepSuccessViewModel.ok()

    mainViewModel.deviceVerificationRouterStack.toFlow()
        .first { it.active.configuration is VerificationRouter.Config.None }
    otherMainViewModel.deviceVerificationRouterStack.toFlow()
        .first { it.active.configuration is VerificationRouter.Config.None }
}

suspend fun MatrixMessengerWithRoot.authorizeUia(username: String, password: String) = with(root) {
    uiaStack.waitFor(UiaRouter.Wrapper.UiaActionConfirmation::class).viewModel.next()
    val uiaStepPasswordViewModel = uiaStack.waitFor(UiaRouter.Wrapper.UiaStepPassword::class).viewModel
    uiaStepPasswordViewModel.username.update(username)
    uiaStepPasswordViewModel.password.update(password)
    uiaStepPasswordViewModel.submit()
    uiaStack.waitFor(UiaRouter.Wrapper.None::class)
}

suspend fun MatrixMessengerWithRoot.authorizeUia(registrationToken: String) = with(root) {
    uiaStack.waitFor(UiaRouter.Wrapper.UiaActionConfirmation::class).viewModel.next()
    val uiaRegistrationTokenViewModel =
        uiaStack.waitFor(UiaRouter.Wrapper.UiaStepRegistrationToken::class).viewModel
    uiaRegistrationTokenViewModel.registrationToken.update(registrationToken)
    uiaRegistrationTokenViewModel.submit()
    uiaStack.waitFor(UiaRouter.Wrapper.None::class)
}

suspend fun MatrixMessengerWithRoot.authorizeUia() = with(root) {
    uiaStack.waitFor(UiaRouter.Wrapper.UiaActionConfirmation::class).viewModel.next()
    val uiaStepDummyViewModel = uiaStack.waitFor(UiaRouter.Wrapper.UiaStepDummy::class).viewModel
    uiaStepDummyViewModel.next()
    uiaStack.waitFor(UiaRouter.Wrapper.None::class)
}

suspend fun MatrixMessengerWithRoot.initiateUserVerification(roomId: RoomId, userId: UserId) = with(root) {
    log.info { "  +- initiate user verification (client1)" }
    withTimeout(20.seconds) {
        val roomViewModel = goToRoom(roomId)
        roomViewModel.openUserProfile(userId)
        val userProfileViewModel =
            roomViewModel.extrasStack.waitFor(ExtrasRouter.Wrapper.UserProfile::class).viewModel
        userProfileViewModel.startVerification()
        log.debug { "started user verification" }
        delay(1.seconds) // wait for request to finish
        val verificationRequest = findActiveVerification(roomViewModel)
        log.debug { "wait for verification state machine to move on" }
        val verificationViewModel =
            verificationRequest.stack.waitFor(VerificationRouter.Wrapper.Verification::class).viewModel
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Wait::class)
        log.debug { "user verification started" }
    }
}

suspend fun MatrixMessengerWithRoot.acceptUserVerification(roomId: RoomId, otherUserId: UserId) = with(root) {
    log.info { "  +- accept user verification (client2)" }
    withTimeout(20.seconds) {
        val verificationViewModel = verificationViewModel(roomId)
        val requestViewModel =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Request::class).viewModel
        requestViewModel.theirUserId shouldBe otherUserId
        requestViewModel.next()
        log.debug { "user verification accepted" }
    }
}

suspend fun MatrixMessengerWithRoot.startVerificationWithEmoji(roomId: RoomId) = with(root) {
    log.info { "  +- start verification with emoji (client1)" }
    withTimeout(20.seconds) {
        val verificationViewModel = verificationViewModel(roomId)
        val selectVerification =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.SelectVerificationMethod::class).viewModel
        selectVerification.verificationMethods.size shouldBe 1
        selectVerification.acceptVerificationMethod(selectVerification.verificationMethods[0].first)
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Wait::class)
    }
}

suspend fun MatrixMessengerWithRoot.acceptVerificationWithEmoji(roomId: RoomId) = with(root) {
    log.info { "  +- accept verification with emoji (client2)" }
    withTimeout(20.seconds) {
        val verificationViewModel = verificationViewModel(roomId)
        val acceptSasStartViewModel =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.AcceptSasStart::class).viewModel
        acceptSasStartViewModel.accept()
        val compareViewModel =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.CompareEmojisOrNumbers::class).viewModel
        compareViewModel.accept()
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Wait::class)
    }
}

suspend fun MatrixMessengerWithRoot.originalClientAcceptVerificationWithEmoji(roomId: RoomId) = with(root) {
    log.info { "  +- other client accept verification with emoji (client1)" }
    val verificationViewModel = verificationViewModel(roomId)
    val compareViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.CompareEmojisOrNumbers::class).viewModel
    compareViewModel.accept()
    val roomViewModel = goToRoom(roomId)
    val done =
        findTimelineElement<VerificationDoneTimelineElementViewModel, BaseTimelineElementHolderViewModel>(
            roomViewModel
        )
    done.message shouldBe "Erfolgreich"
}

private suspend fun RootViewModel.goToRoom(roomId: RoomId): RoomViewModel {
    log.debug { "go to room $roomId" }
    val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel.selectRoom(roomId)
    val roomViewModel = mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
    log.debug { "go to room $roomId successful" }
    return roomViewModel
}

private suspend fun RootViewModel.verificationViewModel(roomId: RoomId): VerificationViewModel {
    val roomViewModel = goToRoom(roomId)
    val verificationRequest = findActiveVerification(roomViewModel)
    val verificationViewModel =
        verificationRequest.stack.waitFor(VerificationRouter.Wrapper.Verification::class).viewModel
    return verificationViewModel
}

private suspend fun findActiveVerification(roomViewModel: RoomViewModel): VerificationRequest {
    log.debug { "find active verification" }
    val verificationRequest =
        findTimelineElement<VerificationRequest, BaseTimelineElementHolderViewModel>(roomViewModel)
    log.debug { "found active verification: $verificationRequest" }
    return verificationRequest
}

private suspend inline fun <reified H : BaseTimelineElementHolderViewModel> findTimelineElement(
    roomViewModel: RoomViewModel,
    crossinline condition: (TimelineElementViewModel<*>) -> Boolean,
) {
    val timelineViewModel = roomViewModel.timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
    timelineViewModel.elements.first { vms ->
        log.debug { "vms: $vms" }
        initViewState(timelineViewModel, vms)
        vms
            .filterIsInstance<H>()
            .any {
                val vm = it.element.filterNotNull().first()
                log.debug { "+++ vm: $vm, ${vm::class.simpleName}" }
                condition(vm)
            }
    }
}

private suspend inline fun <reified T : TimelineElementViewModel<*>, reified H : BaseTimelineElementHolderViewModel> findTimelineElement(
    roomViewModel: RoomViewModel,
): T {
    val timelineViewModel = roomViewModel.timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
    timelineViewModel.elements.first { it.isNotEmpty() }
    timelineViewModel.elements.flatMapLatest { vms ->
        initViewState(timelineViewModel, vms)
        combine(
            vms
                .filterIsInstance<H>()
                .map { it.element.filterNotNull() }) {
            it.toList()
        }
    }.first { vms ->
        log.debug { "+++ vms: $vms" }
        vms
            .any { vm ->
                log.debug { "+++ vm: ${vm::class.simpleName}" }
                vm is T
            }
    }

    return timelineViewModel.elements.map {
        it
            .filterIsInstance<H>()
            .find { vm -> vm.element.filterNotNull().first() is T }
    }.filterNotNull().first().element.filterNotNull().first() as T
}

private fun initViewState(
    timelineViewModel: TimelineViewModel,
    vms: List<BaseTimelineElementHolderViewModel>
) {
    if (timelineViewModel.viewState.value == null) {
        timelineViewModel.viewState.value = TimelineViewModel.ViewState(
            firstVisibleElement = vms.first().key,
            lastVisibleElement = vms.last().key,
            firstLoadedElement = vms.first().key,
            lastLoadedElement = vms.last().key,
            windowIsFocused = true,
        )
    }
}
