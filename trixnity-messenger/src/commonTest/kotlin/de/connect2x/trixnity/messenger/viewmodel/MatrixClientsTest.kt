package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.LoadStoreException.StoreLockedException
import de.connect2x.trixnity.messenger.util.DeleteAccountData
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.AuthenticationApiClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType.User
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker

@OptIn(ExperimentalCoroutinesApi::class)
class MatrixClientsTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    val mocker = Mocker()

    private lateinit var cut: MatrixClients
    private lateinit var mutableMatrixClients: MutableStateFlow<Map<UserId, MatrixClient>>

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
        Dispatchers.setMain(testMainDispatcher)
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
                everySuspending { matrixClientMock1.logout() } runs {
                    logoutCalled = true
                    Result.success(Unit)
                }
                every { matrixClientServerApiClient.authentication } returns authenticationApiClient
                every { matrixClientServerApiClient.accessToken } returns MutableStateFlow(null)
                everySuspending { authenticationApiClient.logout(isAny()) } returns Result.success(Unit)
                every { matrixClientMock1.stop() } returns Unit
                every { matrixClientMock2.stop() } returns Unit

                everySuspending { deleteAccountData.invoke(isAny()) } returns Unit
            }
            mutableMatrixClients = MutableStateFlow(mapOf())
            cut = MatrixClientsImpl(
                factory = matrixClientFactory,
                deleteAccountData = deleteAccountData,
                settings = settings,
                config = MatrixMessengerConfiguration(),
                matrixClients = mutableMatrixClients,
                matrixClientServerApiClientFactory = { _, _ -> matrixClientServerApiClient },
            )
        }

        context("login") {
            should("login and register new account locally") {
                val result = cut.login(Url("https://example.org"), User("test1"), "password", "")
                result shouldBe Result.success(Unit)
                cut.value shouldBe mapOf(UserId("test1", "server") to matrixClientMock1)
                loginCalled shouldBe true
            }
            should("login for another account and create additional MatrixClient") {
                cut.login(Url("https://example.org"), User("test1"), "password", "")
                cut.login(Url("https://example2.org"), User("test2"), "password2", "")

                cut.value shouldBe mapOf(
                    UserId("test1", "server") to matrixClientMock1,
                    UserId("test2", "server") to matrixClientMock2,
                )
            }
            should("not login again, if MatrixClient already present for account") {
                cut.login(Url("https://example.org"), User("test1"), "password", "") shouldBe
                        Result.success(Unit)
                loginCalled = false
                cut.login(Url("https://example.org"), User("test1"), "password", "") shouldBe
                        Result.failure(AccountAlreadyExistsException(UserId("test1", "server")))
                loginCalled shouldBe false // use the existing MatrixClient and do not log in again
            }
            should("return exception in Result if login is not possible") {
                val exception = IllegalHeaderValueException("header", 0)
                login returns Result.failure(exception)

                val result = cut.login(Url("https://example.org"), User("test1"), "password", "")
                result shouldBe Result.failure(exception)
            }
        }
        context("initFromStore") {
            should("init from the store and settings") {
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
            should("skip init from store when matrix client is already present") {
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
            should("has failure when init from store is not possible") {
                settings.update(UserId("test1", "server")) { it }
                initFromStore runs {
                    initFromStoreCalled = true
                    Result.success(null)
                }

                val result = cut.initFromStore()

                result shouldBe MatrixClients.InitFromStoreResult(setOf(), mapOf(UserId("test1", "server") to null))
                cut.value shouldBe mapOf()
                initFromStoreCalled shouldBe true
            }
            should("has failure on exception") {
                settings.update(UserId("test1", "server")) { it }
                initFromStore runs {
                    Result.failure(StoreLockedException("The database is locked."))
                }
                cut.initFromStore() shouldBe MatrixClients.InitFromStoreResult(
                    success = setOf(),
                    failures = mapOf(UserId("test1", "server") to StoreLockedException("The database is locked."))
                )
            }
        }
        context("logout") {
            should("logout matrix client") {
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
                settings.value.accounts.keys shouldBe setOf(UserId("test2", "server"))
                mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
                    matrixClientMock1.stop()
                    deleteAccountData.invoke(UserId("test1", "server"))
                }
            }
        }
        context("remove") {
            should("remove matrix client") {
                settings.update(UserId("test1", "server")) { it }
                settings.update(UserId("test2", "server")) { it }
                mutableMatrixClients.value = mapOf(
                    UserId("test1", "server") to matrixClientMock1,
                    UserId("test2", "server") to matrixClientMock2,
                )

                cut.remove(UserId("test1", "server")) shouldBe Result.success(Unit)

                cut.value shouldBe mapOf(
                    UserId("test2", "server") to matrixClientMock2,
                )
                logoutCalled shouldBe false
                settings.value.accounts.keys shouldBe setOf(UserId("test2", "server"))
                mocker.verifyWithSuspend(exhaustive = false, inOrder = false) {
                    matrixClientMock1.stop()
                    deleteAccountData.invoke(UserId("test1", "server"))
                }
            }
        }
    }
}