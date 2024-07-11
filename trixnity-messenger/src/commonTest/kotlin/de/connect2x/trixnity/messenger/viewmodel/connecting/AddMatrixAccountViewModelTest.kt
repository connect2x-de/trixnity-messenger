package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.HttpClientFactory
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class AddMatrixAccountViewModelTest : ShouldSpec() {
    private val onCancelMock = mock<Function0<Unit>>()
    private val onAddMatrixAccountMethodMock = mock<Function1<AddMatrixAccountMethod, Unit>>()

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(onCancelMock, onAddMatrixAccountMethodMock)
            every { onAddMatrixAccountMethodMock(any()) } returns Unit
            every { onCancelMock() } returns Unit
        }

        should("do server discovery and collect login and registration options") {
            val cut = viewModel {
                addHandler { request ->
                    when {
                        request.url.encodedPath.contains(".well-known") ->
                            respond(
                                """
                                    {
                                        "m.homeserver": {
                                            "base_url": "https://matrix.server.host"
                                        }
                                    }
                                """.trimIndent(),
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )

                        request.url.encodedPath.contains("versions") ->
                            respond(
                                """
                                    {
                                        "versions": [],
                                        "unstable_features": {}
                                    }
                                """.trimIndent(),
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )

                        request.url.encodedPath.contains("/login") && request.url.host == "matrix.server.host" ->
                            respond(
                                """
                                    {
                                      "flows": [
                                        {
                                          "type": "m.login.password"
                                        },
                                        {
                                          "identity_providers": [
                                            {
                                              "id": "com.example.idp.github",
                                              "name": "GitHub"
                                            },
                                            {
                                              "id": "com.example.idp.gitlab",
                                              "name": "GitLab"
                                            }
                                          ],
                                          "type": "m.login.sso"
                                        }
                                      ]
                                    }
                                """.trimIndent(),
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )

                        request.url.encodedPath.contains("/register") && request.url.host == "matrix.server.host" -> {
                            respond(
                                """
                                    {
                                      "completed": [],
                                      "flows": [
                                        {
                                          "stages": [
                                            "m.login.dummy"
                                          ]
                                        }
                                      ],
                                      "session": "xxxxxxyz"
                                    }
                                """.trimIndent(),
                                HttpStatusCode.Unauthorized,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }

                        else -> throw IllegalStateException("no handler for mock engine matched $request")
                    }
                }
            }
            cut.serverUrl.value = "server.host"

            cut.serverDiscoveryState.first { it is ServerDiscoveryState.Success } shouldBe ServerDiscoveryState.Success(
                setOf(
                    AddMatrixAccountMethod.Password("https://matrix.server.host"),
                    AddMatrixAccountMethod.SSO(
                        serverUrl = "https://matrix.server.host",
                        identityProvider = LoginType.SSO.IdentityProvider(
                            id = "com.example.idp.github",
                            name = "GitHub"
                        ),
                        icon = null
                    ),
                    AddMatrixAccountMethod.SSO(
                        serverUrl = "https://matrix.server.host",
                        identityProvider = LoginType.SSO.IdentityProvider(
                            id = "com.example.idp.gitlab",
                            name = "GitLab"
                        ),
                        icon = null
                    ),
                    AddMatrixAccountMethod.Register("https://matrix.server.host"),
                )
            )

            cancelNeverEndingCoroutines()
        }

        should("select add matrix account method") {
            val cut = viewModel {
                addHandler { request ->
                    when {
                        request.url.encodedPath.contains("versions") ->
                            respond(
                                """
                                    {
                                        "versions": [],
                                        "unstable_features": {}
                                    }
                                """.trimIndent(),
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )

                        request.url.encodedPath.contains("/login") ->
                            respond(
                                """
                                    {
                                      "flows": [
                                        {
                                          "type": "m.login.password"
                                        }
                                      ]
                                    }
                                """.trimIndent(),
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )

                        else -> respond("no handler defined", HttpStatusCode.NotFound)
                    }
                }
            }
            cut.serverUrl.value = "server.host"

            cut.serverDiscoveryState.first { it is ServerDiscoveryState.Success } shouldBe ServerDiscoveryState.Success(
                setOf(AddMatrixAccountMethod.Password("https://server.host"))
            )

            cut.selectAddMatrixAccountMethod(AddMatrixAccountMethod.Password("https://server.host"))

            verify {
                onAddMatrixAccountMethodMock.invoke(AddMatrixAccountMethod.Password("https://server.host"))
            }

            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun viewModel(
        mockEngineConfig: (MockEngineConfig.() -> Unit)? = null,
    ): AddMatrixAccountViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val currentCoroutineContext = currentCoroutineContext()
        val mockEngine = MockEngine.config {
            if (mockEngineConfig != null) mockEngineConfig()
            else addHandler { _ -> respond("") }
        }.create()
        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules() + module {
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
            })
        }.koin
        di.get<I18n>().setCurrentLang(DefaultLanguages.EN)
        return AddMatrixAccountViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                di,
                DefaultComponentContext(LifecycleRegistry()),
                coroutineContext = currentCoroutineContext,
            ),
            onAddMatrixAccountMethod = onAddMatrixAccountMethodMock,
            onCancel = onCancelMock,
        )
    }
}
