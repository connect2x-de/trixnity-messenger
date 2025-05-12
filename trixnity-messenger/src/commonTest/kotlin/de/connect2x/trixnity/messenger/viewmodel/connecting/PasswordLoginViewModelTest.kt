package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseLockedException
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testViewModelContext
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import io.ktor.util.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class PasswordLoginViewModelTest {
    val matrixClientFactoryMock = mock<MatrixClientFactory>()

    val matrixClientMock = mock<MatrixClient>()

    private val onBackMock = mock<Function0<Unit>>()
    private val onLoginMock = mock<Function0<Unit>>()

    init {
        resetMocks(matrixClientFactoryMock, matrixClientMock, onBackMock, onLoginMock)
        every { onBackMock() } returns Unit
        every { onLoginMock() } returns Unit
        every { matrixClientMock.userId } returns UserId("test", "server")
    }

    @Test
    fun `call login and start sync`() = runTest {
        everySuspend {
            matrixClientFactoryMock.loginWith(
                any(),
                any(),
                any(),
            )
        } returns Result.success(matrixClientMock)
        every { matrixClientMock.loginState } returns MutableStateFlow(null)

        val cut = viewModel()
        cut.canLogin.first { it }
        cut.tryLogin()
        delay(10)

        verifySuspend {
            matrixClientFactoryMock.loginWith(
                any(),
                any(),
                any(),
            )
            onLoginMock.invoke()
        }
        cut.addMatrixAccountState.value shouldBe AddMatrixAccountState.Success
    }

    @Test
    fun `set addMatrixAccountState when login fails because it was forbidden`() = runTest {
        everySuspend {
            matrixClientFactoryMock.loginWith(
                any(),
                any(),
                any(),
            )
        } returns Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("403")))
        val cut = viewModel()
        cut.canLogin.first { it }
        cut.tryLogin()
        delay(10)

        cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "not correct"
    }


    @Test
    fun `show the correct error message when server is configured wrong`() = runTest {
        everySuspend {
            matrixClientFactoryMock.loginWith(
                any(),
                any(),
                any(),
            )
        } returns Result.failure(UnresolvedAddressException())
        val cut = viewModel()
        cut.canLogin.first { it }
        cut.tryLogin()
        delay(10)

        cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "address"
    }

    @Test
    fun `show the correct error message when an unknown error occurs`() = runTest {
        everySuspend {
            matrixClientFactoryMock.loginWith(
                any(),
                any(),
                any(),
            )
        } returns Result.failure(Exception("Something unexpected."))
        val cut = viewModel()
        cut.canLogin.first { it }
        cut.tryLogin()
        delay(10)

        cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "Matrix server"
    }

    @Test
    fun `cancel login when user aborts the login`() = runTest {
        everySuspend {
            matrixClientFactoryMock.loginWith(
                any(),
                any(),
                any(),
            )
        } returns Result.success(matrixClientMock)
        val cut = viewModel()
        cut.canLogin.first { it }
        cut.back()

        verify {
            onBackMock.invoke()
        }
    }

    @Test
    fun `abort with correct Exception in callback when store is locked`() = runTest {
        everySuspend {
            matrixClientFactoryMock.loginWith(
                any(),
                any(),
                any(),
            )
        } calls { throw DatabaseLockedException() }
        val cut = viewModel()
        cut.canLogin.first { it }
        cut.tryLogin()
        delay(10)

        cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "instance"
    }

    private suspend fun TestScope.viewModel(serverUrl: String = "http://localhost"): PasswordLoginViewModelImpl {
        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules() + module {
                single<MatrixClientFactory> { matrixClientFactoryMock }
            })
        }.koin
        di.get<I18n>().setCurrentLang(DefaultLanguages.EN)
        return PasswordLoginViewModelImpl(
            viewModelContext = testViewModelContext(di),
            serverUrl = serverUrl,
            onLogin = onLoginMock,
            onBack = onBackMock,
        ).apply {
            username.update("timmy")
            password.update("sup3rs3cr3t")
        }
    }
}
