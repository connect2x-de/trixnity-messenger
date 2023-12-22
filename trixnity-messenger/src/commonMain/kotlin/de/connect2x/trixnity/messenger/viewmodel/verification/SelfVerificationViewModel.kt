package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKey
import net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.*
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.key.RecoveryKeyInvalidException
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface SelfVerificationViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onClose: () -> Unit,
    ): SelfVerificationViewModel {
        return SelfVerificationViewModelImpl(viewModelContext, onClose)
    }

    companion object : SelfVerificationViewModelFactory
}

interface SelfVerificationViewModel {
    val userId: UserId
    val showVerificationHelp: MutableStateFlow<Boolean>
    val selfVerificationMethods: MutableStateFlow<Set<SelfVerificationMethod>>
    val showPassphraseMethod: MutableStateFlow<AesHmacSha2RecoveryKeyWithPbkdf2Passphrase?>
    val showRecoveryKeyMethod: MutableStateFlow<AesHmacSha2RecoveryKey?>
    val recoveryKeyWrong: MutableStateFlow<Boolean>
    val passphraseWrong: MutableStateFlow<Boolean>
    val error: MutableStateFlow<String?>

    fun waitForAvailableVerificationMethods()
    fun launchVerification(selfVerificationMethod: SelfVerificationMethod)
    fun verifyWithRecoveryKey(recoveryKey: String)
    fun verifyWithPassphrase(passphrase: String)
    fun backToChoose()
    fun closeMessenger()
}

open class SelfVerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onClose: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, SelfVerificationViewModel {

    private val verifyAccount = get<VerifyAccount>()

    override val showVerificationHelp = MutableStateFlow(true)
    override val selfVerificationMethods = MutableStateFlow<Set<SelfVerificationMethod>>(emptySet())
    override val showPassphraseMethod = MutableStateFlow<AesHmacSha2RecoveryKeyWithPbkdf2Passphrase?>(null)
    override val showRecoveryKeyMethod = MutableStateFlow<AesHmacSha2RecoveryKey?>(null)

    override val recoveryKeyWrong = MutableStateFlow(false)
    override val passphraseWrong = MutableStateFlow(false)

    override val error = MutableStateFlow<String?>(null)

    override fun waitForAvailableVerificationMethods() {
        coroutineScope.launch {
            matrixClients.scopedCollectLatest { matrixClients ->
                matrixClients.forEach { (userId, matrixClient) ->
                    launch {
                        log.debug { "launch self verification method listener for account $userId" }
                        matrixClient.verification.getSelfVerificationMethods()
                            .collectLatest { foundSelfVerificationMethods ->
                                showVerificationHelp.value = false

                                when (foundSelfVerificationMethods) {
                                    is PreconditionsNotMet -> {
                                        log.debug { "$userId: cannot determine yet if cross-signing is needed" }
                                    }

                                    is NoCrossSigningEnabled -> {
                                        log.debug { "$userId: no cross-signing is enabled" }
                                    }

                                    is AlreadyCrossSigned -> {
                                        log.debug { "$userId: client is already cross-signed" }
                                    }

                                    is CrossSigningEnabled -> {
                                        log.debug { "$userId: multiple self verification methods are available" }
                                        selfVerificationMethods.value = foundSelfVerificationMethods.methods
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    override fun launchVerification(selfVerificationMethod: SelfVerificationMethod) {
        log.debug { "start self verification for method: $selfVerificationMethod" }
        when (selfVerificationMethod) {
            is SelfVerificationMethod.CrossSignedDeviceVerification -> {
                coroutineScope.launch {
                    selfVerificationMethod.createDeviceVerification()
                        .onSuccess {
                            log.debug { "successfully created a device verification" }
                        }
                        .onFailure {
                            log.error(it) { "device verification failed" }
                        }
                    log.debug { "close self verification view" }
                    onClose()
                }
            }

            is AesHmacSha2RecoveryKeyWithPbkdf2Passphrase -> {
                showPassphraseMethod.value = selfVerificationMethod
            }

            is AesHmacSha2RecoveryKey -> {
                showRecoveryKeyMethod.value = selfVerificationMethod
            }
        }
    }

    override fun verifyWithRecoveryKey(recoveryKey: String) {
        error.value = null
        recoveryKeyWrong.value = false

        coroutineScope.launch {
            log.debug { "verify" }
            showRecoveryKeyMethod.value?.let { recoveryKeyMethod ->
                verifyAccount.verify(recoveryKeyMethod, recoveryKey).fold(
                    onSuccess = {
                        log.debug { "successfully verified with recovery key" }
                        onClose()
                    },
                    onFailure = {
                        if (it is RecoveryKeyInvalidException) {
                            log.debug { "recovery key is wrong: ${it.message}" }
                            recoveryKeyWrong.value = true
                        } else {
                            log.error(it) { "Cannot verify with recovery key." }
                            error.value = i18n.selfVerificationErrorMasterKey()
                        }
                    }
                )
            }
        }
    }

    override fun verifyWithPassphrase(passphrase: String) {
        error.value = null
        passphraseWrong.value = false

        coroutineScope.launch {
            showPassphraseMethod.value?.let { passphraseMethod ->
                verifyAccount.verify(passphraseMethod, passphrase).fold(
                    onSuccess = {
                        log.debug { "successfully verified with passphrase" }
                        onClose()
                    },
                    onFailure = {
                        // internally, the passphrase is used to re-create the recovery key
                        if (it is RecoveryKeyInvalidException) {
                            passphraseWrong.value = true
                        } else {
                            log.error(it) { "Cannot verify with passphrase." }
                            error.value = i18n.selfVerificationErrorMasterPassphrase()
                        }
                    }
                )
            }
        }
    }

    override fun backToChoose() {
        showPassphraseMethod.value = null
        showRecoveryKeyMethod.value = null
        waitForAvailableVerificationMethods()
    }

    override fun closeMessenger() {
        getOrNull<CloseApp>()?.invoke()
    }

}