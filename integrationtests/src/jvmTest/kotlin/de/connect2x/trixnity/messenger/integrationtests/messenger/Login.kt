package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

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
        return bootstrap(verification, serverUrl, username, password)
    }
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
    serverUrl: String,
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
