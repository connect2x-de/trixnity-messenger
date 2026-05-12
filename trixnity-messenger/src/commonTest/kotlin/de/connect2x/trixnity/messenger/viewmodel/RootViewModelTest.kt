package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test

class RootViewModelTest {

    val roomServiceMock = mock<RoomService> {
        every { getAll() } returns MutableStateFlow(mapOf())
    }

    val userServiceMock = mock<UserService> {
        every { getAllReceipts(any()) } returns MutableStateFlow(mapOf())
    }

    val matrixClientMock = mock<MatrixClient> {
        every { di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                })
        }.koin
        everySuspend { syncOnce<Unit>(any(), any(), any()) } returns Result.success(Unit)
        every { syncState } returns MutableStateFlow(SyncState.STOPPED)
    }

    val downloadManagerMock = mock<DownloadManager>()
    val accountViewModelMock = mock<AccountViewModel>()
    val roomListViewModelMock = mock<RoomListViewModel>()
    val mainViewModelMock = mock<MainViewModel> {
        every { start() } returns Unit
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `show account creation when there is no account defined yet`() = runTest {
        val cut = rootViewModel()
        cut.stack.toFlow().first { it.active.configuration == RootRouter.Config.AddMatrixAccount }
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
    private fun TestScope.rootViewModel(): RootViewModelImpl {
        Dispatchers.setMain(testDispatcher)
        val koinApplication = koinApplication {
            modules(
                createTestDefaultTrixnityMessengerModules() + module {
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
                                onShowAppInfo: () -> Unit,
                                onShowAccounts: () -> Unit
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
                                onShowAccounts: () -> Unit,
                                onOpenAppInfo: () -> Unit,
                                onSendLogs: () -> Unit,
                                onAccountSelected: () -> Unit,
                                onStartVerification: (UserId) -> Unit,
                                onCloseRoom: () -> Unit,
                            ): RoomListViewModel = roomListViewModelMock
                        }
                    }
                })
        }
        return RootViewModelImpl(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            di = koinApplication.koin,
            coroutineContext = backgroundScope.coroutineContext
        )
    }
}
