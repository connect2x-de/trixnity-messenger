package de.connect2x.trixnity.messenger.integrationtests.messenger

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.DefaultMatrixClientService
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.integrationtests.util.newDatabase
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsOverviewViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapStep
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.store.repository.exposed.createExposedRepositoriesModule
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import org.koin.core.KoinApplication
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

fun createMessenger(koinApplication: KoinApplication): RootViewModelImpl {
    val defaultMatrixClientService1 = createDefaultMatrixClientService(koinApplication)
    return createRootViewModel(koinApplication, defaultMatrixClientService1)
}

suspend fun RootViewModelImpl.login(
    serverUrl: String,
    username: String,
    password: String,
    recoveryKey: String? = null,
    otherMessenger: RootViewModel? = null,
): String? {
    log.debug { " +- ADD ACCOUNT" }
    addMatrixAccount(serverUrl, username, password)
    log.debug { " +- try login" }
    val main = rootStack.waitFor(RootRouter.RootWrapper.Main::class)
    log.info { " +- main view" }
    val mainViewModel = main.mainViewModel
    val verification = mainViewModel.selfVerificationStack.toFlow().first { childStack ->
        log.debug { " active: ${childStack.active.instance}" }
        childStack.active.instance is MainViewModel.SelfVerificationWrapper.Bootstrap ||
                childStack.active.instance is MainViewModel.SelfVerificationWrapper.View
    }.active.instance
    if (verification is MainViewModel.SelfVerificationWrapper.View) {
        if (recoveryKey != null) {
            selfVerify(verification, recoveryKey)
            mainViewModel.selfVerificationStack.waitFor(MainViewModel.SelfVerificationWrapper.None::class)
            log.info { "self verification done successfully" }
        } else {
            if (otherMessenger != null) {
                selfVerify(verification, mainViewModel, otherMessenger)
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

suspend fun RootViewModelImpl.createNewAccount(
    serverUrl: String,
    username: String,
    password: String,
    recoveryKey: String? = null,
): String? {
    val accountsOverviewViewModel = openAccountsOverview()
    accountsOverviewViewModel.createNewAccount()
    val thisRecoveryKey = login(serverUrl, username, password, recoveryKey)
    val mainViewModel = rootStack.waitFor(RootRouter.RootWrapper.Main::class).mainViewModel
    mainViewModel.roomListRouterStack.waitFor(RoomListRouter.RoomListWrapper.List::class)
    mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.InitialSyncWrapper.None::class)
    return thisRecoveryKey ?: recoveryKey
}

suspend fun RootViewModelImpl.deleteAccount(username: String) {
    val accountsOverviewViewModel = openAccountsOverview()
    accountsOverviewViewModel.removeAccount(username)
    rootStack.waitFor(RootRouter.RootWrapper.MatrixClientLogout::class)
    val mainViewModel = rootStack.waitFor(RootRouter.RootWrapper.Main::class).mainViewModel
    mainViewModel.roomListRouterStack.waitFor(RoomListRouter.RoomListWrapper.List::class)
    mainViewModel.initialSyncStack.waitFor(InitialSyncRouter.InitialSyncWrapper.None::class)
}

suspend fun RootViewModelImpl.verifyAccountsArePresent(vararg accountNames: String) {
    val accountsOverviewViewModel = openAccountsOverview()
    withTimeout(5.seconds) {
        accountsOverviewViewModel.accountNames.first {
            log.debug { "found: ${it.joinToString { it }}, expected: ${accountNames.joinToString { it }}" }
            it.containsAll(accountNames.toList()) && accountNames.toList().containsAll(it)
        }
    }
    accountsOverviewViewModel.close()
    rootStack.waitFor(RootRouter.RootWrapper.Main::class).mainViewModel
        .roomListRouterStack.waitFor(RoomListRouter.RoomListWrapper.List::class)
}

private suspend fun RootViewModelImpl.openAccountsOverview(): AccountsOverviewViewModel {
    val mainViewModel = rootStack.waitFor(RootRouter.RootWrapper.Main::class).mainViewModel
    val roomListViewModel =
        mainViewModel.roomListRouterStack.waitFor(RoomListRouter.RoomListWrapper.List::class).roomListViewModel
    roomListViewModel.openAccountsOverview()
    return mainViewModel.roomListRouterStack.waitFor(RoomListRouter.RoomListWrapper.AccountsOverview::class).accountsOverviewViewModel
}

private fun createDefaultMatrixClientService(koinApplication: KoinApplication) =
    DefaultMatrixClientService(
        context = null,
        di = koinApplication.koin,
        repositoriesModuleCreation = { accountName: String -> createExposedRepositoriesModule(newDatabase(accountName)) },
        mediaStoreCreation = { InMemoryMediaStore() },
    )

private fun createRootViewModel(koinApplication: KoinApplication, matrixClientService: MatrixClientService) =
    RootViewModelImpl(
        DefaultComponentContext(LifecycleRegistry()),
        matrixClientService = matrixClientService,
        initialSyncOnceIsFinished = {},
        koinApplication = koinApplication,
        minimizeMessenger = {},
        coroutineContext = Dispatchers.Default,
    )

private suspend fun RootViewModelImpl.addMatrixAccount(
    serverUrl: String,
    username: String,
    password: String,
) {
    val addMatrixAccount = rootStack.waitFor(RootRouter.RootWrapper.AddMatrixAccount::class)
    val addMatrixAccountViewModel = addMatrixAccount.addMatrixAccountViewModel
    addMatrixAccountViewModel.mode.value = AddMatrixAccountViewModel.Mode.USERNAME
    addMatrixAccountViewModel.accountName.value = username
    addMatrixAccountViewModel.serverUrl.value = serverUrl
    addMatrixAccountViewModel.username.value = username
    addMatrixAccountViewModel.password.value = password
    addMatrixAccountViewModel.canLogin.first { it }
    addMatrixAccountViewModel.tryLogin()
}

private suspend fun bootstrap(
    verification: MainViewModel.SelfVerificationWrapper,
    username: String,
    password: String,
): String? {
    log.info { "  +- bootstrap" }
    val bootstrapViewModel =
        (verification as MainViewModel.SelfVerificationWrapper.Bootstrap).bootstrapViewModel
    val step = bootstrapViewModel.step
    val shouldAuthenticate = bootstrapViewModel.shouldAuthenticate
    bootstrapViewModel.bootstrap()
    bootstrapViewModel.generatingRecoveryKey.first { it.not() }
    val createdRecoveryKey = bootstrapViewModel.recoveryKey.first { it != null }
    log.info { "user '$username' with password '$password' has recovery key '$createdRecoveryKey'" }
    shouldAuthenticate.first { it }
    bootstrapViewModel.username.value = username
    bootstrapViewModel.password.value = password
    bootstrapViewModel.authenticate()
    step.first { it == BootstrapStep.FINISHED }
    bootstrapViewModel.close()
    log.info { "   - bootstrap finished" }

    return createdRecoveryKey
}

private suspend fun selfVerify(
    verification: MainViewModel.SelfVerificationWrapper,
    recoveryKey: String,
) {
    log.info { "  +- self verification with recovery key" }
    val selfVerificationViewModel =
        (verification as MainViewModel.SelfVerificationWrapper.View).selfVerificationViewModel
    selfVerificationViewModel.waitForAvailableVerificationMethods()
    selfVerificationViewModel.selfVerificationMethods.first { it.any { selfVerificationMethod -> selfVerificationMethod is SelfVerificationMethod.AesHmacSha2RecoveryKey } }
    selfVerificationViewModel.launchVerification(
        selfVerificationViewModel.selfVerificationMethods.value.find {
            it is SelfVerificationMethod.AesHmacSha2RecoveryKey
        } ?: throw IllegalStateException("Can only use recovery key method"))
    selfVerificationViewModel.verifyWithRecoveryKey(recoveryKey)
}

private suspend fun selfVerify(
    verification: MainViewModel.SelfVerificationWrapper,
    mainViewModel: MainViewModel,
    otherMessenger: RootViewModel,
) {
    log.info { "  +- self verification with other device" }
    val selfVerificationViewModel =
        (verification as MainViewModel.SelfVerificationWrapper.View).selfVerificationViewModel
    selfVerificationViewModel.waitForAvailableVerificationMethods()
    selfVerificationViewModel.selfVerificationMethods.first { it.any { selfVerificationMethod -> selfVerificationMethod is SelfVerificationMethod.CrossSignedDeviceVerification } }
    selfVerificationViewModel.launchVerification(selfVerificationViewModel.selfVerificationMethods.value.find {
        it is SelfVerificationMethod.CrossSignedDeviceVerification
    } ?: throw IllegalStateException("can only use device verification"))

    mainViewModel.selfVerificationStack.waitFor(MainViewModel.SelfVerificationWrapper.None::class)
    val verificationViewModel =
        mainViewModel.deviceVerificationRouterStack.waitFor(VerificationRouter.VerificationWrapper.Verification::class).verificationViewModel
    verificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.Wait::class)

    val otherMainViewModel = otherMessenger.rootStack.waitFor(RootRouter.RootWrapper.Main::class).mainViewModel
    val otherVerificationViewModel =
        otherMainViewModel.deviceVerificationRouterStack.waitFor(VerificationRouter.VerificationWrapper.Verification::class).verificationViewModel
    val otherVerificationStepRequestViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.Request::class).verificationStepRequestViewModel
    otherVerificationStepRequestViewModel.next()

    val selectVerificationMethodViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.SelectVerificationMethod::class).selectVerificationMethodViewModel
    selectVerificationMethodViewModel.verificationMethods.size shouldBe 1
    selectVerificationMethodViewModel.acceptVerificationMethod(selectVerificationMethodViewModel.verificationMethods[0].first)

    val otherAcceptSasStartViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.AcceptSasStart::class).acceptSasStartViewModel
    otherAcceptSasStartViewModel.accept()

    val verificationStepCompareViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.CompareEmojisOrNumbers::class).verificationStepCompareViewModel
    val otherVerificationStepCompareViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.CompareEmojisOrNumbers::class).verificationStepCompareViewModel
    verificationStepCompareViewModel.emojis shouldBeEqual otherVerificationStepCompareViewModel.emojis

    verificationStepCompareViewModel.accept()
    otherVerificationStepCompareViewModel.accept()

    val verificationStepSuccessViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.Success::class).verificationStepSuccessViewModel
    val otherVerificationStepSuccessViewModel =
        otherVerificationViewModel.stack.waitFor(VerificationViewModel.VerificationStepWrapper.Success::class).verificationStepSuccessViewModel
    verificationStepSuccessViewModel.ok()
    otherVerificationStepSuccessViewModel.ok()

    mainViewModel.deviceVerificationRouterStack.toFlow()
        .first { it.active.configuration is VerificationRouter.Config.None }
    otherMainViewModel.deviceVerificationRouterStack.toFlow()
        .first { it.active.configuration is VerificationRouter.Config.None }
}
