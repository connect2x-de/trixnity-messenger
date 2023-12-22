package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.AccountAlreadyExistsException
import de.connect2x.trixnity.messenger.LoadStoreException.StoreAccessException
import de.connect2x.trixnity.messenger.LoadStoreException.StoreLockedException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.i18n.I18n
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.util.network.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.MatrixClient.LoginInfo
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.MatrixServerException

private val log = KotlinLogging.logger { }

suspend fun MatrixClients.loginCatching(
    serverUrl: String,
    username: String,
    password: String,
    initialDeviceDisplayName: String,
    addMatrixAccountState: MutableStateFlow<AddMatrixAccountState>,
    i18n: I18n,
    onLogin: () -> Unit,
) {
    log.info { "try to login" }
    catchLogin(addMatrixAccountState, i18n, onLogin) {
        login(
            baseUrl = Url(serverUrl),
            identifier = IdentifierType.User(username),
            password = password,
            initialDeviceDisplayName = initialDeviceDisplayName,
        )
    }
}

suspend fun MatrixClients.loginCatching(
    serverUrl: String,
    token: String,
    initialDeviceDisplayName: String,
    addMatrixAccountState: MutableStateFlow<AddMatrixAccountState>,
    i18n: I18n,
    onLogin: () -> Unit,
) {
    log.info { "try to login" }
    catchLogin(addMatrixAccountState, i18n, onLogin) {
        login(
            baseUrl = Url(serverUrl),
            token = token,
            initialDeviceDisplayName = initialDeviceDisplayName,
        )
    }
}

suspend fun MatrixClients.loginWithCatching(
    baseUrl: String,
    loginInfo: LoginInfo,
    addMatrixAccountState: MutableStateFlow<AddMatrixAccountState>,
    i18n: I18n,
    onLogin: () -> Unit,
) {
    log.info { "try to loginWith" }
    catchLogin(addMatrixAccountState, i18n, onLogin) {
        loginWith(
            baseUrl = Url(baseUrl),
            loginInfo = loginInfo,
        )
    }
}

private suspend fun catchLogin(
    addMatrixAccountState: MutableStateFlow<AddMatrixAccountState>,
    i18n: I18n,
    onLogin: () -> Unit,
    block: suspend () -> Result<Unit>
) {
    addMatrixAccountState.value = AddMatrixAccountState.Connecting
    val errorMessage = try {
        block().getOrThrow()
        log.info { "login success" }
        null
    } catch (exc: MatrixServerException) {
        log.error(exc) { "Cannot contact Matrix Server." }
        when (exc.statusCode) {
            HttpStatusCode.Forbidden -> i18n.connectingErrorForbidden()
            HttpStatusCode.NotFound -> i18n.connectingErrorNotFound()
            else -> i18n.connectingErrorStandard()
        }
    } catch (exc: AccountAlreadyExistsException) {
        log.warn { "account already exists locally" }
        i18n.connectingAccountAlreadyExists(exc.userId)
    } catch (exc: StoreLockedException) {
        log.error(exc) { "database is locked" }
        i18n.connectingErrorDbLocked()
    } catch (exc: StoreAccessException) {
        log.error(exc) { "cannot access database; this is a serious problem and might only be solved by deleting the database if the problem persists" }
        // we cannot load data from the DB, so either close the App or remove the DB and try again
        i18n.connectingErrorDbAccess()
    } catch (exc: CancellationException) {
        // do nothing as this is the case when the view model is removed
        null
    } catch (exc: Exception) {
        log.error(exc) { "Cannot contact Matrix Server." }
        when (exc) {
            is UnresolvedAddressException, is IllegalArgumentException ->
                i18n.connectingErrorWrongAddress()

            is IOException -> {
                handleIoException(i18n, exc)
            }

            else -> handleCause(i18n, exc.cause)
        }
    }

    if (errorMessage == null) {
        addMatrixAccountState.value = AddMatrixAccountState.Success
        onLogin()
    } else {
        addMatrixAccountState.value = AddMatrixAccountState.Failure(errorMessage)
    }
}

// HACK to circumvent https://youtrack.jetbrains.com/issue/KTOR-1372
private suspend fun handleIoException(i18n: I18n, exc: Exception): String {
    log.error { exc }
    return if (exc.message == "Connection refused" ||
        exc.message?.startsWith("Failed to connect") == true ||
        exc.message == "Verbindungsaufbau abgelehnt"
    ) {
        i18n.connectingErrorStandard()
    } else if (exc.message?.startsWith("Cleartext HTTP traffic") == true) {
        i18n.connectingErrorHttps()
    } else {
        handleCause(i18n, exc.cause)
    }
}

private suspend fun handleCause(i18n: I18n, exc: Throwable?) = if (exc != null) {
    when (exc) {
        is UnresolvedAddressException, is IllegalArgumentException ->
            i18n.connectingErrorWrongAddress()

        is IOException -> {
            handleIoException(i18n, exc)
        }

        else -> i18n.connectingErrorStandard()
    }
} else {
    log.error { exc }
    i18n.connectingErrorStandard()
}