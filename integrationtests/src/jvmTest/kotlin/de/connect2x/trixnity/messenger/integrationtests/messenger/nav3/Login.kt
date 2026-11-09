package de.connect2x.trixnity.messenger.integrationtests.messenger.nav3

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.verification.SelfVerificationMethod
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.integrationtests.util.push
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.integrationtests.util.waitForNotActive
import de.connect2x.trixnity.messenger.integrationtests.util.waitForViewModel
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.internal.routes.root.AddMatrixAccountRoute
import de.connect2x.trixnity.messenger.internal.routes.root.PasswordLoginRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaActionConfirmationRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationAcceptSasStartRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationCompareEmojisOrNumbersRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRequestRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationSuccessRoute
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountMethod
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel
import io.kotest.matchers.equals.shouldBeEqual
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.messenger.nav3.LoginKt")

@OptIn(TrixnityMessengerPrivateApi::class)
suspend fun Nav3MatrixMessengerWithRoot.login(
    serverUrl: String,
    username: String,
    password: String,
    recoveryKey: String? = null,
    otherMessenger: Nav3MatrixMessengerWithRoot? = null,
): String? {
    log.debug { " +- ADD ACCOUNT" }
    log.debug { " +- try login" }
    addMatrixAccountViaPassword(serverUrl, username, password)
    log.debug { " +- do setup/cross signing" }
    val setupViewModel = waitForViewModel<AccountSetupRoute, AccountSetupViewModel>()
    val verification =
        withTimeoutOrNull(15.seconds) { waitForViewModel<CrossSigningBootstrapRoute, CrossSigningBootstrapViewModel>() }
    setupViewModel.closeAccountSetup()

    if (verification == null) {
        val mxid = UserId("@$username:localhost:8008")
        log.info { "Got no cross signing bootstrap, starting self verification" }
        push(SelfVerificationRoute(mxid))
        val selfVerificationViewModel = waitForViewModel<SelfVerificationRoute, SelfVerificationViewModel>()
        if (recoveryKey != null) {
            selfVerify(selfVerificationViewModel, recoveryKey)
            waitForNotActive<SelfVerificationRoute>()
            log.info { "self verification done successfully" }
        } else {
            if (otherMessenger != null) {
                selfVerify(selfVerificationViewModel, otherMessenger)
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

@OptIn(TrixnityMessengerPrivateApi::class)
private suspend fun Nav3MatrixMessengerWithRoot.addMatrixAccountViaPassword(
    serverUrl: String,
    username: String,
    password: String,
) {
    val addMatrixAccountViewModel = waitForViewModel<AddMatrixAccountRoute, AddMatrixAccountViewModel>()
    addMatrixAccountViewModel.serverUrl.update(serverUrl)
    val registerMethod =
        addMatrixAccountViewModel.serverDiscoveryState
            .filterIsInstance<AddMatrixAccountViewModel.ServerDiscoveryState.Success>()
            .first()
            .addMatrixAccountMethods
            .filterIsInstance<AddMatrixAccountMethod.Password>()
            .first()
    addMatrixAccountViewModel.selectAddMatrixAccountMethod(registerMethod)
    val passwordLoginViewModel = waitForViewModel<PasswordLoginRoute, PasswordLoginViewModel>()
    addMatrixAccountViewModel.serverUrl.update(serverUrl)
    passwordLoginViewModel.username.update(username)
    passwordLoginViewModel.password.update(password)
    passwordLoginViewModel.canLogin.first { it }
    passwordLoginViewModel.tryLogin()
}

@OptIn(TrixnityMessengerPrivateApi::class)
private suspend fun Nav3MatrixMessengerWithRoot.bootstrap(
    crossSigningBootstrapViewModel: CrossSigningBootstrapViewModel,
    serverUrl: String,
    username: String,
    password: String,
): String? {
    log.info { "  +- bootstrap" }
    crossSigningBootstrapViewModel.startCrossSigningBootstrap()
    waitForViewModel<UiaActionConfirmationRoute, UiaActionConfirmationViewModel>()

    crossSigningBootstrapViewModel.isBootstrapRunning.first { it.not() }
    val createdRecoveryKey = crossSigningBootstrapViewModel.recoveryKey.first { it != null }
    log.info { "user '$username' with password '$password' has recovery key '$createdRecoveryKey'" }

    crossSigningBootstrapViewModel.close()
    log.info { "   - bootstrap finished" }
    return createdRecoveryKey
}

private suspend fun selfVerify(selfVerificationViewModel: SelfVerificationViewModel, recoveryKey: String) {
    log.info { "  +- self verification with recovery key" }
    selfVerificationViewModel.availableSelfVerificationMethods.first {
        it?.any { selfVerificationMethod -> selfVerificationMethod is SelfVerificationMethod.AesHmacSha2RecoveryKey }
            ?: false
    }
    selfVerificationViewModel.recoveryKey.update(recoveryKey)
    selfVerificationViewModel.verifyWithRecoveryKey()
}

@OptIn(TrixnityMessengerPrivateApi::class)
private suspend fun Nav3MatrixMessengerWithRoot.selfVerify(
    selfVerificationViewModel: SelfVerificationViewModel,
    otherMessenger: Nav3MatrixMessengerWithRoot,
) {
    log.info { "  +- self verification with other device" }
    selfVerificationViewModel.availableSelfVerificationMethods.first {
        it?.any { selfVerificationMethod ->
            selfVerificationMethod is SelfVerificationMethod.CrossSignedDeviceVerification
        } ?: false
    }
    selfVerificationViewModel.verifyWithOtherDevice()

    val verificationViewModel = waitForViewModel<VerificationRoute, VerificationViewModel>()

    val otherVerificationViewModel = otherMessenger.waitForViewModel<VerificationRoute, VerificationViewModel>()

    otherMessenger.waitFor<VerificationRequestRoute>()
    otherVerificationViewModel.acceptRequest()

    val verificationMethod = verificationViewModel.verificationMethods.filterNotNull().first { it.size == 1 }.first()
    verificationViewModel.acceptVerificationMethod(verificationMethod.first)

    otherMessenger.waitFor<VerificationAcceptSasStartRoute>()
    otherVerificationViewModel.acceptSasVerification()

    waitFor<VerificationCompareEmojisOrNumbersRoute>()
    otherMessenger.waitFor<VerificationCompareEmojisOrNumbersRoute>()
    val ownEmojis = verificationViewModel.emojis.filterNotNull().first()
    val otherEmojis = otherVerificationViewModel.emojis.filterNotNull().first()
    ownEmojis shouldBeEqual otherEmojis

    verificationViewModel.acceptEmojisOrNumbers()
    otherVerificationViewModel.acceptEmojisOrNumbers()

    waitFor<VerificationSuccessRoute>()
    otherMessenger.waitFor<VerificationSuccessRoute>()
    verificationViewModel.ok()
    otherVerificationViewModel.ok()

    di.get<GetRouteNavigation>().routes.first { it.none { it is VerificationRouteMarker } }
    otherMessenger.di.get<GetRouteNavigation>().routes.first { it.none { it is VerificationRouteMarker } }
}
