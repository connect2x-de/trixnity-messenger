package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKey
import net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.AlreadyCrossSigned
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.CrossSigningEnabled
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.NoCrossSigningEnabled
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.PreconditionsNotMet
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.crypto.key.RecoveryKeyInvalidException
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface SelfVerificationViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onCloseSelfVerification: (Boolean) -> Unit,
        onResetRecovery: () -> Unit,
    ): SelfVerificationViewModel {
        return SelfVerificationViewModelImpl(viewModelContext, onCloseSelfVerification, onResetRecovery)
    }

    companion object : SelfVerificationViewModelFactory
}

interface SelfVerificationViewModel {
    val userId: UserId
    val showVerificationHelp: MutableStateFlow<Boolean>
    val showResetRecoveryWarning: MutableStateFlow<Boolean>
    val selfVerificationMethods: MutableStateFlow<Set<SelfVerificationMethod>>
    val showPassphraseMethod: MutableStateFlow<AesHmacSha2RecoveryKeyWithPbkdf2Passphrase?>
    val showRecoveryKeyMethod: MutableStateFlow<AesHmacSha2RecoveryKey?>
    val recoveryKeyWrong: MutableStateFlow<Boolean>
    val passphraseWrong: MutableStateFlow<Boolean>
    val error: MutableStateFlow<String?>
    val isVerified: StateFlow<Boolean?>
    val verificationMethodsLoaded: StateFlow<Boolean?>
    val isSetup: StateFlow<Boolean>

    fun waitForAvailableVerificationMethods()
    fun launchVerification(selfVerificationMethod: SelfVerificationMethod)
    fun verifyWithRecoveryKey(recoveryKey: String)
    fun verifyWithPassphrase(passphrase: String)
    fun backToChoose()
    fun backToHelp()
    fun resetRecoveryWarning()
    fun resetRecovery()
    fun closeMessenger()
    fun close()
}

open class SelfVerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseSelfVerification: (Boolean) -> Unit,
    private val onResetRecovery: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, SelfVerificationViewModel {

    private val verifyAccount = get<VerifyAccount>()

    override val showVerificationHelp = MutableStateFlow(true)
    override val showResetRecoveryWarning = MutableStateFlow(false)
    override val selfVerificationMethods = MutableStateFlow<Set<SelfVerificationMethod>>(emptySet())
    override val showPassphraseMethod = MutableStateFlow<AesHmacSha2RecoveryKeyWithPbkdf2Passphrase?>(null)
    override val showRecoveryKeyMethod = MutableStateFlow<AesHmacSha2RecoveryKey?>(null)

    override val recoveryKeyWrong = MutableStateFlow(false)
    override val passphraseWrong = MutableStateFlow(false)

    override val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isVerified: StateFlow<Boolean?> =
        matrixClients.map { it[userId] }.filterNotNull()
            .map { it.key.getTrustLevel(userId, it.deviceId).map { it.isVerified } }.flatMapLatest { it }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)


    override val isSetup =
        get<MatrixMessengerSettingsHolder>().map { it.base.accounts[userId]?.base?.accountSetupFinished == false }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(), false
            )

    @OptIn(FlowPreview::class)
    private val verificationMethods =
        matrixClient.verification.getSelfVerificationMethods()
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

    private val loadingDone = MutableStateFlow(false)
    override val verificationMethodsLoaded: StateFlow<Boolean?> =
        verificationMethods.map { verificationMethods ->
            if (loadingDone.value) true
            else if (verificationMethods !is PreconditionsNotMet) {
                loadingDone.value = true
                true
            } else false
        }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                null
            )

    override fun waitForAvailableVerificationMethods() {
        coroutineScope.launch {
            log.debug { "launch self verification method listener for account $userId" }
            verificationMethods.collectLatest { foundSelfVerificationMethods ->
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
                    onCloseSelfVerification(true)
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
                        onCloseSelfVerification(true)
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
                        onCloseSelfVerification(true)
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

    private fun resetMethods() {
        showPassphraseMethod.value = null
        showRecoveryKeyMethod.value = null
    }

    override fun backToChoose() {
        resetMethods()
        showResetRecoveryWarning.value = false
        showVerificationHelp.value = false
        waitForAvailableVerificationMethods()
    }

    override fun backToHelp() {
        resetMethods()
        showResetRecoveryWarning.value = false
        showVerificationHelp.value = true
    }

    override fun resetRecoveryWarning() {
        resetMethods()
        showVerificationHelp.value = false
        showResetRecoveryWarning.value = true
    }

    override fun resetRecovery() {
        onResetRecovery()
    }

    override fun closeMessenger() {
        getOrNull<CloseApp>()?.invoke()
    }

    override fun close() {
        onCloseSelfVerification(!showVerificationHelp.value)
    }

    private val backCallback = BackCallback(priority = 1) {
        when {
            showVerificationHelp.value -> close()
            (showResetRecoveryWarning.value || showPassphraseMethod.value != null || showRecoveryKeyMethod.value != null) -> backToChoose()

            else -> backToHelp()
        }
    }

    init {
        backHandler.register(
            backCallback
        )
    }
}
