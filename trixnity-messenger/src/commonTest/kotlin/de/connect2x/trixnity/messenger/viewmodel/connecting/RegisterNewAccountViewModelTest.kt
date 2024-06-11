package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.HttpClientFactory
import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.firstNotNullWithClue
import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.util.AuthorizeUiaMock
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.Register
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.kodein.mock.mockFunction1
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterNewAccountViewModelTest : ShouldSpec() {
    val mocker = Mocker()

    @Mock
    lateinit var matrixClientFactoryMock: MatrixClientFactory

    @Mock
    lateinit var matrixClientMock: MatrixClient

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var authorizeUia: AuthorizeUiaMock

    private val onLoginMock = mockFunction1<Unit, MatrixClient>(mocker)

    init {
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            coroutineScope = CoroutineScope(Dispatchers.Default)
            authorizeUia = AuthorizeUiaMock(coroutineScope)
            with(mocker) {
                everySuspending {
                    matrixClientFactoryMock.loginWith(isAny(), isAny(), isAny())
                } returns Result.success(MatrixClientFactory.LoginResult(matrixClientMock, null))

                every { onLoginMock.invoke(isAny()) } returns Unit
                every { matrixClientMock.userId } returns UserId("test", "server")
            }
        }
        afterTest {
            coroutineScope.cancel()
        }

        should("register") {
            val cut = registerNewAccountViewModel(serverUrl = "http://myMatrixServer:55678")
            cut.canRegisterNewUser.value shouldBe false
            cut.username.update { "user1" }
            cut.password.update { "user1-password" }
            cut.canRegisterNewUser.firstWithClue { true }

            cut.register()

            val authorizeUiaParams = authorizeUia.onRequestFlowState.firstNotNullWithClue()
            authorizeUiaParams.onResult(
                AuthorizeUiaResult.Success(
                    UIA.Success(
                        Register.Response(
                            UserId("@user1:myMatrixServer:55678"),
                            "GHTYAJCE",
                            "abc123"
                        )
                    )
                )
            )

            eventually(2.seconds) {
                mocker.verifyWithSuspend(exhaustive = false) {
                    matrixClientFactoryMock.loginWith(
                        isEqual(Url("http://myMatrixServer:55678")),
                        isEqual(
                            MatrixClient.LoginInfo(
                                UserId("@user1:myMatrixServer:55678"),
                                "GHTYAJCE",
                                "abc123"
                            )
                        ),
                        isAny(),
                    )
                    onLoginMock.invoke(isAny())
                }
            }

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun registerNewAccountViewModel(
        serverUrl: String = "https://local.host",
    ): RegisterNewAccountViewModelImpl {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val currentCoroutineContext = currentCoroutineContext()
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules() +
                        module {
                            single<HttpClientFactory> {
                                HttpClientFactory {
                                    {
                                        HttpClient(MockEngine {
                                            respond("")
                                        }) {
                                            it()
                                            install(Logging) {
                                                logger = Logger.DEFAULT
                                                level = LogLevel.ALL
                                            }
                                        }
                                    }
                                }
                            }
                            single<MatrixClientFactory> { matrixClientFactoryMock }
                            single<AuthorizeUia> { authorizeUia }
                        }
            )
        }.koin
        return RegisterNewAccountViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                di = di,
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                coroutineContext = currentCoroutineContext,
            ),
            serverUrl,
            onLogin = onLoginMock,
            onBack = mockFunction0(mocker),
        )
    }
}
