package de.connect2x.trixnity.messenger.viewmodel

import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.LoadStoreException.StoreAccessException
import de.connect2x.trixnity.messenger.LoadStoreException.StoreLockedException
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType.User
import net.folivo.trixnity.core.model.RoomId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMatrixClientServiceTest : ShouldSpec() {
    override fun timeout(): Long = 5_000

    val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var matrixClientMock2: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var roomServiceMock2: RoomService

    @Mock
    lateinit var matrixClientFactory: MatrixClientFactory

    private val roomsFlow = MutableStateFlow<Map<RoomId, StateFlow<Room?>>>(mapOf())

    private lateinit var di: Koin
    private var loginCalled = false
    private var initFromStoreCalled = false

    private lateinit var login: Mocker.EverySuspend<Result<MatrixClient?>>
    private lateinit var initFromStore: Mocker.EverySuspend<Result<MatrixClient?>>

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)
            di = koinApplication {
                modules(
                    module {
                        single<GetAccountNames> {
                            object : GetAccountNames {
                                val accounts = mutableListOf<String>()
                                override suspend fun invoke(): List<String> {
                                    return accounts
                                }
                            }
                        }
                    }
                )
            }.koin
            loginCalled = false
            initFromStoreCalled = false

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                        }
                    )
                }.koin
                login = everySuspending { matrixClientFactory.login(isAny(), isAny(), isAny(), isAny(), isAny()) }
                login runs {
                    loginCalled = true
                    Result.success(matrixClientMock)
                }
                initFromStore = everySuspending { matrixClientFactory.initFromStore(isAny()) }
                initFromStore runs {
                    initFromStoreCalled = true
                    Result.success(matrixClientMock)
                }

                every { roomServiceMock.getAll() } returns roomsFlow
            }
        }

        should("login and register new account locally") {
            val cut = defaultMatrixClientService(di)

            val result = cut.login(Url("https://example.org"), User("user"), "password", "", "test1")
            result shouldBe Result.success(Unit)
            cut.matrixClients.value shouldBe listOf(
                NamedMatrixClient(
                    accountName = "test1",
                    MutableStateFlow(matrixClientMock),
                )
            )
            loginCalled shouldBe true
        }

        should("login for another account and create additional MatrixClient") {
            val cut = defaultMatrixClientService(di)

            cut.login(Url("https://example.org"), User("user"), "password", "", "test1")
            cut.login(Url("https://example2.org"), User("user2"), "password2", "", "test2")

            cut.matrixClients.value shouldBe listOf(
                NamedMatrixClient(
                    accountName = "test1",
                    MutableStateFlow(matrixClientMock),
                ),
                NamedMatrixClient(
                    accountName = "test2",
                    MutableStateFlow(matrixClientMock),
                )
            )
        }

        should("not login again, if MatrixClient already present for account") {
            val cut = defaultMatrixClientService(di)

            val result = cut.login(Url("https://example.org"), User("user"), "password", "", "test1")
            result shouldBe Result.success(Unit)
            loginCalled = false
            cut.login(Url("https://example.org"), User("user"), "password", "", "test1")
            loginCalled shouldBe false // use the existing MatrixClient and do not log in again
        }

        should("return exception in Result if login is not possible") {
            val exception = IllegalHeaderValueException("header", 0)
            val cut = defaultMatrixClientService(di)
            login returns Result.failure(exception)

            val result = cut.login(Url("https://example.org"), User("user"), "password", "", "test1")
            result shouldBe Result.failure(exception)
        }

        should("init from the store") {
            val cut = defaultMatrixClientService(di)
            val result = cut.initFromStore("test1")

            result shouldBe Result.success(true)
            cut.matrixClients.value shouldBe listOf(
                NamedMatrixClient(
                    accountName = "test1",
                    MutableStateFlow(matrixClientMock),
                )
            )
            initFromStoreCalled shouldBe true
        }

        should("return 'false' when init from store is not possible") {
            initFromStore runs {
                initFromStoreCalled = true
                Result.success(null)
            }

            val cut = defaultMatrixClientService(di)
            val result = cut.initFromStore("test1")

            result shouldBe Result.success(false)
            cut.matrixClients.value shouldBe listOf()
            initFromStoreCalled shouldBe true
        }

        should("not init from store when matrix client is present already") {
            val cut = defaultMatrixClientService(di)
            cut.matrixClients.value =
                listOf(
                    NamedMatrixClient(
                        accountName = "test1",
                        MutableStateFlow(matrixClientMock),
                    )
                )
            val result = cut.initFromStore("test1")

            result shouldBe Result.success(true)
            initFromStoreCalled shouldBe false
        }

        should("remove matrix clients of all accounts on destroy") {
            val cut = defaultMatrixClientService(di)
            cut.matrixClients.value = listOf(
                NamedMatrixClient(
                    accountName = "test1",
                    MutableStateFlow(matrixClientMock),
                ),
                NamedMatrixClient(
                    accountName = "test2",
                    MutableStateFlow(matrixClientMock),
                ),
                NamedMatrixClient(
                    accountName = "test3",
                    MutableStateFlow(matrixClientMock),
                ),
            )

            val namedMatrixClient = cut.matrixClients.value[0]
            val job = launch {
                namedMatrixClient.matrixClient.first { it == null }
            }

            cut.destroy()

            cut.matrixClients.value shouldBe listOf()
            job.join()
        }

        should("throw StoreLockedException when the underlying exception says the store is locked") {
            initFromStore runs {
                initFromStoreCalled = true
                Result.failure(StoreAccessException(RuntimeException(RuntimeException("The database is locked."))))
            }
            val cut = defaultMatrixClientService(di)
            val result = cut.initFromStore("test1")

            result.exceptionOrNull() should beOfType<StoreLockedException>()
        }

        should("throw StoreAccessException when the underlying exception is a StoreException and the store is not locked") {
            initFromStore runs {
                initFromStoreCalled = true
                Result.failure(StoreAccessException(RuntimeException("Oh no!")))
            }
            val cut = defaultMatrixClientService(di)
            val result = cut.initFromStore("test1")

            result.exceptionOrNull() should beOfType<StoreAccessException>()
        }

        should("rethrow original exception from the MatrixClient if it has nothing to do with the store") {
            initFromStore runs {
                initFromStoreCalled = true
                Result.failure(IllegalStateException("Oh no!"))
            }
            val cut = defaultMatrixClientService(di)
            val result = cut.initFromStore("test1")

            result.exceptionOrNull() should beOfType<IllegalStateException>()
        }

        should("change the notification count when rooms change") {
            mocker.every { matrixClientMock2.di } returns koinApplication {
                modules(
                    module {
                        single { roomServiceMock2 }
                    }
                )
            }.koin
            val roomsFlow2 = MutableStateFlow<Map<RoomId, StateFlow<Room?>>>(mapOf())
            mocker.every { roomServiceMock2.getAll() } returns roomsFlow2
            val cut = defaultMatrixClientService(di)
            val job = launch {
                cut.notificationCount.collect {} // to subscribe the whole time
            }

            cut.login(Url("https://example.org"), User("user"), "password", "", "test1")
            login returns Result.success(matrixClientMock2)
            cut.login(Url("https://example2.org"), User("user2"), "password2", "", "test2")

            val roomId1 = RoomId("room1", "localhost")
            val roomId2 = RoomId("room2", "localhost")
            cut.notificationCount.value shouldBe 0

            roomsFlow.value = mapOf(
                roomId1 to MutableStateFlow(
                    Room(
                        roomId1,
                        unreadMessageCount = 5,
                    )
                ),
                roomId2 to MutableStateFlow(
                    Room(
                        roomId2,
                        unreadMessageCount = 17,
                    )
                )
            )

            eventually(2.seconds) {
                cut.notificationCount.value shouldBe 22
            }

            roomsFlow2.value = mapOf(
                roomId1 to MutableStateFlow(
                    Room(
                        roomId1,
                        unreadMessageCount = 2,
                    )
                )
            )

            eventually(2.seconds) {
                cut.notificationCount.value shouldBe 24
            }

            job.cancel()
        }
    }

    private fun defaultMatrixClientService(di: Koin) = DefaultMatrixClientService(
        repositoriesModuleCreation = { createInMemoryRepositoriesModule() },
        matrixClientFactory = { matrixClientFactory },
    )
}