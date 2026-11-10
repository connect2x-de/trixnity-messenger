package de.connect2x.trixnity.messenger.viewmodel.roomlist

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.adapters.SpaceRoomListViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.logic.SelectedRoomIdLogic
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.spaces.RoomListSpace
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceBrowserItem
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceContentViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceRoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.spaces.SpaceRoomListViewModelFactory
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RoomListViewModelFactoryTest {
    @Test
    fun `standard overrides remain callable and space adapters use their injected factory`() = runTest {
        val expected = mock<RoomListViewModel>()
        val factory =
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
                ): RoomListViewModel = expected
            }
        val actual =
            factory.create(
                viewModelContext = mock(),
                selectedRoomId = mock(),
                onRoomSelected = mock(),
                onStartCreateNewRoom = mock(),
                onUserSettingsSelected = mock(),
                onShowAccounts = mock(),
                onOpenAppInfo = mock(),
                onSendLogs = mock(),
                onAccountSelected = mock(),
                onStartVerification = mock(),
                onCloseRoom = mock(),
            )
        assertSame(expected, actual)
        val account = UserId("alice", "example.org")
        val space = RoomListSpace(account, RoomId("!space:example.org"))
        val requests = mutableListOf<RoomListSpace>()
        val browser = mock<SpaceContentViewModel>()
        val spaceFactory =
            object : SpaceRoomListViewModelFactory {
                override fun create(
                    viewModelContext: ViewModelContext,
                    space: RoomListSpace,
                    selectedRoomId: StateFlow<RoomId?>,
                    exploreSelected: StateFlow<Boolean>,
                    onOpenExplore: () -> Unit,
                    onSelect: (SpaceBrowserItem) -> Unit,
                    onBack: () -> Unit,
                    onRoomSelected: (UserId, RoomId) -> Unit,
                    onStartCreateNewRoom: (UserId) -> Unit,
                    onUserSettingsSelected: () -> Unit,
                    onShowAccounts: () -> Unit,
                    onOpenAppInfo: () -> Unit,
                    onSendLogs: () -> Unit,
                    onAccountSelected: (UserId) -> Unit,
                    onSpaceUnavailable: () -> Unit,
                    onStartVerification: (UserId) -> Unit,
                    onCloseRoom: () -> Unit,
                ): SpaceRoomListViewModel {
                    requests += space
                    return object : SpaceRoomListViewModel {
                        override val content = browser
                        override val exploreSelected = exploreSelected

                        override fun openExplore() = onOpenExplore()

                        override val rooms = expected
                    }
                }
            }
        val navigation =
            RouteNavigation(
                object : InitialRoutes {
                    override val routes = listOf(RoomListRoute, SpaceRoomListRoute(account, space.roomId))
                }
            )
        val di = koinApplication { modules(module { single<BackHandler> { BackHandlerImpl() } }) }.koin
        val adapter =
            SpaceRoomListViewModelFactoryAdapter(
                spaceFactory,
                navigation,
                mock(),
                object : SelectedRoomIdLogic {
                    override val selectedRoomId: RoomId? = null

                    override fun selectedRoomIdFlow() = flowOf<RoomId?>(null)
                },
            )
        val result = adapter.create(parametersOf(testViewModelContext(di), SpaceRoomListRoute(account, space.roomId)))
        assertSame(expected, result.rooms)
        assertSame(browser, result.content)
        assertEquals(listOf(space), requests)
    }
}
