package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import de.connect2x.trixnity.messenger.viewmodel.util.testMessengerSettings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.utils.io.*
import korlibs.io.lang.Charset
import korlibs.io.lang.toString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.kodein.mock.mockFunction0
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class RegisterNewAccountViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 3_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientServiceMock: MatrixClientService

    private val onLoginMock = mockFunction0<Unit>(mocker)

    init {
        coroutineTestScope = true
        Dispatchers.setMain(testMainDispatcher)

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                everySuspending {
                    matrixClientServiceMock.login(isAny(), isAny(), isAny(), isAny(), isAny())
                } returns Result.success(Unit)

                every { onLoginMock.invoke() } returns Unit
            }
        }

        should("show an empty list of registration options when no server is selected") {
            val cut = registerNewAccountViewModel(coroutineContext)
            testCoroutineScheduler.advanceTimeBy(600.milliseconds)
            testCoroutineScheduler.advanceUntilIdle()

            cut.registrationOptions.value shouldBe emptyList()
            cut.selectedRegistration.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("show an empty list of registration options when the given server URL is no valid URL") {
            val cut = registerNewAccountViewModel(coroutineContext)
            cut.serverUrl.update { "87fydf##://ds" }
            testCoroutineScheduler.advanceTimeBy(600.milliseconds)
            testCoroutineScheduler.advanceUntilIdle()

            cut.registrationOptions.value shouldBe emptyList()
            cut.selectedRegistration.value shouldBe null

            cancelNeverEndingCoroutines()
        }

        should("show all registration options when server url is valid and the server returns a list of options") {
            val mockEngine = MockEngine.config {
                dispatcher = dispatcher()
                addHandler { request ->
                    when {
                        request.url.encodedPath.contains(".well-known") ->
                            respond(
                                content = ByteReadChannel(
                                    """
                                {
                                    "m.homeserver": {
                                        "base_url": "http://myMatrixServer:55678"
                                    }
                                }
                            """.trimIndent()
                                ),
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )

                        request.url.encodedPath.contains("versions") ->
                            respond(
                                content = ByteReadChannel(
                                    """
                                {
                                    "versions": [],
                                    "unstable_features": {}
                                }
                            """.trimIndent()
                                ),
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )

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
            }.create()
            val cut = registerNewAccountViewModel(coroutineContext, mockEngine)
            cut.serverUrl.update { "http://myMatrixServer:55678" }
            testCoroutineScheduler.advanceUntilIdle()

            cut.registrationOptions.value shouldBe listOf(
                AuthenticationType.RegistrationToken,
                AuthenticationType.Password,
            )
            cut.selectedRegistration.value shouldBe AuthenticationType.RegistrationToken

            cancelNeverEndingCoroutines()
        }

        should("register with username/password and registration token when registration token is selected") {
            val mockEngine = MockEngine.config {
                dispatcher = dispatcher()
                addHandler { request ->
                    val body = request.body.toByteArray().toString(Charset.forName("UTF-8"))
                    println("  -- ${request.url.encodedPath}")
                    when {
                        request.url.encodedPath.contains(".well-known") ->
                            respond(
                                content = ByteReadChannel(
                                    """
                                {
                                    "m.homeserver": {
                                        "base_url": "http://myMatrixServer:55678"
                                    }
                                }
                            """.trimIndent()
                                ),
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )

                        request.url.encodedPath.contains("versions") ->
                            respond(
                                content = ByteReadChannel(
                                    """
                                {
                                    "versions": [],
                                    "unstable_features": {}
                                }
                            """.trimIndent()
                                ),
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )

                        request.url.encodedPath.contains("validity") ->
                            respond("""
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
            }.create()
            val cut = registerNewAccountViewModel(coroutineContext, mockEngine)
            cut.serverUrl.update { "http://myMatrixServer:55678" }
            testCoroutineScheduler.advanceUntilIdle()
            cut.selectedRegistration.value shouldBe AuthenticationType.RegistrationToken
            cut.accountName.update { "Standard" }
            cut.username.update { "user1" }
            cut.password.update { "user1-password" }
            cut.registrationToken.update { "myRegistrationToken" }
            cut.tryRegistration()
            testCoroutineScheduler.advanceUntilIdle()

            mocker.verifyWithSuspend {
                matrixClientServiceMock.login(
                    isEqual(Url("http://myMatrixServer:55678")),
                    isEqual(IdentifierType.User("user1")),
                    isEqual("user1-password"),
                    isAny(),
                    isEqual("Standard")
                )
                onLoginMock.invoke()
            }

            cancelNeverEndingCoroutines()
        }
    }

    private fun registerNewAccountViewModel(
        coroutineContext: CoroutineContext,
        mockEngine: HttpClientEngine = MockEngine.config {
            dispatcher = coroutineContext.testCoroutineScheduler[CoroutineDispatcher] ?: Dispatchers.Unconfined
            addHandler { _ -> respond("") }
        }.create(),
    ): RegisterNewAccountViewModelImpl {
        val di = koinApplication {
            modules(
                trixnityMessengerModule(),
                module {
                    single { testMessengerSettings("EN") }
                }
            )
        }.koin
        return RegisterNewAccountViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                di = di,
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                coroutineContext = coroutineContext,
            ),
            matrixClientServiceMock,
            onLogin = onLoginMock,
            onCancel = mockFunction0(mocker),
            httpClientFactory = { config ->
                HttpClient(mockEngine) {
                    config()
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.ALL
                    }
                }
            }
        )
    }

    private fun TestScope.dispatcher() =
        coroutineContext.testCoroutineScheduler[CoroutineDispatcher] ?: Dispatchers.Unconfined
}
