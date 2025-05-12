package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.firstNotNullWithClue
import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaImpl
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.Register
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class RegisterNewAccountViewModelTest {
    val matrixClientFactoryMock = mock<MatrixClientFactory>()

    val matrixClientMock = mock<MatrixClient>()

    private val authorizeUia = AuthorizeUiaImpl()

    private val onLoginMock = mock<Function0<Unit>>()

    init {
        resetMocks(matrixClientFactoryMock, matrixClientMock, onLoginMock)
        everySuspend {
            matrixClientFactoryMock.loginWith(any(), any(), any())
        } returns Result.success(matrixClientMock)

        every { onLoginMock.invoke() } returns Unit
        every { matrixClientMock.userId } returns UserId("test", "server")
        every { matrixClientMock.loginState } returns MutableStateFlow(null)
    }

    @Test
    fun register() = runTest {
        val cut = registerNewAccountViewModel(serverUrl = "http://myMatrixServer:55678")
        cut.canRegisterNewUser.value shouldBe false
        cut.username.update("user1")
        cut.password.update("user1-password")
        cut.canRegisterNewUser.firstWithClue { true }

        cut.register()

        val authorizeUiaParams = authorizeUia.onRequestFlow.firstNotNullWithClue()
        authorizeUiaParams.onResult(
            AuthorizeUiaResult.Success(
                UIA.Success(
                    Register.Response(
                        UserId("@user1:myMatrixServer:55678"), "GHTYAJCE", "abc123"
                    )
                )
            )
        )

        eventually(2.seconds) {
            verifySuspend {
                matrixClientFactoryMock.loginWith(
                    eq(Url("http://myMatrixServer:55678")),
                    any(),
                    any(),
                )
                onLoginMock.invoke()
            }
        }
    }

    private fun TestScope.registerNewAccountViewModel(
        serverUrl: String = "https://local.host",
    ): RegisterMatrixAccountViewModelImpl {
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules() + module {
                    single<MatrixClientFactory> { matrixClientFactoryMock }
                    single<AuthorizeUia> { authorizeUia }
                })
        }.koin
        di.get<MatrixMessengerConfiguration>().httpClientEngine = MockEngine.create {
            dispatcher = coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
            addHandler {
                respond("")
            }
        }
        return RegisterMatrixAccountViewModelImpl(
            viewModelContext = testViewModelContext(
                di = di,
            ),
            serverUrl,
            onLogin = onLoginMock,
            onBack = mock(),
        )
    }
}
