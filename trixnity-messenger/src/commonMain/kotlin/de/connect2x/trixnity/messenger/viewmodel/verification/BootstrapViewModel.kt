package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.closeApp
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapStep.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.core.ErrorResponse
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

enum class BootstrapStep {
    EXPLANATION, RECOVERY_KEY_EXPLANATION, RECOVERY_KEY, AUTHENTICATE, FINISHED
}

interface BootstrapViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onClose: () -> Unit,
    ): BootstrapViewModel {
        return BootstrapViewModelImpl(
            viewModelContext, onClose
        )
    }

    companion object : BootstrapViewModelFactory
}

interface BootstrapViewModel {
    val accountName: String
    val showAccountName: StateFlow<Boolean>
    val step: StateFlow<BootstrapStep>
    val recoveryKey: StateFlow<String?>
    val recoveryKeyPart1: StateFlow<String?>
    val recoveryKeyPart2: StateFlow<String?>
    val generatingRecoveryKey: StateFlow<Boolean>
    val shouldAuthenticate: StateFlow<Boolean>
    val uiaAuthenticationRunning: StateFlow<Boolean>
    val username: MutableStateFlow<String>
    val password: MutableStateFlow<String>
    val canGoNext: StateFlow<Boolean>
    val passwordWrong: StateFlow<Boolean>
    val error: StateFlow<String?>

    fun next()
    fun previous()
    fun bootstrap()
    fun authenticate()
    fun close()
    fun closeMessenger()
}

open class BootstrapViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onClose: () -> Unit,
) : MatrixClientViewModelContext by viewModelContext, BootstrapViewModel {

    private val getAccountNames = get<GetAccountNames>()

    override val accountName: String = viewModelContext.accountName
    override val showAccountName: StateFlow<Boolean> = channelFlow { send(getAccountNames().isNotEmpty()) }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val step = MutableStateFlow(EXPLANATION)

    override val recoveryKey = MutableStateFlow<String?>(null)
    override val recoveryKeyPart1 = recoveryKey.map {
        it?.split(" ")?.take(6)?.joinToString(" ")
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val recoveryKeyPart2 = recoveryKey.map {
        it?.split(" ")?.drop(6)?.joinToString(" ")
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val generatingRecoveryKey = MutableStateFlow(false)

    private val uiaAuthenticate = MutableStateFlow<UIA.Step<*>?>(null)
    override val shouldAuthenticate = uiaAuthenticate.map { it != null }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val uiaAuthenticationRunning = MutableStateFlow(false)
    override val username = MutableStateFlow("")
    override val password = MutableStateFlow("")
    override val canGoNext = combine(shouldAuthenticate, username, password) { authenticate, name, pw ->
        authenticate && name.isNotBlank() && pw.isNotBlank()
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val passwordWrong = MutableStateFlow(false)

    override val error = MutableStateFlow<String?>(null)

    init {
        coroutineScope.launch {
            combine(username, password) { _, _ ->
                passwordWrong.value = false
            }.collectLatest { }
        }
    }

    override fun next() {
        step.value = BootstrapStep.values().getOrElse(step.value.ordinal + 1) { BootstrapStep.values().last() }
    }

    override fun previous() {
        step.value = BootstrapStep.values().getOrElse(step.value.ordinal - 1) { BootstrapStep.values().first() }
    }

    override fun bootstrap() {
        coroutineScope.launch {
            generatingRecoveryKey.value = true
            val bootstrap = matrixClient.key.bootstrapCrossSigning()
            bootstrap.result
                .onSuccess {
                    when (it) {
                        is UIA.Success -> {
                            log.info { "finished bootstrapping with UIA success" }
                        }

                        is UIA.Error -> {
                            log.error { "bootstrapping has failed: ${it.errorResponse}" }
                            error.value = i18n.bootstrapErrorAccount()
                        }

                        is UIA.Step -> {
                            log.info { "user has to authenticate again to finish bootstrapping" }
                            uiaAuthenticate.value = it
                        }
                    }
                }
                .onFailure {
                    log.error(it) { "calling bootstrapping API has failed" }
                    error.value = i18n.bootstrapErrorAccount()
                }
            generatingRecoveryKey.value = false
            recoveryKey.value = bootstrap.recoveryKey
            step.value = RECOVERY_KEY
        }
    }

    override fun authenticate() {
        if (shouldAuthenticate.value) {
            coroutineScope.launch {
                uiaAuthenticationRunning.value = true

                val uiaStep = uiaAuthenticate.value
                if (uiaStep == null) {
                    log.warn { "cannot authenticate user as there is no UIAStep set" }
                } else {
                    uiaStep.authenticate(
                        AuthenticationRequest.Password(
                            IdentifierType.User(username.value),
                            password.value
                        )
                    )
                        .onSuccess {
                            when (it) {
                                is UIA.Success -> {
                                    log.info { "user successfully authenticated -> finish bootstrapping" }
                                    step.value = FINISHED
                                }

                                is UIA.Error -> {
                                    if (it.errorResponse is ErrorResponse.Forbidden) {
                                        log.warn { "username / password was wrong" }
                                        passwordWrong.value = true
                                    } else {
                                        log.warn { "cannot authenticate; reason: ${it.errorResponse.error} (${it.errorResponse::class.simpleName})" }
                                    }
                                }

                                is UIA.Step -> {
                                    // TODO implement later (CAPTHA, SMS, etc.)
                                }
                            }
                        }.onFailure {
                            log.error(it) { "cannot authenticate user in bootstrapping process" }
                            error.value = i18n.bootstrapErrorLogin()
                        }

                    uiaAuthenticationRunning.value = false
                }
            }
        } else {
            // in case no authentication is needed, simply go to the next step
            next()
        }
    }

    override fun close() {
        onClose()
    }

    override fun closeMessenger() {
        closeApp()
    }
}