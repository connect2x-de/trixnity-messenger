package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.StoreAccessException
import de.connect2x.trixnity.messenger.StoreLockedException
import de.connect2x.trixnity.messenger.deviceDisplayName
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.util.network.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId

private val log = KotlinLogging.logger { }

fun ViewModelContext.login(
    matrixClientService: MatrixClientService,
    accountName: String,
    serverUrl: String,
    username: String,
    password: String,
    loginState: MutableStateFlow<AddMatrixAccountViewModel.LoginState>,
    onLogin: () -> Unit,
) {
    log.info { "try to login" }
    loginState.value = AddMatrixAccountViewModel.LoginState.Connecting

    coroutineScope.launch {
        val errorMessage = try {
            matrixClientService.login(
                baseUrl = Url(serverUrl),
                identifier = IdentifierType.User(username),
                password = password,
                initialDeviceDisplayName = deviceDisplayName(),
                accountName = accountName,
            ).getOrThrow()
            log.info { "login success" }
            null
        } catch (exc: MatrixServerException) {
            log.error(exc) { "Cannot contact Matrix Server." }
            val errorResponse = exc.errorResponse
            if (errorResponse is ErrorResponse.LimitExceeded && errorResponse.retryAfterMillis < 5_000) {
                delay(errorResponse.retryAfterMillis)
                login(matrixClientService, accountName, serverUrl, username, password, loginState, onLogin)
                return@launch // since the recursive call has already set the errorMessage
            } else {
                when (exc.statusCode) {
                    HttpStatusCode.Forbidden -> i18n.connectingErrorForbidden()
                    HttpStatusCode.NotFound -> i18n.connectingErrorNotFound()
                    else -> i18n.connectingErrorStandard()
                }
            }
        } catch (exc: CancellationException) {
            // do nothing as this is the case when the view model is removed
            null
        } catch (exc: StoreLockedException) {
            log.error(exc) { "database is locked" }
            i18n.connectingErrorDbLocked()
        } catch (exc: StoreAccessException) {
            log.error(exc) { "cannot access database; this is a serious problem and might only be solved by deleting the database if the problem persists" }
            // we cannot load data from the DB, so either close the App or remove the DB and try again
            i18n.connectingErrorDbAccess()
        } catch (exc: Exception) {
            log.error(exc) { "Cannot contact Matrix Server." }
            when (exc) {
                is UnresolvedAddressException, is IllegalArgumentException ->
                    i18n.connectingErrorWrongAddress()

                is IOException -> {
                    handleIoException(exc)
                }

                else -> handleCause(exc.cause)
            }
        }

        if (errorMessage == null) {
            loginState.value = AddMatrixAccountViewModel.LoginState.Success
            log.info {"call onLogin()"}
            onLogin()
        } else {
            loginState.value = AddMatrixAccountViewModel.LoginState.Failure(errorMessage)
        }
    }
}

fun ViewModelContext.loginWith(
    matrixClientService: MatrixClientService,
    accountName: String,
    serverUrl: String,
    userId: UserId,
    deviceId: String,
    accessToken: String,
    displayName: String?,
    avatarUrl: String? = null,
    loginState: MutableStateFlow<AddMatrixAccountViewModel.LoginState>,
    onLogin: () -> Unit,
) {
    log.info { "try to loginWith" }
    loginState.value = AddMatrixAccountViewModel.LoginState.Connecting

    coroutineScope.launch {
        val errorMessage = try {
            matrixClientService.loginWith(
                Url(serverUrl), userId, deviceId, accessToken, displayName, avatarUrl, accountName
            ).getOrThrow()
            log.info { "login success" }
            null
        } catch (exc: MatrixServerException) {
            log.error(exc) { "Cannot contact Matrix Server." }
            when (exc.statusCode) {
                HttpStatusCode.Forbidden -> i18n.connectingErrorForbidden()
                HttpStatusCode.NotFound -> i18n.connectingErrorNotFound()
                else -> i18n.connectingErrorStandard()
            }
        } catch (exc: CancellationException) {
            // do nothing as this is the case when the view model is removed
            null
        } catch (exc: StoreLockedException) {
            log.error(exc) { "database is locked" }
            i18n.connectingErrorDbLocked()
        } catch (exc: StoreAccessException) {
            log.error(exc) { "cannot access database; this is a serious problem and might only be solved by deleting the database if the problem persists" }
            // we cannot load data from the DB, so either close the App or remove the DB and try again
            i18n.connectingErrorDbAccess()
        } catch (exc: Exception) {
            log.error(exc) { "Cannot contact Matrix Server." }
            when (exc) {
                is UnresolvedAddressException, is IllegalArgumentException ->
                    i18n.connectingErrorWrongAddress()

                is IOException -> {
                    handleIoException(exc)
                }

                else -> handleCause(exc.cause)
            }
        }

        if (errorMessage == null) {
            loginState.value = AddMatrixAccountViewModel.LoginState.Success
            onLogin()
        } else {
            loginState.value = AddMatrixAccountViewModel.LoginState.Failure(errorMessage)
        }
    }
}

// HACK to circumvent https://youtrack.jetbrains.com/issue/KTOR-1372
private suspend fun ViewModelContext.handleIoException(exc: Exception): String {
    log.error { exc }
    return if (exc.message == "Connection refused" ||
        exc.message?.startsWith("Failed to connect") == true ||
        exc.message == "Verbindungsaufbau abgelehnt"
    ) {
        i18n.connectingErrorStandard()
    } else if (exc.message?.startsWith("Cleartext HTTP traffic") == true) {
        i18n.connectingErrorHttps()
    } else {
        handleCause(exc.cause)
    }
}

private suspend fun ViewModelContext.handleCause(exc: Throwable?) = if (exc != null) {
    when (exc) {
        is UnresolvedAddressException, is IllegalArgumentException ->
            i18n.connectingErrorWrongAddress()

        is IOException -> {
            handleIoException(exc)
        }

        else -> i18n.connectingErrorStandard()
    }
} else {
    log.error { exc }
    i18n.connectingErrorStandard()
}