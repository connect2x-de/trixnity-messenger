package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.SyncState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.kodein.mock.Mock
import org.kodein.mock.Mocker
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
class RootViewModelTest : ShouldSpec() {
    private val mocker = Mocker()

    @Mock
    lateinit var matrixClientMock: MatrixClient

    @Mock
    lateinit var roomServiceMock: RoomService

    @Mock
    lateinit var userServiceMock: UserService

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
        coroutineTestScope = true

        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            with(mocker) {
                every { matrixClientMock.di } returns koinApplication {
                    modules(
                        module {
                            single { roomServiceMock }
                            single { userServiceMock }
                        }
                    )
                }.koin
                everySuspending { matrixClientMock.syncOnce<Unit>(isAny(), isAny(), isAny()) } returns
                        Result.success(Unit)

                every { roomServiceMock.getAll() } returns MutableStateFlow(mapOf())
                every { userServiceMock.getAllReceipts(isAny()) } returns MutableStateFlow(mapOf())

                every { matrixClientMock.syncState } returns MutableStateFlow(SyncState.STOPPED)

                every { mainViewModelMock.start() } runs {}
            }
        }

        should("show account creation when there is no account defined yet") {
            val cut = rootViewModel()
            testCoroutineScheduler.advanceUntilIdle()
            cut.stack.toFlow().first { it.active.configuration == RootRouter.Config.AddMatrixAccount }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun rootViewModel(): RootViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        val koinApplication = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules() +
                        module {
                            single { downloadManagerMock }
                            single<MainViewModelFactory> {
                                object : MainViewModelFactory {
                                    override fun create(
                                        viewModelContext: ViewModelContext,
                                        onCreateNewAccount: () -> Unit,
                                        onRemoveAccount: (userId: UserId) -> Unit
                                    ): MainViewModel = mainViewModelMock
                                }
                            }
                            single<MatrixClientInitializationViewModelFactory> {
                                object : MatrixClientInitializationViewModelFactory {}
                            }
                            single<AccountViewModelFactory> {
                                object : AccountViewModelFactory {
                                    override fun create(
                                        viewModelContext: ViewModelContext,
                                        onAccountSelected: (UserId?) -> Unit,
                                        onUserSettingsSelected: () -> Unit,
                                        onShowAppInfo: () -> Unit
                                    ): AccountViewModel = accountViewModelMock
                                }
                            }
                            single<RoomListViewModelFactory> {
                                object : RoomListViewModelFactory {
                                    override fun create(
                                        viewModelContext: ViewModelContext,
                                        selectedRoomId: StateFlow<RoomId?>,
                                        onRoomSelected: (UserId, RoomId) -> Unit,
                                        onStartCreateNewRoom: (UserId) -> Unit,
                                        onUserSettingsSelected: () -> Unit,
                                        onOpenAppInfo: () -> Unit,
                                        onSendLogs: () -> Unit,
                                        onOpenAccounts: () -> Unit
                                    ): RoomListViewModel = roomListViewModelMock
                                }
                            }
                        })
        }
        di = koinApplication.koin
        return RootViewModelImpl(
            componentContext = DefaultComponentContext(lifecycle),
            di = koinApplication.koin,
        )
    }
}
