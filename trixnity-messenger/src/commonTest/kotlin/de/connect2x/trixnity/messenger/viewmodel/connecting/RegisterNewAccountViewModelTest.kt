package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.HttpClientFactory
import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.util.IOOrDefault
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class RegisterNewAccountViewModelTest : ShouldSpec() {
    val mocker = Mocker()

    @Mock
    lateinit var matrixClientFactoryMock: MatrixClientFactory

    @Mock
    lateinit var matrixClientMock: MatrixClient

    private val onLoginMock = mockFunction0<Unit>(mocker)

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                everySuspending {
                    matrixClientFactoryMock.loginWith(isAny(), isAny(), isAny())
                } returns Result.success(MatrixClientFactory.LoginResult(matrixClientMock, null))

                every { onLoginMock.invoke() } returns Unit
                every { matrixClientMock.userId } returns UserId("test", "server")
            }
        }

        should("show an empty list of registration options when no server is selected") {
            val cut = registerNewAccountViewModel()
            testCoroutineScheduler.advanceTimeBy(600.milliseconds)
            testCoroutineScheduler.advanceUntilIdle()

            cut.registrationOptions.value shouldBe emptyList()
            cut.selectedRegistration.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("show an empty list of registration options when the given server URL is no valid URL") {
            val cut = registerNewAccountViewModel(serverUrl = "87fydf##://ds")
            testCoroutineScheduler.advanceTimeBy(600.milliseconds)
            testCoroutineScheduler.advanceUntilIdle()

            cut.registrationOptions.value shouldBe emptyList()
            cut.selectedRegistration.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("show all registration options when server url is valid and the server returns a list of options") {
            val cut =
                registerNewAccountViewModel(serverUrl = "http://myMatrixServer:55678") {
                    addHandler { request ->
                        when {
                            request.url.host == "myMatrixServer" && request.url.port == 55678 -> {
                                respond(
                                    content = ByteReadChannel(
                                        """
                                {
                                  "completed": [],
                                  "flows": [
                                    {
                                      "stages": [
                                        "m.login.registration_token"
                                      ]
                                    },
                                    {
                                      "stages": [
                                        "m.login.password"
                                      ]
                                    },
                                    {
                                      "stages": [
                                        "m.login.dummy"
                                      ]
                                    }
                                  ],
                                  "session": "xxxxxxyz"
                                }
                            """.trimIndent()
                                    ),
                                    status = HttpStatusCode.Unauthorized,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )
                            }

                            else -> respond("")
                        }
                    }
                }
            testCoroutineScheduler.advanceUntilIdle()

            cut.registrationOptions.first {
                it == listOf(
                    AuthenticationType.RegistrationToken,
                    AuthenticationType.Password,
                )
            }
            cut.selectedRegistration.first { it == AuthenticationType.RegistrationToken }

            cancelNeverEndingCoroutines()
        }

        should("register with username/password and registration token when registration token is selected") {
            val cut =
                registerNewAccountViewModel(serverUrl = "http://myMatrixServer:55678") {
                    addHandler { request ->
                        val body = request.body.toByteArray().decodeToString()
                        when {
                            request.url.encodedPath.contains("validity") ->
                                respond(
                                    """
                                {
                                    "valid": true
                                }
                            """.trimIndent(),
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )

                            request.url.host == "myMatrixServer" &&
                                    request.url.port == 55678 &&
                                    request.url.pathSegments.contains("register") &&
                                    body.contains("token").not() &&
                                    body.contains("m.login.dummy").not() ->
                                respond(
                                    content = ByteReadChannel(
                                        """
                                {
                                  "completed": [],
                                  "flows": [
                                    {
                                      "stages": [
                                        "m.login.registration_token",
                                        "m.login.dummy"
                                      ]
                                    },
                                    {
                                      "stages": [
                                        "m.login.registration_token",
                                        "m.login.email.identity"
                                      ]
                                    }
                                  ],
                                  "session": "xxxxxxyz"
                                }
                                """.trimIndent()
                                    ),
                                    status = HttpStatusCode.Unauthorized,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )

                            request.url.host == "myMatrixServer" &&
                                    request.url.port == 55678 &&
                                    body.contains("token") ->
                                respond(
                                    content = ByteReadChannel(
                                        """
                                    {
                                      "completed": [
                                        "m.login.registration_token"
                                      ],
                                      "flows": [
                                        {
                                          "stages": [
                                            "m.login.registration_token",
                                            "m.login.dummy"
                                          ]
                                        }
                                      ],
                                      "session": "xxxxxxyz"
                                    }
                                    """.trimIndent()
                                    ),
                                    status = HttpStatusCode.Unauthorized,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )

                            request.url.host == "myMatrixServer" &&
                                    request.url.port == 55678 &&
                                    body.contains("m.login.dummy") ->
                                respond(
                                    content = ByteReadChannel(
                                        """
                                    {
                                      "access_token": "abc123",
                                      "device_id": "GHTYAJCE",
                                      "user_id": "@user1:myMatrixServer:55678"
                                    }
                                    """.trimIndent()
                                    ),
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )

                            else -> respond("")
                        }
                    }
                }
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectedRegistration.first { it == AuthenticationType.RegistrationToken }
            cut.username.update { "user1" }
            cut.password.update { "user1-password" }
            cut.registrationToken.update { "myRegistrationToken" }

            cut.canRegisterNewUser.first { it }
            cut.tryRegistration()
            testCoroutineScheduler.advanceUntilIdle()

            withContext(Dispatchers.IOOrDefault) { // eventually does not work with TestDispatcher
                // we need eventually here since the MockEngine can only be run with Dispatchers.IO (MockEngine.config {dispatcher cannot be used anymore})
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
                        onLoginMock.invoke()
                    }
                }
            }

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun registerNewAccountViewModel(
        serverUrl: String = "https://local.host",
        mockEngineConfig: (MockEngineConfig.() -> Unit)? = null,
    ): RegisterNewAccountViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val currentCoroutineContext = currentCoroutineContext()
        val mockEngine = MockEngine.config {
            if (mockEngineConfig != null) mockEngineConfig()
            else addHandler { _ -> respond("") }
        }.create()
        val di = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules() +
                        module {
                            single<HttpClientFactory> {
                                HttpClientFactory {
                                    {
                                        HttpClient(mockEngine) {
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
