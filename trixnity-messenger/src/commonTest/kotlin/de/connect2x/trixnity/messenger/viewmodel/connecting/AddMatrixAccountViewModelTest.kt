package de.connect2x.trixnity.messenger.viewmodel.connecting

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModel.ServerDiscoveryState
import dev.mokkery.MockMode.autoUnit
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.fail


class AddMatrixAccountViewModelTest {
    val onCancelMock = mock<Function0<Unit>>(autoUnit)
    val onAddMatrixAccountMethodMock = mock<Function1<AddMatrixAccountMethod, Unit>>(autoUnit)

    @Test
    fun `do server discovery and collect login and registration options`() = runTest {
        val cut = viewModel { request ->
            if (request.url.encodedPath == "/.well-known/matrix/client")
                return@viewModel respondJson(Responses.WELL_KNOWN)

            request.url.host shouldBe "matrix.server.host"

            when (request.url.encodedPath) {
                "/_matrix/client/versions" ->
                    respondJson(Responses.VERSIONS)

                "/_matrix/client/v3/login" ->
                    respondJson(Responses.MULTI_LOGIN)

                "/_matrix/client/v3/register" ->
                    respondJson(Responses.REGISTER, HttpStatusCode.Unauthorized)


                "/_matrix/client/v1/auth_metadata" ->
                    respondJson(Responses.AUTH_METADATA)

                else -> null
            }
        }
        cut.serverUrl.update("server.host")

        cut.serverDiscoveryState.first { it is ServerDiscoveryState.Success } shouldBe ServerDiscoveryState.Success(
            setOf(
                AddMatrixAccountMethod.OAuth2("https://matrix.server.host", type = OAuth2LoginViewModel.Type.LOGIN),
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
    }

    @Test
    fun `select add matrix account method`() = runTest {
        val cut = viewModel { request ->
            request.url.host shouldBe "server.host"

            when (request.url.encodedPath) {
                "/.well-known/matrix/client" ->
                    respondError(HttpStatusCode.NotFound)

                "/_matrix/client/versions" ->
                    respondJson(Responses.VERSIONS)

                "/_matrix/client/v3/login" ->
                    respondJson(Responses.PASSWORD_LOGIN)

                "/_matrix/client/v3/register" ->
                    respondError(HttpStatusCode.Forbidden)

                "/_matrix/client/v1/auth_metadata" ->
                    respondJson(Responses.AUTH_METADATA)

                else -> null
            }
        }
        cut.serverUrl.update("server.host")

        cut.serverDiscoveryState.first { it is ServerDiscoveryState.Success } shouldBe ServerDiscoveryState.Success(
            setOf(
                AddMatrixAccountMethod.OAuth2("https://server.host", type = OAuth2LoginViewModel.Type.LOGIN),
                AddMatrixAccountMethod.Password("https://server.host")
            )
        )

        cut.selectAddMatrixAccountMethod(AddMatrixAccountMethod.Password("https://server.host"))

        verify {
            onAddMatrixAccountMethodMock.invoke(AddMatrixAccountMethod.Password("https://server.host"))
        }
    }

    private suspend fun TestScope.viewModel(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData?,
    ): AddMatrixAccountViewModelImpl {

        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules())
        }.koin

        di.apply {
            get<MatrixMessengerConfiguration>().apply {
                httpClientEngine = MockEngine.config {
                    dispatcher = testDispatcher
                    addHandler { request ->
                        async { handler(request) ?: unhandledRequest(request) }.await()
                    }
                }.create()
            }
            get<I18n>().setCurrentLang(DefaultLanguages.EN)
        }

        return AddMatrixAccountViewModelImpl(
            viewModelContext = ViewModelContextImpl(
                di = di,
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                coroutineContext = backgroundScope.coroutineContext
            ),
            onAddMatrixAccountMethod = onAddMatrixAccountMethodMock,
            onCancel = onCancelMock,
        )
    }

    object Responses {
        const val WELL_KNOWN = """{ "m.homeserver": { "base_url": "https://matrix.server.host" } }"""
        const val VERSIONS = """{ "versions": [], "unstable_features": {} }"""
        const val MULTI_LOGIN =
            """
                { 
                    "flows": [
                        { "type": "m.login.password" },
                        { "identity_providers": 
                            [
                                { "id": "com.example.idp.github", "name": "GitHub" },
                                { "id": "com.example.idp.gitlab", "name": "GitLab" }
                            ],
                            "type": "m.login.sso"
                        }
                    ]
                }
            """
        const val PASSWORD_LOGIN = """{ "flows": [ { "type": "m.login.password" } ] }"""
        const val REGISTER =
            """{ "completed": [], "flows": [{ "stages": [ "m.login.dummy" ] }], "session": "xxxxxxyz" }"""

        const val AUTH_METADATA =
            """
                {
                  "authorization_endpoint": "https://account.example.com/oauth2/auth",
                  "code_challenge_methods_supported": [
                    "S256"
                  ],
                  "grant_types_supported": [
                    "authorization_code",
                    "refresh_token"
                  ],
                  "issuer": "https://account.example.com/",
                  "registration_endpoint": "https://account.example.com/oauth2/clients/register",
                  "response_modes_supported": [
                    "query",
                    "fragment"
                  ],
                  "response_types_supported": [
                    "code"
                  ],
                  "revocation_endpoint": "https://account.example.com/oauth2/revoke",
                  "token_endpoint": "https://account.example.com/oauth2/token"
                }
            """
    }
}

fun MockRequestHandleScope.respondJson(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    headers: Headers = headersOf(),
) = respond(
    content = content,
    status = status,
    headers = HeadersImpl(
        buildMap {
            put(HttpHeaders.ContentType, listOf(ContentType.Application.Json.toString()))
            putAll(headers.toMap())
        }
    )
)

fun MockRequestHandleScope.unhandledRequest(
    request: HttpRequestData,
): Nothing = fail("unhandled request: $request")
