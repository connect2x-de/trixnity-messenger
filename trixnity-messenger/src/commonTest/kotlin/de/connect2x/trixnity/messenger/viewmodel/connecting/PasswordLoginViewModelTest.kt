package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.clientserverapi.client.ClassicMatrixClientAuthProviderData
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test

class PasswordLoginViewModelTest {
    val matrixClientsMock = mock<MatrixClients>()

    private val onBackMock = mock<Function0<Unit>>()
    private val onLoginMock = mock<Function0<Unit>>()

    init {
        resetMocks(matrixClientsMock, onBackMock, onLoginMock)
        every { onBackMock() } returns Unit
        every { onLoginMock() } returns Unit
    }

    @Test
    fun `call login and start sync`() = runTest {
        everySuspend {
            matrixClientsMock.create(any())
        } returns MatrixClients.CreateResult.Success

        val cut = viewModel()
        cut.canLogin.first { it }
        cut.tryLogin()
        delay(10)

        verifySuspend {
            matrixClientsMock.create(
                ClassicMatrixClientAuthProviderData(
                    baseUrl = Url("http://localhost"),
                    accessToken = "abc123",
                    accessTokenExpiresInMs = null,
                    refreshToken = null,
                )
            )
            onLoginMock.invoke()
        }
        cut.addMatrixAccountState.value shouldBe AddMatrixAccountState.Success
    }

    @Test
    fun `set addMatrixAccountState when login fails because it was forbidden`() = runTest {
        everySuspend {
            matrixClientsMock.create(any())
        } returns MatrixClients.CreateResult.Failure.AccountAlreadyExists("exists")

        val cut = viewModel()
        cut.canLogin.first { it }
        cut.tryLogin()
        delay(10)

        cut.addMatrixAccountState.value.shouldBeInstanceOf<AddMatrixAccountState.Failure>().message shouldContain "exists"
    }

    private suspend fun TestScope.viewModel(
        serverUrl: String = "http://localhost",
        withFailure: Boolean = false,
    ): PasswordLoginViewModelImpl {
        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules() + module {
                single<MatrixClients> { matrixClientsMock }
                single<MatrixMessengerConfiguration> {
                    MatrixMessengerConfiguration().apply {
                        httpClientEngine = MockEngine.create {
                            dispatcher = coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
                            addHandler { request ->
                                when (request.url.fullPath) {
                                    "/_matrix/client/v3/login" -> {
                                        if (withFailure)
                                            respond(
                                                "wrong password",
                                                HttpStatusCode.Unauthorized
                                            )
                                        else
                                            respond(
                                                """
                                                    {
                                                      "access_token": "abc123",
                                                      "device_id": "GHTYAJCE",
                                                      "user_id": "@cheeky_monkey:matrix.org"
                                                    }
                                                """.trimIndent(),
                                                HttpStatusCode.OK,
                                                headersOf(
                                                    HttpHeaders.ContentType,
                                                    ContentType.Application.Json.toString(),
                                                )
                                            )
                                    }

                                    else -> {
                                        respond(
                                            "unknown ${request.url.fullPath}",
                                            HttpStatusCode.BadRequest
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
