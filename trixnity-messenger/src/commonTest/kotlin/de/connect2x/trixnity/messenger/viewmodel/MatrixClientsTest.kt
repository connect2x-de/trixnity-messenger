package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.AccountAlreadyExistsException
import de.connect2x.trixnity.messenger.MatrixClientFactory
import de.connect2x.trixnity.messenger.MatrixClientInitializationException
import de.connect2x.trixnity.messenger.MatrixClientInitializationException.DatabaseLockedException
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixClientsImpl
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import de.connect2x.trixnity.messenger.util.ImmediateDispatcherElement
import dev.mokkery.answering.SuspendAnsweringScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.AuthenticationApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType.User
import net.folivo.trixnity.clientserverapi.model.authentication.Login
import net.folivo.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test


class MatrixClientsTest {
    private val mutableMatrixClients: MutableStateFlow<Map<UserId, MatrixClient>>
    private val loginState: MutableStateFlow<MatrixClient.LoginState>

    private val matrixClientMock1 = mock<MatrixClient>()
    private val matrixClientMock2 = mock<MatrixClient>()
    private val matrixClientServerApiClient = mock<MatrixClientServerApiClient>()
    private val authenticationApiClient = mock<AuthenticationApiClient>()
    private val matrixClientFactory = mock<MatrixClientFactory>()
    private val deleteAccountData = mock<DeleteAccountData>()

    private val settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder()

    private var loginCalled = false
    private var logoutCalled = false
    private var initFromStoreCalled = false
    private var initFromStoreCalledCount = 0

    private val login: SuspendAnsweringScope<Result<MatrixClient>> = everySuspend {
        matrixClientFactory.loginWith(
            any(),
            any(),
            any(),
        )
    }

    init {
        login calls {
            runCatching {
                @Suppress("UNCHECKED_CAST") val loginInfo =
                    (it.args[1] as suspend (MatrixClientServerApiClient) -> MatrixClient.LoginInfo).invoke(
                        matrixClientServerApiClient
                    )
                @Suppress("UNCHECKED_CAST") (it.args[2] as? suspend (MatrixClient.LoginInfo) -> Unit)?.invoke(loginInfo)
                loginCalled = true
                val username = loginInfo.userId.localpart
                when (username) {
                    "test1" -> matrixClientMock1
                    "test2" -> matrixClientMock2
                    else -> fail("username $username not supported in login")
                }
            }
        }

        val state = koinApplication {
            modules(module {
                single<CoroutineScope> {
                    CoroutineScope(EmptyCoroutineContext).also { it.coroutineContext.job.cancel() }
                }
            })
        }.koin
        every { matrixClientMock1.di } returns state

        everySuspend { matrixClientFactory.initFromStore(any()) } calls {
            val username = checkNotNull((it.args[0] as? UserId)).localpart
            initFromStoreCalled = true
            initFromStoreCalledCount++
            val matrixClient = when (username) {
                "test1" -> matrixClientMock1
                "test2" -> matrixClientMock2
                else -> fail("username $username not supported in login")
            }
            Result.success(matrixClient)
        }

        every { matrixClientMock1.userId } returns UserId("test1", "server")
        every { matrixClientMock2.userId } returns UserId("test2", "server")
        loginState = MutableStateFlow(MatrixClient.LoginState.LOGGED_IN)
        every { matrixClientMock1.loginState } returns loginState
        every { matrixClientMock2.loginState } returns loginState
        everySuspend { matrixClientMock1.logout() } calls {
            logoutCalled = true
            Result.success(Unit)
        }
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
        } calls { args ->
            val username = args.args[0] as User
            Result.success(
                Login.Response(
                    UserId(username.user, "server"),
                    accessToken = "",
                    deviceId = "",
                )
            )
        }
        every { matrixClientServerApiClient.authentication } returns authenticationApiClient
        everySuspend { authenticationApiClient.logout(any()) } returns Result.success(Unit)
        every { matrixClientMock1.close() } returns Unit
        every { matrixClientMock2.close() } returns Unit

        everySuspend { deleteAccountData.invoke(any()) } returns Unit
        mutableMatrixClients = MutableStateFlow(mapOf())
    }

    @Test
    fun `login » login and register new account locally`() = runTest {
        val cut = createCut()
        cut.login(Url("https://example.org"), User("test1"), "password", "").getOrThrow()
        cut.value shouldBe mapOf(UserId("test1", "server") to matrixClientMock1)
        loginCalled shouldBe true
    }

    @Test
    fun `login » login for another account and create additional MatrixClient`() = runTest {
        val cut = createCut()
        cut.login(Url("https://example.org"), User("test1"), "password", "").getOrThrow()
        cut.login(Url("https://example2.org"), User("test2"), "password2", "").getOrThrow()

        cut.value shouldBe mapOf(
            UserId("test1", "server") to matrixClientMock1,
            UserId("test2", "server") to matrixClientMock2,
        )
    }

    @Test
    fun `login » not login again if MatrixClient already present for account`() = runTest {
        val cut = createCut()
        cut.login(Url("https://example.org"), User("test1"), "password", "").getOrThrow()
        loginCalled = false
        cut.login(Url("https://example.org"), User("test1"), "password", "") shouldBe Result.failure(
            AccountAlreadyExistsException(UserId("test1", "server"))
        )
        //loginCalled shouldBe false // use the existing MatrixClient and do not log in again
    }

    @Test
    fun `login » return exception in Result if login is not possible`() = runTest {
        val cut = createCut()
        val exception = IllegalHeaderValueException("header", 0)
        login returns Result.failure(exception)

        val result = cut.login(Url("https://example.org"), User("test1"), "password", "")
        result shouldBe Result.failure(exception)
    }

    @Test
    fun `initFromStore » init from the store and settings`() = runTest {
        val cut = createCut()
        settings.update(UserId("test1", "server")) { it }
        settings.update(UserId("test2", "server")) { it }
        val result = cut.initFromStore()

        result shouldBe MatrixClients.InitFromStoreResult(
            setOf(
                UserId("test1", "server"),
                UserId("test2", "server"),
            ), mapOf()
        )
        cut.value shouldBe mapOf(
            UserId("test1", "server") to matrixClientMock1,
            UserId("test2", "server") to matrixClientMock2,
        )
        initFromStoreCalled shouldBe true
    }

    @Test
    fun `initFromStore » skip init from store when matrix client is already present`() = runTest {
        val cut = createCut()
        settings.update(UserId("test1", "server")) { it }
        settings.update(UserId("test2", "server")) { it }
        mutableMatrixClients.value = mapOf(
            UserId("test1", "server") to matrixClientMock1,
        )
        val result = cut.initFromStore()

        result shouldBe MatrixClients.InitFromStoreResult(
            setOf(UserId("test2", "server")), mapOf()
        )
        cut.value shouldBe mapOf(
            UserId("test1", "server") to matrixClientMock1,
            UserId("test2", "server") to matrixClientMock2,
        )
        initFromStoreCalledCount shouldBe 1
    }

    @Test
    fun `initFromStore » have failure when init from store is not possible`() = runTest {
        val cut = createCut()
        settings.update(UserId("test1", "server")) { it }
        everySuspend { matrixClientFactory.initFromStore(any()) } calls {
            initFromStoreCalled = true
            Result.success(null)
        }

        val result = cut.initFromStore()

        result shouldBe MatrixClients.InitFromStoreResult(
            setOf(), mapOf(UserId("test1", "server") to MatrixClientInitializationException.NoDatabaseException)
        )
        cut.value shouldBe mapOf()
        initFromStoreCalled shouldBe true
    }

    @Test
    fun `initFromStore » have failure on exception`() = runTest {
        val cut = createCut()
        settings.update(UserId("test1", "server")) { it }
        everySuspend { matrixClientFactory.initFromStore(any()) } calls {
            Result.failure(DatabaseLockedException("The database is locked."))
        }
        cut.initFromStore() shouldBe MatrixClients.InitFromStoreResult(
            success = setOf(),
            failures = mapOf(UserId("test1", "server") to DatabaseLockedException("The database is locked."))
        )
    }

    @Test
    fun `logout » logout matrix client`() = runTest {
        val cut = createCut()
        settings.update(UserId("test1", "server")) { it }
        settings.update(UserId("test2", "server")) { it }
        mutableMatrixClients.value = mapOf(
            UserId("test1", "server") to matrixClientMock1,
            UserId("test2", "server") to matrixClientMock2,
        )

        cut.logout(UserId("test1", "server")) shouldBe Result.success(Unit)

        cut.value shouldBe mapOf(
            UserId("test2", "server") to matrixClientMock2,
        )
        logoutCalled shouldBe true
        settings.value.base.accounts.keys shouldBe setOf(UserId("test2", "server"))
        verify {
            matrixClientMock1.close()
        }
        verifySuspend {
            deleteAccountData.invoke(UserId("test1", "server"))
        }
    }


    @Test
    fun `external logout » remove matrix client`() = runTest {
        val cut = createCut()
        settings.update(UserId("test1", "server")) { it }
        mutableMatrixClients.value = mapOf(
            UserId("test1", "server") to matrixClientMock1,
        )

        loginState.value = MatrixClient.LoginState.LOGGED_OUT

        cut.filterNotNull().first { it.isEmpty() }
        logoutCalled shouldBe false
        settings.value.base.accounts.keys shouldBe setOf()
        verify {
            matrixClientMock1.close()
        }
        verifySuspend {
            deleteAccountData.invoke(UserId("test1", "server"))
        }
    }


    @Test
    fun `remove » remove matrix client`() = runTest {
        val cut = createCut()
        settings.update<MatrixMessengerAccountSettingsBase>(UserId("test1", "server")) { it }
        settings.update<MatrixMessengerAccountSettingsBase>(UserId("test2", "server")) { it }
        mutableMatrixClients.value = mapOf(
            UserId("test1", "server") to matrixClientMock1,
            UserId("test2", "server") to matrixClientMock2,
        )

        cut.remove(UserId("test1", "server")) shouldBe Result.success(Unit)

        cut.value shouldBe mapOf(
            UserId("test2", "server") to matrixClientMock2,
        )
        logoutCalled shouldBe false
        settings.value.base.accounts.keys shouldBe setOf(UserId("test2", "server"))
        verify {
            matrixClientMock1.close()
        }
        verifySuspend {
            deleteAccountData.invoke(UserId("test1", "server"))
        }
    }


    private fun TestScope.createCut(): MatrixClients = MatrixClientsImpl(
        factory = matrixClientFactory,
        deleteAccountData = deleteAccountData,
        settings = settings,
        config = MatrixMessengerConfiguration().apply {
            httpClientEngine = MockEngine.create {
                dispatcher = coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
                addHandler {
                    respond("")
                }
            }
        },
        coroutineScope = backgroundScope + ImmediateDispatcherElement(testDispatcher),
        matrixClients = mutableMatrixClients,
    )
}
