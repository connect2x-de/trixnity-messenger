package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaRouter
import io.kotest.assertions.nondeterministic.eventually
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.messenger.AccountsKt")

suspend fun MatrixMessengerWithRoot.createNewAccount(
    serverUrl: String,
    username: String,
    password: String,
    recoveryKey: String? = null,
): String? =
    with(root) {
        val accountsView = viewAccounts()
        accountsView.createNewAccount()
        val thisRecoveryKey = login(serverUrl, username, password, recoveryKey)
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
        mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.Wrapper.None::class)
        return thisRecoveryKey ?: recoveryKey
    }

suspend fun MatrixMessengerWithRoot.deleteAccount(username: String) =
    with(root) {
        val accountsView = viewAccounts()
        val userId = di.get<MatrixClients>().value.keys.find { it.localpart == username }
        checkNotNull(userId)
        accountsView.removeAccount(userId)
        stack.waitFor(RootRouter.Wrapper.RemoveMatrixAccount::class)
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class)
        mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.Wrapper.None::class)
        log.debug { " +- delete account finished" }
    }

suspend fun MatrixMessengerWithRoot.verifyAccountsArePresent(vararg usernames: String) =
    with(root) {
        val accountsView = viewAccounts()
        withTimeout(5.seconds) {
            eventually(4.seconds) {
                accountsView.accountSingleViewModels.value.map { it.userId.localpart }.containsAll(usernames.toList())
            }
            accountsView.close()
            stack
                .waitFor(RootRouter.Wrapper.Main::class)
                .viewModel
                .roomListRouterStack
                .waitFor(RoomListRouter.Wrapper.UserSettings::class)
                .viewModel
                .closeUserSettings()
            stack
                .waitFor(RootRouter.Wrapper.Main::class)
                .viewModel
                .roomListRouterStack
                .waitFor(RoomListRouter.Wrapper.List::class)
                .viewModel
        }
    }

suspend fun MatrixMessengerWithRoot.registerAccountWithToken(serverUrl: String, registrationToken: String) =
    with(root) {
        withTimeout(10.seconds) {
            val addMatrixAccountViewModel = stack.waitFor(RootRouter.Wrapper.AddMatrixAccount::class).viewModel
            addMatrixAccountViewModel.serverUrl.update(serverUrl)
            val registerMethod =
                addMatrixAccountViewModel.serverDiscoveryState
                    .filterIsInstance<AddMatrixAccountViewModel.ServerDiscoveryState.Success>()
                    .first()
                    .addMatrixAccountMethods
                    .filterIsInstance<AddMatrixAccountMethod.Register>()
                    .first()
            addMatrixAccountViewModel.selectAddMatrixAccountMethod(registerMethod)
            val registerMatrixAccountViewModel =
                stack.waitFor(RootRouter.Wrapper.RegisterMatrixAccount::class).viewModel
            registerMatrixAccountViewModel.username.update("user1")
            registerMatrixAccountViewModel.password.update("user1password")
            registerMatrixAccountViewModel.canRegisterNewUser.first { it }
            registerMatrixAccountViewModel.register()
            authorizeUia(registrationToken)

            val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
            mainViewModel.roomListRouterStack
                .waitFor(RoomListRouter.Wrapper.List::class)
                .viewModel
                .accountViewModel
                .accounts
                .first { it.isNotEmpty() }
            mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.Wrapper.None::class)
        }
    }

suspend fun MatrixMessengerWithRoot.logout(userId: UserId) =
    with(root) {
        withTimeout(20.seconds) {
            val accountsView = viewAccounts()
            accountsView.removeAccount(userId)
            stack.waitFor(RootRouter.Wrapper.RemoveMatrixAccount::class)
            // since we do not have multiple accounts here, we have to wait for this view and not the MainView
            // (the last account was deleted, so we expect the user to log in with a new account)
            stack.waitFor(RootRouter.Wrapper.AddMatrixAccount::class)
            log.debug { " +- logout finished" }
        }
    }

suspend fun MatrixMessengerWithRoot.authorizeUia(username: String, password: String) =
    with(root) {
        uiaStack.waitFor(UiaRouter.Wrapper.UiaActionConfirmation::class).viewModel.next()
        val uiaStepPasswordViewModel = uiaStack.waitFor(UiaRouter.Wrapper.UiaStepPassword::class).viewModel
        uiaStepPasswordViewModel.username.update(username)
        uiaStepPasswordViewModel.password.update(password)
        uiaStepPasswordViewModel.submit()
        uiaStack.waitFor(UiaRouter.Wrapper.None::class)
    }

suspend fun MatrixMessengerWithRoot.authorizeUia(registrationToken: String) =
    with(root) {
        uiaStack.waitFor(UiaRouter.Wrapper.UiaActionConfirmation::class).viewModel.next()
        val uiaRegistrationTokenViewModel =
            uiaStack.waitFor(UiaRouter.Wrapper.UiaStepRegistrationToken::class).viewModel
        uiaRegistrationTokenViewModel.registrationToken.update(registrationToken)
        uiaRegistrationTokenViewModel.submit()
        uiaStack.waitFor(UiaRouter.Wrapper.None::class)
    }

suspend fun MatrixMessengerWithRoot.authorizeUia() =
    with(root) {
        uiaStack.waitFor(UiaRouter.Wrapper.UiaActionConfirmation::class).viewModel.next()
        val uiaStepDummyViewModel = uiaStack.waitFor(UiaRouter.Wrapper.UiaStepDummy::class).viewModel
        uiaStepDummyViewModel.next()
        uiaStack.waitFor(UiaRouter.Wrapper.None::class)
    }

private suspend fun RootViewModel.viewAccounts(): AccountsViewModel {
    val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    mainViewModel.roomListRouterStack
        .waitFor(RoomListRouter.Wrapper.List::class)
        .viewModel
        .accountViewModel
        .openUserSettings()
    mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.UserSettings::class).viewModel.showAccounts()
    return mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.Accounts::class).viewModel
}

private suspend fun AccountsViewModel.removeAccount(userId: UserId) {
    delay(1.seconds) // otherwise `accountSingleViewModels.value` may be null
    accountSingleViewModels.value.find { it.userId == userId }!!.logout()
}
