package de.connect2x.trixnity.messenger.viewmodel.verification.v2

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.key
import de.connect2x.trixnity.client.verification
import de.connect2x.trixnity.client.verification.SelfVerificationMethod
import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods.AlreadyCrossSigned
import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods.CrossSigningEnabled
import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods.NoCrossSigningEnabled
import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods.PreconditionsNotMet
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.crypto.key.RecoveryKeyInvalidException
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.VerifyAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.get

interface SelfVerificationViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onCloseSelfVerification: () -> Unit,
        onResetRecovery: () -> Unit,
    ): SelfVerificationViewModel {
        return SelfVerificationViewModelImpl(viewModelContext, onCloseSelfVerification, onResetRecovery)
    }

    companion object : SelfVerificationViewModelFactory
}

/** A concise UI for verification of the current device. Will continue if device is already verified. */
interface SelfVerificationViewModel {
    val userId: UserId

    /**
     * After the initial sync is complete, this returns a non-`null` value: the available self-verification methods like
     * recovery key, etc.
     */
    val availableSelfVerificationMethods: StateFlow<List<SelfVerificationMethod>?>

    /** The entered recovery key. */
    val recoveryKey: TextFieldViewModel

    /** If `true`, the recovery key is currently being verified. */
    val recoveryKeyVerificationInProgress: StateFlow<Boolean>

    /** If `true`, the entered recovery key was wrong. */
    val recoveryKeyWrong: StateFlow<Boolean>

    /** The entered passphrase. */
    val passphrase: TextFieldViewModel

    /** If `true`, the passphrase is currently being verified. */
    val passphraseVerificationInProgress: StateFlow<Boolean>

    /** If `true`, the entered passphrase was wrong. */
    val passphraseWrong: StateFlow<Boolean>

    /** `null` if no error occurred, else the error message. */
    val error: StateFlow<String?>

    /** Use the recovery key to verify the current device. */
    fun verifyWithRecoveryKey()

    /** Use the passphrase to verify the current device. */
    fun verifyWithPassphrase()

    /** Verify with another device (by comparing numbers or emojis, etc.). */
    fun verifyWithOtherDevice()

    /** **Important**: used to reset the recovery key or passphrase. All encrypted messages from before are lost. */
    fun resetRecovery()

    /**
     * No device verification is performed now. It can be initiated later, but encrypted messages cannot be decrypted
     * for now.
     */
    fun continueWithoutVerification()
}

class SelfVerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseSelfVerification: () -> Unit,
    private val onResetRecovery: () -> Unit,
) : SelfVerificationViewModel, MatrixClientViewModelContext by viewModelContext {

    private val verifyMutex = Mutex()
    private val verifyAccount = get<VerifyAccount>()

    private val verificationMethods =
        matrixClient.verification
            .getSelfVerificationMethods()
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val isVerified: StateFlow<Boolean?> =
        matrixClients
            .mapNotNull { it[userId] }
            .map { it.key.getTrustLevel(userId, it.deviceId).map { it.isVerified } }
            .flatMapLatest { it }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val availableSelfVerificationMethods =
        verificationMethods
            .mapLatest { foundSelfVerificationMethods ->
                when (foundSelfVerificationMethods) {
                    is PreconditionsNotMet -> {
                        log.debug { "$userId: cannot determine yet if cross-signing is needed" }
                        emptyList()
                    }

                    is NoCrossSigningEnabled -> {
                        log.debug { "$userId: no cross-signing is enabled" }
                        emptyList()
                    }

                    is AlreadyCrossSigned -> {
                        log.debug { "$userId: client is already cross-signed" }
                        emptyList()
                    }

                    is CrossSigningEnabled -> {
                        log.debug { "$userId: multiple self verification methods are available" }

                        foundSelfVerificationMethods.methods.sortedBy {
                            when (it) {
                                is SelfVerificationMethod.AesHmacSha2RecoveryKey -> 0
                                is SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase -> 1
                                is SelfVerificationMethod.CrossSignedDeviceVerification -> 2
                            }
                        }
                    }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val recoveryKey: TextFieldViewModel = TextFieldViewModelImpl(maxLength = 60)
    override val recoveryKeyVerificationInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val recoveryKeyWrong: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val passphrase: TextFieldViewModel = TextFieldViewModelImpl(maxLength = 100)
    override val passphraseVerificationInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val passphraseWrong: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    init {
        coroutineScope.launch {
            isVerified.collect {
                if (it == true) {
                    close()
                }
            }
        }
        coroutineScope.launch {
            recoveryKey.collect {
                recoveryKeyWrong.value = false
                error.value = null
            }
        }
        coroutineScope.launch {
            passphrase.collect {
                passphraseWrong.value = false
                error.value = null
            }
        }
    }

    override fun verifyWithRecoveryKey() {
        coroutineScope.launch {
            verifyMutex.withLock {
                recoveryKeyVerificationInProgress.value = true
                log.debug { "verify with recovery key" }
                availableSelfVerificationMethods.value
                    ?.filterIsInstance<SelfVerificationMethod.AesHmacSha2RecoveryKey>()
                    ?.firstOrNull()
                    ?.let { recoveryKeyMethod ->
                        verifyAccount
                            .verify(recoveryKeyMethod, recoveryKey.textValue)
                            .fold(
                                onSuccess = {
                                    log.debug { "successfully verified with recovery key" }
                                    close()
                                },
                                onFailure = {
                                    if (it is RecoveryKeyInvalidException) {
                                        log.debug { "recovery key is wrong: ${it.message}" }
                                        recoveryKeyWrong.value = true
                                    } else {
                                        log.error(it) { "Cannot verify with recovery key." }
                                        error.value = i18n.selfVerificationErrorMasterKey()
                                    }
                                },
                            )
                    }
                    ?: run {
                        log.error {
                            "cannot initiate recovery key verification as it is not part of the available self verification methods"
                        }
                    }
                recoveryKeyVerificationInProgress.value = false
            }
        }
    }

    override fun verifyWithPassphrase() {
        coroutineScope.launch {
            verifyMutex.withLock {
                passphraseVerificationInProgress.value = true
                availableSelfVerificationMethods.value
                    ?.filterIsInstance<SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase>()
                    ?.firstOrNull()
                    ?.let { passphraseMethod ->
                        verifyAccount
                            .verify(passphraseMethod, passphrase.textValue)
                            .fold(
                                onSuccess = {
                                    log.debug { "successfully verified with passphrase" }
                                    close()
                                },
                                onFailure = {
                                    // internally, the passphrase is used to re-create the recovery key
                                    if (it is RecoveryKeyInvalidException) {
                                        passphraseWrong.value = true
                                    } else {
                                        log.error(it) { "Cannot verify with passphrase." }
                                        error.value = i18n.selfVerificationErrorMasterPassphrase()
                                    }
                                },
                            )
                    }
                    ?: run {
                        log.error {
                            "cannot initiate password verification as it is not part of the available self verification methods"
                        }
                    }
                passphraseVerificationInProgress.value = false
            }
        }
    }

    override fun verifyWithOtherDevice() {
        coroutineScope.launch {
            availableSelfVerificationMethods.value
                ?.filterIsInstance<SelfVerificationMethod.CrossSignedDeviceVerification>()
                ?.firstOrNull()
                ?.createDeviceVerification()
                ?.onSuccess { log.debug { "successfully created a device verification" } }
                ?.onFailure { log.error(it) { "device verification failed" } }
                ?: run {
                    log.error {
                        "cannot initiate cross signed device verification as it is not part of the available self verification methods"
                    }
                }
            log.debug { "close self verification view" }
            close()
        }
    }

    override fun resetRecovery() {
        onResetRecovery()
    }

    override fun continueWithoutVerification() {
        close()
    }

    private fun close() {
        onCloseSelfVerification()
    }
}
