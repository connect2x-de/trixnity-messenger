package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.clientserverapi.client.AuthenticationApiClient
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.oauth2.oAuth2
import de.connect2x.trixnity.clientserverapi.model.authentication.IdentifierType.User
import de.connect2x.trixnity.clientserverapi.model.authentication.Login
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixClientsImpl
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.secrets.SecretByteArrays
import de.connect2x.trixnity.messenger.secrets.SecretId
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import dev.mokkery.answering.SuspendAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Suppress("NonAsciiCharacters")
class MatrixClientsTest {
    private val baseUrl = Url("https://localhost")
    private val userId1 = UserId("user1", "localhost")
    private val userId2 = UserId("user2", "localhost")
    private val mutableMatrixClients: MutableStateFlow<Map<UserId, MatrixClient>>
    private val loginState: MutableStateFlow<MatrixClient.LoginState>

    // We could also do a mapping of access token to userId, but this is just simpler.
    private val authProviderData1 = MatrixClientAuthProviderData.oAuth2(baseUrl, "1", userId1.full)
    private val authProviderData2 = MatrixClientAuthProviderData.oAuth2(baseUrl, "2", userId2.full)

    private val matrixClientMock1 = mock<MatrixClient>()
    private val matrixClientMock2 = mock<MatrixClient>()
    private val matrixClientServerApiClient = mock<MatrixClientServerApiClient>()
    private val authenticationApiClient = mock<AuthenticationApiClient>()
    private val matrixClientFactory = mock<MatrixClientFactory>()
    private val deleteAccountData = mock<DeleteAccountData>()
    private val secretByteArrays = mock<SecretByteArrays>()

    private val settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder()

    private var createCalled = false
    private var loadCalled = false
    private var logoutCalled = false
    private var createCalledCount = 0
    private var loadCalledCount = 0

    private val createMatrixClient: SuspendAnsweringScope<Result<MatrixClient>> = everySuspend {
        matrixClientFactory.create(any(), any(), any())
    }

    private val loadMatrixClient: SuspendAnsweringScope<Result<MatrixClient>> = everySuspend {
        matrixClientFactory.load(any(), any(), any())
    }

    init {
        createMatrixClient calls
            {
                val userId = it.args[0] as? UserId
                createCalled = true
                createCalledCount++
                val matrixClient =
                    when (userId) {
                        userId1 -> matrixClientMock1
                        userId2 -> matrixClientMock2
                        else -> fail("unconfigured repositories module $userId")
                    }
                Result.success(matrixClient)
            }

        loadMatrixClient calls
            {
                val userId = it.args[0] as? UserId
                loadCalled = true
                loadCalledCount++
                val matrixClient =
                    when (userId) {
                        userId1 -> matrixClientMock1
                        userId2 -> matrixClientMock2
                        else -> fail("unconfigured repositories module $userId")
                    }
                Result.success(matrixClient)
            }

        val state =
            koinApplication {
                    modules(
                        module {
                            single<CoroutineScope> {
                                CoroutineScope(EmptyCoroutineContext).also { it.coroutineContext.job.cancel() }
                            }
                        }
                    )
                }
                .koin
        every { matrixClientMock1.di } returns state

        every { matrixClientMock1.userId } returns userId1
        every { matrixClientMock2.userId } returns userId2
        loginState = MutableStateFlow(MatrixClient.LoginState.LOGGED_IN)
        every { matrixClientMock1.loginState } returns loginState
        every { matrixClientMock2.loginState } returns loginState
        everySuspend { matrixClientMock1.logout() } calls
            {
                logoutCalled = true
                Result.success(Unit)
            }
        everySuspend { secretByteArrays.set(any<SecretId>(), any()) } returns Unit
        everySuspend { secretByteArrays.get(any<SecretId>()) } returns null
        everySuspend { secretByteArrays.removeSecretsForUser(any()) } returns Unit
        everySuspend {
            authenticationApiClient.login(
                identifier = any(),
                password = any(),
                token = any(),
                type = any(),
                deviceId = any(),
                initialDeviceDisplayName = any(),
                refreshToken = any(),
            )
        } calls
            { args ->
                val username = args.args[0] as User
                Result.success(Login.Response(UserId(username.user, "server"), accessToken = "", deviceId = ""))
            }
        every { matrixClientServerApiClient.authentication } returns authenticationApiClient
        everySuspend { authenticationApiClient.logout() } returns Result.success(Unit)
        every { matrixClientMock1.close() } returns Unit
        every { matrixClientMock2.close() } returns Unit
        everySuspend { matrixClientMock1.closeSuspending() } returns Unit
        everySuspend { matrixClientMock2.closeSuspending() } returns Unit

        everySuspend { deleteAccountData.invoke(any()) } returns Unit
        mutableMatrixClients = MutableStateFlow(mapOf())
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
        createCalled = false
        createCalledCount = 0
        logoutCalled = false
        loadCalled = false
        loadCalledCount = 0
    }

    @Test
    fun `login » login and register new account locally`() = runTest {
        val cut = createCut()
        cut.create(authProviderData1) shouldBe MatrixClients.CreateResult.Success
        cut.value shouldBe mapOf(userId1 to matrixClientMock1)
        createCalled shouldBe true
        createCalledCount shouldBe 1
    }

    @Test
    fun `login » login for another account and create additional MatrixClient`() = runTest {
        val cut = createCut()
        cut.create(authProviderData1) shouldBe MatrixClients.CreateResult.Success
        cut.create(authProviderData2) shouldBe MatrixClients.CreateResult.Success

        createCalled shouldBe true
        createCalledCount shouldBe 2

        cut.value shouldBe mapOf(userId1 to matrixClientMock1, userId2 to matrixClientMock2)
    }

    @Test
    fun `login » not login again if MatrixClient already present for account`() = runTest {
        val cut = createCut()
        cut.create(authProviderData1) shouldBe MatrixClients.CreateResult.Success
        createCalled = false
        cut.create(authProviderData1).shouldBeInstanceOf<MatrixClients.CreateResult.Failure.AccountAlreadyExists>()
        createCalled shouldBe false // keep the existing MatrixClient and do not log in again
    }

    @Test
    fun `login » return exception in Result if login is not possible`() = runTest {
        val cut = createCut()
        createMatrixClient returns Result.failure(IllegalArgumentException())

        cut.create(authProviderData1).shouldBeInstanceOf<MatrixClients.CreateResult.Failure.Unknown>().message
    }

    @Test
    fun `initFromStore » init from the store and settings`() = runTest {
        val cut = createCut()
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        val result = cut.initFromStore()

        result shouldBe MatrixClients.InitFromStoreResult(setOf(userId1, userId2), mapOf())
        cut.value shouldBe mapOf(userId1 to matrixClientMock1, userId2 to matrixClientMock2)
        loadCalled shouldBe true
    }

    @Test
    fun `initFromStore » skip init from store when matrix client is already present`() = runTest {
        val cut = createCut()
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        mutableMatrixClients.value = mapOf(userId1 to matrixClientMock1)
        val result = cut.initFromStore()

        result shouldBe MatrixClients.InitFromStoreResult(setOf(userId2), mapOf())
        cut.value shouldBe mapOf(userId1 to matrixClientMock1, userId2 to matrixClientMock2)
        loadCalledCount shouldBe 1
    }

    @Test
    fun `initFromStore » have failure when init from store is not possible`() = runTest {
        val cut = createCut()
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        loadMatrixClient returns Result.failure(MatrixClientInitializationException.DatabaseAccessException())

        val result = cut.initFromStore()

        result shouldBe
            MatrixClients.InitFromStoreResult(
                setOf(),
                mapOf(userId1 to MatrixClientInitializationException.DatabaseAccessException()),
            )
        cut.value shouldBe mapOf()
    }

    @Test
    fun `initFromStore » remove account from success on delete`() = runTest {
        val cut = createCut()
        val id = userId1

        settings.create(userId1, MatrixMessengerAccountSettingsBase())

        val result = cut.initFromStore()

        result shouldBe MatrixClients.InitFromStoreResult(setOf(id), mapOf())
        cut.value shouldBe mapOf(id to matrixClientMock1)
        loadCalled shouldBe true

        cut.remove(id)
        cut.initFromStoreResult.value shouldBe MatrixClients.InitFromStoreResult(setOf(), mapOf())
    }

    @Test
    fun `initFromStore » remove account from failures on delete`() = runTest {
        val cut = createCut()
        val id = userId1

        settings.create(id, MatrixMessengerAccountSettingsBase())

        loadMatrixClient returns Result.failure(MatrixClientInitializationException.DatabaseAccessException())

        val result = cut.initFromStore()

        result shouldBe
            MatrixClients.InitFromStoreResult(
                setOf(),
                mapOf(id to MatrixClientInitializationException.DatabaseAccessException()),
            )
        cut.value shouldBe mapOf()

        cut.remove(id)
        cut.initFromStoreResult.value shouldBe MatrixClients.InitFromStoreResult(setOf(), mapOf())
    }

    @Test
    fun `logout » logout matrix client`() = runTest {
        val cut = createCut()
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        mutableMatrixClients.value = mapOf(userId1 to matrixClientMock1, userId2 to matrixClientMock2)

        cut.logout(userId1) shouldBe Result.success(Unit)

        cut.value shouldBe mapOf(userId2 to matrixClientMock2)
        logoutCalled shouldBe true
        settings.value.base.accounts.keys shouldBe setOf(userId2)
        verifySuspend {
            matrixClientMock1.closeSuspending()
            deleteAccountData.invoke(userId1)
        }
    }

    @Test
    fun `external logout » remove matrix client`() = runTest {
        val cut = createCut()
        backgroundScope.launch { cut.doWork() }
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        mutableMatrixClients.value = mapOf(userId1 to matrixClientMock1)

        loginState.value = MatrixClient.LoginState.LOGGED_OUT

        cut.filterNotNull().firstWithClue { emptyMap() }
        logoutCalled shouldBe false
        settings.value.base.accounts.keys shouldBe setOf()
        verifySuspend {
            matrixClientMock1.closeSuspending()
            secretByteArrays.removeSecretsForUser(userId1)
            deleteAccountData.invoke(userId1)
        }
    }

    @Test
    fun `remove » remove matrix client`() = runTest {
        val cut = createCut()
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        mutableMatrixClients.value = mapOf(userId1 to matrixClientMock1, userId2 to matrixClientMock2)

        cut.remove(userId1) shouldBe Result.success(Unit)

        cut.value shouldBe mapOf(userId2 to matrixClientMock2)
        logoutCalled shouldBe false
        settings.value.base.accounts.keys shouldBe setOf(userId2)
        verifySuspend {
            matrixClientMock1.closeSuspending()
            secretByteArrays.removeSecretsForUser(userId1)
            deleteAccountData.invoke(userId1)
        }
    }

    private fun TestScope.createCut(): MatrixClientsImpl =
        MatrixClientsImpl(
            matrixClientFactory = matrixClientFactory,
            deleteAccountData = deleteAccountData,
            settings = settings,
            config =
                MatrixMessengerConfiguration().apply {
                    httpClientEngine = MockEngine.create {
                        dispatcher = coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
                        addHandler { request ->
                            when (request.url.fullPath) {
                                "/_matrix/client/v3/account/whoami" -> {
                                    val userId =
                                        request.headers["Authorization"]?.removePrefix("Bearer ")?.let { UserId(it) }
                                    if (userId == null) respond("no Authorization header", HttpStatusCode.Unauthorized)
                                    else
                                        respond(
                                            """{"user_id":"${userId.full}","device_id":"deviceId"}""",
                                            HttpStatusCode.OK,
                                            headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                                        )
                                }

                                else -> {
                                    respond("unknown ${request.url.fullPath}", HttpStatusCode.BadRequest)
                                }
                            }
                        }
                    }
                },
            secretByteArrays = secretByteArrays,
            i18n =
                object :
                    I18n(
                        DefaultLanguages,
                        createTestMatrixMessengerSettingsHolder(),
                        GetSystemLang { "en" },
                        TimeZone.of("CET"),
                    ) {},
            matrixClients = mutableMatrixClients,
        )
}
