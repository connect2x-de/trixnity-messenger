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
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
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
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.AuthenticationApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType.User
import net.folivo.trixnity.clientserverapi.model.authentication.Login
import net.folivo.trixnity.core.model.UserId
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class MatrixClientsTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    private lateinit var mutableMatrixClients: MutableStateFlow<Map<UserId, MatrixClient>>
    private lateinit var loginState: MutableStateFlow<MatrixClient.LoginState>

    val matrixClientMock1 = mock<MatrixClient>()

    val matrixClientMock2 = mock<MatrixClient>()

    val matrixClientServerApiClient = mock<MatrixClientServerApiClient>()

    val authenticationApiClient = mock<AuthenticationApiClient>()

    val matrixClientFactory = mock<MatrixClientFactory>()

    val deleteAccountData = mock<DeleteAccountData>()

    lateinit var settings: MatrixMessengerSettingsHolder

    private var loginCalled = false
    private var logoutCalled = false
    private var initFromStoreCalled = false
    private var initFromStoreCalledCount = 0

    private lateinit var login: SuspendAnsweringScope<Result<MatrixClientFactory.LoginResult>>
    private lateinit var initFromStore: SuspendAnsweringScope<Result<MatrixClient?>>

    init {
        coroutineTestScope = true

        beforeTest {
            settings = createTestMatrixMessengerSettingsHolder()
            loginCalled = false
            logoutCalled = false
            initFromStoreCalled = false
            initFromStoreCalledCount = 0

            resetMocks(
                matrixClientMock1,
                matrixClientMock2,
                matrixClientServerApiClient,
                authenticationApiClient,
                matrixClientFactory,
                deleteAccountData
            )

            login = everySuspend {
                matrixClientFactory.loginWith(
                    any(),
                    any(),
                    any(),
                )
            }

            login calls {
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    val loginInfo = (it.args[1] as suspend (MatrixClientServerApiClient) -> MatrixClient.LoginInfo)
                        .invoke(matrixClientServerApiClient)
                    @Suppress("UNCHECKED_CAST")
                    (it.args[2] as? suspend (MatrixClient.LoginInfo) -> Unit)
                        ?.invoke(loginInfo)
                    loginCalled = true
                    val username = loginInfo.userId.localpart
                    MatrixClientFactory.LoginResult(
                        when (username) {
                            "test1" -> matrixClientMock1
                            "test2" -> matrixClientMock2
                            else -> fail("username $username not supported in login")
                        }, null
                    )
                }
            }

            initFromStore = everySuspend { matrixClientFactory.initFromStore(any(), any()) }
            initFromStore calls {
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
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } calls { args ->
                val username = args.args[0] as User
                Result.success(
                    Login.Response(
                        UserId(username.user, "server"),
                        accessToken = "",
                        deviceId = ""
                    )
                )
            }
            every { matrixClientServerApiClient.authentication } returns authenticationApiClient
            every { matrixClientServerApiClient.accessToken } returns MutableStateFlow(null)
            everySuspend { authenticationApiClient.logout(any()) } returns Result.success(Unit)
            every { matrixClientMock1.close() } returns Unit
            every { matrixClientMock2.close() } returns Unit

            everySuspend { deleteAccountData.invoke(any()) } returns Unit
            mutableMatrixClients = MutableStateFlow(mapOf())
        }

        context("login") {
            should("login and register new account locally") {
                val cut = createCut()
                cut.login(Url("https://example.org"), User("test1"), "password", "")
                    .getOrThrow()
                cut.value shouldBe mapOf(UserId("test1", "server") to matrixClientMock1)
                loginCalled shouldBe true

                cancelNeverEndingCoroutines()
            }
            should("login for another account and create additional MatrixClient") {
                val cut = createCut()
                cut.login(Url("https://example.org"), User("test1"), "password", "").getOrThrow()
                cut.login(Url("https://example2.org"), User("test2"), "password2", "").getOrThrow()

                cut.value shouldBe mapOf(
                    UserId("test1", "server") to matrixClientMock1,
                    UserId("test2", "server") to matrixClientMock2,
                )

                cancelNeverEndingCoroutines()
            }
            should("not login again, if MatrixClient already present for account") {
                val cut = createCut()
                cut.login(Url("https://example.org"), User("test1"), "password", "").getOrThrow()
                loginCalled = false
                cut.login(Url("https://example.org"), User("test1"), "password", "") shouldBe
                        Result.failure(AccountAlreadyExistsException(UserId("test1", "server")))
                loginCalled shouldBe false // use the existing MatrixClient and do not log in again

                cancelNeverEndingCoroutines()
            }
            should("return exception in Result if login is not possible") {
                val cut = createCut()
                val exception = IllegalHeaderValueException("header", 0)
                login returns Result.failure(exception)

                val result = cut.login(Url("https://example.org"), User("test1"), "password", "")
                result shouldBe Result.failure(exception)

                cancelNeverEndingCoroutines()
            }
        }
        context("initFromStore") {
            should("init from the store and settings") {
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

                cancelNeverEndingCoroutines()
            }
            should("skip init from store when matrix client is already present") {
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

                cancelNeverEndingCoroutines()
            }
            should("has failure when init from store is not possible") {
                val cut = createCut()
                settings.update(UserId("test1", "server")) { it }
                initFromStore calls {
                    initFromStoreCalled = true
                    Result.success(null)
                }

                val result = cut.initFromStore()

                result shouldBe MatrixClients.InitFromStoreResult(
                    setOf(),
                    mapOf(UserId("test1", "server") to MatrixClientInitializationException.NoDatabaseException)
                )
                cut.value shouldBe mapOf()
                initFromStoreCalled shouldBe true

                cancelNeverEndingCoroutines()
            }
            should("has failure on exception") {
                val cut = createCut()
                settings.update(UserId("test1", "server")) { it }
                initFromStore calls {
                    Result.failure(DatabaseLockedException("The database is locked."))
                }
                cut.initFromStore() shouldBe MatrixClients.InitFromStoreResult(
                    success = setOf(),
                    failures = mapOf(UserId("test1", "server") to DatabaseLockedException("The database is locked."))
                )

                cancelNeverEndingCoroutines()
            }
        }
        context("logout") {
            should("logout matrix client") {
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

                cancelNeverEndingCoroutines()
            }
        }
        context("external logout") {
            should("remove matrix client") {
                val cut = createCut()
                settings.update(UserId("test1", "server")) { it }
                mutableMatrixClients.value = mapOf(
                    UserId("test1", "server") to matrixClientMock1,
                )

                loginState.value = MatrixClient.LoginState.LOGGED_OUT
                testCoroutineScheduler.advanceTimeBy(1.seconds)

                cut.value shouldBe mapOf()
                logoutCalled shouldBe false
                settings.value.base.accounts.keys shouldBe setOf()
                verify {
                    matrixClientMock1.close()
                }
                verifySuspend {
                    deleteAccountData.invoke(UserId("test1", "server"))
                }

                cancelNeverEndingCoroutines()
            }
        }
        context("remove") {
            should("remove matrix client") {
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

                cancelNeverEndingCoroutines()
            }
        }
    }

    suspend fun createCut(): MatrixClients =
        MatrixClientsImpl(
            factory = matrixClientFactory,
            deleteAccountData = deleteAccountData,
            settings = settings,
            config = MatrixMessengerConfiguration().apply {
                httpClientEngine = MockEngine { respond("") }
            },
            coroutineScope = CoroutineScope(currentCoroutineContext()),
            matrixClients = mutableMatrixClients,
        )
}
