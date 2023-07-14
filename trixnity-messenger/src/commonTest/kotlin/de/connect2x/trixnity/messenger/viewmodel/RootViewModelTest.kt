package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.GetAccountNames
import de.connect2x.trixnity.messenger.MatrixClientService
import de.connect2x.trixnity.messenger.NamedMatrixClient
import de.connect2x.trixnity.messenger.trixnityMessengerModule
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModel
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.assertions.timing.continually
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest : ShouldSpec() {
    private val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var matrixClientServiceMock: MatrixClientService

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var downloadManagerMock: DownloadManager

    @Mock
    lateinit var matrixClientInitializationViewModelMock: MatrixClientInitializationViewModel

    @Mock
    lateinit var accountViewModelMock: AccountViewModel

    @Mock
    lateinit var roomListViewModelMock: RoomListViewModel

    @Mock
    lateinit var mainViewModelMock: MainViewModel

    private val lifecycle = LifecycleRegistry()

    private lateinit var di: Koin

    init {
        Dispatchers.setMain(testMainDispatcher)
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                        }
                    )
                }.koin
                everySuspending { matrixClientServiceMock.initFromStore(isAny()) } returns Result.success(true)
                everySuspending { matrixClientServiceMock.login(isAny(), isAny(), isAny(), isAny(), isAny()) } returns
                        Result.success(Unit)

                everySuspending { matrixClientMock.syncOnce<Unit>(isAny(), isAny()) } returns Result.success(Unit)

                every { roomServiceMock.getAll() } returns MutableStateFlow(mapOf())

                every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STOPPED)

                every { mainViewModelMock.start() } runs {}
            }
        }

        should("show account creation when there is no account defined yet") {
            mocker.every { matrixClientServiceMock.matrixClients } returns MutableStateFlow(emptyList())
            val cut = rootViewModel(emptyList())
            eventually(1.seconds) {
                val config = cut.rootStack.value.active.configuration
                config.shouldBeTypeOf<RootRouter.Config.AddMatrixAccount>()
            }
        }

        should("show initialization after the creation of a new account") {
            val matrixClients = MutableStateFlow<List<NamedMatrixClient>>(emptyList())
            mocker.every { matrixClientServiceMock.matrixClients } returns matrixClients
            val cut = rootViewModel(emptyList())
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.AddMatrixAccount>()
            }

            val matrixClient: MutableStateFlow<MatrixClient?> = MutableStateFlow(null)
            matrixClients.value =
                listOf(NamedMatrixClient(accountName = "test1", matrixClient))
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
            }

            matrixClient.value = matrixClientMock
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.Main>()
            }
        }

        should("show initialization and finally main view when there is at least one account present") {
            val matrixClient: MutableStateFlow<MatrixClient?> = MutableStateFlow(null)
            val matrixClients = MutableStateFlow<List<NamedMatrixClient>>(emptyList())
            mocker.every { matrixClientServiceMock.matrixClients } returns matrixClients
            val cut = rootViewModel(listOf("test1"))
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
            }

            matrixClients.value =
                listOf(NamedMatrixClient(accountName = "test1", matrixClient))
            continually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
            }

            matrixClient.value = matrixClientMock
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.Main>()
            }
        }

        should("initialize MatrixClients for multiple accounts present") {
            val matrixClients = MutableStateFlow<List<NamedMatrixClient>>(emptyList())
            mocker.every { matrixClientServiceMock.matrixClients } returns matrixClients
            val cut = rootViewModel(listOf("test1", "test2", "test3"))
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
            }

            val matrixClient1: MutableStateFlow<MatrixClient?> = MutableStateFlow(null)
            matrixClients.value = listOf(
                NamedMatrixClient(accountName = "test1", matrixClient1),
            )

            continually(500.milliseconds) {
                val config = cut.rootStack.value.active.configuration
                config.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
                config.accountName shouldBe "test1"
            }
            matrixClient1.value = matrixClientMock
            eventually(1.seconds) {
                val config = cut.rootStack.value.active.configuration
                config.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
                config.accountName shouldBe "test2"
            }
            // --- client 1 finished ---

            val matrixClient2: MutableStateFlow<MatrixClient?> = MutableStateFlow(null)
            matrixClients.value = listOf(
                NamedMatrixClient(accountName = "test1", matrixClient1),
                NamedMatrixClient(accountName = "test2", matrixClient2),
            )
            continually(500.milliseconds) {
                val config = cut.rootStack.value.active.configuration
                config.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
                config.accountName shouldBe "test2"
            }
            matrixClient2.value = matrixClientMock
            eventually(1.seconds) {
                val config = cut.rootStack.value.active.configuration
                config.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
                config.accountName shouldBe "test3"
            }
            // --- client 2 finished ---

            val matrixClient3: MutableStateFlow<MatrixClient?> = MutableStateFlow(null)
            matrixClients.value = listOf(
                NamedMatrixClient(accountName = "test1", matrixClient1),
                NamedMatrixClient(accountName = "test2", matrixClient2),
                NamedMatrixClient(accountName = "test3", matrixClient3),
            )
            continually(500.milliseconds) {
                val config = cut.rootStack.value.active.configuration
                config.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
                config.accountName shouldBe "test3"
            }
            matrixClient3.value = matrixClientMock
            // --- client 3 finished ---

            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.Main>()
            }
        }

        should("show the account creation if the user logs out of all accounts") {
            val matrixClients = MutableStateFlow<List<NamedMatrixClient>>(emptyList())
            mocker.every { matrixClientServiceMock.matrixClients } returns matrixClients
            val cut = rootViewModel(listOf("test1", "test2", "test3"))
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.MatrixClientInitialization>()
            }
            matrixClients.value = listOf(
                NamedMatrixClient("test1", MutableStateFlow(matrixClientMock)),
            )
            delay(100.milliseconds)
            matrixClients.value = listOf(
                NamedMatrixClient("test1", MutableStateFlow(matrixClientMock)),
                NamedMatrixClient("test2", MutableStateFlow(matrixClientMock)),
            )
            delay(100.milliseconds)
            matrixClients.value = listOf(
                NamedMatrixClient("test1", MutableStateFlow(matrixClientMock)),
                NamedMatrixClient("test2", MutableStateFlow(matrixClientMock)),
                NamedMatrixClient("test3", MutableStateFlow(matrixClientMock)),
            )
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.Main>()
            }

            di.loadModules(listOf(module {
                single<GetAccountNames> {
                    object : GetAccountNames {
                        override suspend fun invoke(): List<String> {
                            return listOf("test2", "test3")
                        }
                    }
                }
            }))
            matrixClients.value = listOf(
                NamedMatrixClient("test2", MutableStateFlow(matrixClientMock)),
                NamedMatrixClient("test3", MutableStateFlow(matrixClientMock)),
            )
            continually(500.milliseconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.Main>()
            }

            di.loadModules(listOf(module {
                single<GetAccountNames> {
                    object : GetAccountNames {
                        override suspend fun invoke(): List<String> {
                            return listOf()
                        }
                    }
                }
            }))
            matrixClients.value = listOf()
            eventually(1.seconds) {
                cut.rootStack.value.active.configuration.shouldBeTypeOf<RootRouter.Config.AddMatrixAccount>()
            }
        }
    }

    private fun rootViewModel(initialAccountNames: List<String>): RootViewModelImpl {
        val koinApplication = koinApplication {
            modules(
                trixnityMessengerModule(),
                module {
                    single { downloadManagerMock }
                    single<GetAccountNames> {
                        object : GetAccountNames {
                            override suspend fun invoke(): List<String> {
                                return initialAccountNames
                            }
                        }
                    }
                    single<MainViewModelFactory> {
                        object : MainViewModelFactory {
                            override fun newMainViewModel(
                                viewModelContext: ViewModelContext,
                                initialSyncOnceIsFinished: (Boolean) -> Unit,
                                minimizeMessenger: () -> Unit,
                                onCreateNewAccount: () -> Unit,
                                onRemoveAccount: (String) -> Unit,
                            ): MainViewModel = mainViewModelMock
                        }
                    }
                    single<MatrixClientInitializationViewModelFactory> {
                        object : MatrixClientInitializationViewModelFactory {
                            override fun newMatrixClientInitializationViewModel(
                                viewModelContext: ViewModelContext,
                                matrixClientService: MatrixClientService,
                                accountName: String,
                                onInitializationFailure: () -> Unit,
                                onStoreFailure: (Result<Unit>) -> Unit
                            ): MatrixClientInitializationViewModel = matrixClientInitializationViewModelMock
                        }
                    }
                    single<AccountViewModelFactory> {
                        object : AccountViewModelFactory {
                            override fun newAccountViewModel(
                                viewModelContext: ViewModelContext,
                                onAccountSelected: (String?) -> Unit,
                                onUserSettingsSelected: () -> Unit,
                                onShowAppInfo: () -> Unit
                            ): AccountViewModel = accountViewModelMock
                        }
                    }
                    single<RoomListViewModelFactory> {
                        object : RoomListViewModelFactory {
                            override fun newRoomListViewModel(
                                viewModelContext: ViewModelContext,
                                selectedRoomId: StateFlow<RoomId?>,
                                onRoomSelected: (String, RoomId) -> Unit,
                                onCreateNewRoom: (String) -> Unit,
                                onUserSettingsSelected: () -> Unit,
                                onOpenAppInfo: () -> Unit,
                                onSendLogs: () -> Unit,
                                onOpenAccountsOverview: () -> Unit
                            ) = roomListViewModelMock
                        }
                    }
                })
        }
        di = koinApplication.koin
        return RootViewModelImpl(
            componentContext = DefaultComponentContext(lifecycle),
            matrixClientService = matrixClientServiceMock,
            initialSyncOnceIsFinished = {},
            koinApplication = koinApplication,
        )
    }

}
