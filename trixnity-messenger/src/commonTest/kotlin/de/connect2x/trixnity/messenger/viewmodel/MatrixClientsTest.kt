package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.LoadStoreException.StoreLockedException
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.AuthenticationApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType.User
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class MatrixClientsTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    val mocker = Mocker()

    private lateinit var mutableMatrixClients: MutableStateFlow<Map<UserId, MatrixClient>>
    private lateinit var loginState: MutableStateFlow<MatrixClient.LoginState>

    @Mock
    lateinit var matrixClientMock1: MatrixClient

    @Mock
    lateinit var matrixClientMock2: MatrixClient

    @Mock
    lateinit var matrixClientServerApiClient: MatrixClientServerApiClient

    @Mock
    lateinit var authenticationApiClient: AuthenticationApiClient

    @Mock
    lateinit var matrixClientFactory: MatrixClientFactory

    @Mock
    lateinit var deleteAccountData: DeleteAccountData

    lateinit var settings: MatrixMessengerSettingsHolder

    private var loginCalled = false
    private var logoutCalled = false
    private var initFromStoreCalled = false
    private var initFromStoreCalledCount = 0

    private lateinit var login: Mocker.EverySuspend<Result<MatrixClientFactory.LoginResult>>
    private lateinit var initFromStore: Mocker.EverySuspend<Result<MatrixClient?>>

    init {
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)
            settings = createTestMatrixMessengerSettingsHolder()
            loginCalled = false
            logoutCalled = false
            initFromStoreCalled = false
            initFromStoreCalledCount = 0

            with(mocker) {
                login = everySuspending {
                    matrixClientFactory.login(
                        isAny(),
                        isAny(),
                        isAny(),
                        isAny(),
                        isAny()
                    )
                }

                login runs {
                    val username = checkNotNull((it[1] as? User)?.user)
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        (it[4] as? suspend (MatrixClient.LoginInfo) -> Unit)
                            ?.invoke(MatrixClient.LoginInfo(UserId(username, "server"), "", ""))
                        loginCalled = true
                        MatrixClientFactory.LoginResult(
                            when (username) {
                                "test1" -> matrixClientMock1
                                "test2" -> matrixClientMock2
                                else -> fail("username $username not supported in login")
                            }, null
                        )
                    }
                }
                initFromStore = everySuspending { matrixClientFactory.initFromStore(isAny(), isAny()) }
                initFromStore runs {
                    val username = checkNotNull((it[0] as? UserId)).localpart
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
                everySuspending { matrixClientMock1.logout() } runs {
                    logoutCalled = true
                    Result.success(Unit)
                }
                every { matrixClientServerApiClient.authentication } returns authenticationApiClient
                every { matrixClientServerApiClient.accessToken } returns MutableStateFlow(null)
                everySuspending { authenticationApiClient.logout(isAny()) } returns Result.success(Unit)
                everySuspending { matrixClientMock1.stop(isAny()) } returns Unit
                everySuspending { matrixClientMock2.stop(isAny()) } returns Unit

                everySuspending { deleteAccountData.invoke(isAny()) } returns Unit
            }
            mutableMatrixClients = MutableStateFlow(mapOf())
        }

        context("login") {
            should("login and register new account locally") {
                val cut = createCut()
                val result = cut.login(Url("https://example.org"), User("test1"), "password", "")
                result.isSuccess shouldBe true
                cut.value shouldBe mapOf(UserId("test1", "server") to matrixClientMock1)
                loginCalled shouldBe true

                cancelNeverEndingCoroutines()
            }
            should("login for another account and create additional MatrixClient") {
                val cut = createCut()
                cut.login(Url("https://example.org"), User("test1"), "password", "")
                cut.login(Url("https://example2.org"), User("test2"), "password2", "")

                cut.value shouldBe mapOf(
                    UserId("test1", "server") to matrixClientMock1,
                    UserId("test2", "server") to matrixClientMock2,
                )

                cancelNeverEndingCoroutines()
            }
            should("not login again, if MatrixClient already present for account") {
                val cut = createCut()
                cut.login(Url("https://example.org"), User("test1"), "password", "").isSuccess shouldBe true
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
                initFromStore runs {
                    initFromStoreCalled = true
                    Result.success(null)
                }

                val result = cut.initFromStore()

                result shouldBe MatrixClients.InitFromStoreResult(setOf(), mapOf(UserId("test1", "server") to null))
                cut.value shouldBe mapOf()
                initFromStoreCalled shouldBe true

                cancelNeverEndingCoroutines()
            }
            should("has failure on exception") {
                val cut = createCut()
                settings.update(UserId("test1", "server")) { it }
                initFromStore runs {
                    Result.failure(StoreLockedException("The database is locked."))
                }
                cut.initFromStore() shouldBe MatrixClients.InitFromStoreResult(
                    success = setOf(),
                    failures = mapOf(UserId("test1", "server") to StoreLockedException("The database is locked."))
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
                mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
                    matrixClientMock1.stop(isAny())
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
                mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
                    matrixClientMock1.stop(isAny())
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
                mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
                    matrixClientMock1.stop(isAny())
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
            config = MatrixMessengerConfiguration(),
            coroutineScope = CoroutineScope(currentCoroutineContext()),
            matrixClients = mutableMatrixClients,
            matrixClientServerApiClientFactory = { _, _ -> matrixClientServerApiClient },
        )
}
