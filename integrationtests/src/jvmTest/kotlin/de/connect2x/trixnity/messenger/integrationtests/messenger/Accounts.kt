package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsOverviewViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaRouter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

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

private suspend fun RootViewModel.openAccountsOverview(): AccountsOverviewViewModel {
    val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    val roomListViewModel =
        mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
    roomListViewModel.openAccountsOverview()
    return mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.AccountsOverview::class).viewModel
}
