package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.testMainDispatcher
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

                everySuspending { matrixClientMock.syncOnce<Unit>(isAny(), isAny(), isAny()) } returns
                        Result.success(Unit)

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
                        object : MatrixClientInitializationViewModelFactory {}
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
