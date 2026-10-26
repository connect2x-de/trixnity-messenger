package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.adapters.SpaceBrowserViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SpaceExploreViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SpaceNavigationRailViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SpaceRoomListViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.logic.SelectedRoomIdLogic
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.closeRoom
import de.connect2x.trixnity.messenger.internal.navigation.openHome
import de.connect2x.trixnity.messenger.internal.navigation.openRoom
import de.connect2x.trixnity.messenger.internal.navigation.openSpaceBrowser
import de.connect2x.trixnity.messenger.internal.navigation.openSpaceExplore
import de.connect2x.trixnity.messenger.internal.navigation.returnHomeForUnavailableSpace
import de.connect2x.trixnity.messenger.internal.navigation.selectSpace
import de.connect2x.trixnity.messenger.internal.navigation.selectSpaceAccount
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.RoomSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.UserSettingsRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceBrowserRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceExploreRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.internal.workers.FallbackBackCallbackWorker
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceBrowserNavigationTest {
    private val user = UserId("second", "example.org")
    private val parent = SpaceRoomListRoute(user, RoomId("!parent:example.org"))
    private val child = SpaceRoomListRoute(user, RoomId("!child:example.org"))
    private val list = SpaceBrowserRoute
    private val hierarchyModes = mutableListOf<Boolean>()
    private val browserScopes = mutableListOf<Pair<UserId?, RoomId?>>()

    private fun TestScope.browser(
        routes: List<Route>,
        route: Route?,
        backHandler: BackHandler = BackHandlerImpl(),
        onBrowserAccountSelected: ((UserId?) -> Unit) -> Unit = {},
        onSpaceRoomListCreated: (SpaceRoomListViewModel) -> Unit = {},
    ): Pair<SpaceContentViewModel, RouteNavigation> {
        val navigation =
            RouteNavigation(
                object : InitialRoutes {
                    override val routes = routes
                }
            )
        val factory =
            object : SpaceContentViewModelFactory {
                override fun create(
                    viewModelContext: ViewModelContext,
                    userId: UserId?,
                    spaceId: RoomId?,
                    onSelect: (SpaceBrowserItem) -> Unit,
                    onBack: () -> Unit,
                    loadHierarchy: Boolean,
                ): SpaceContentViewModel =
                    object : SpaceContentViewModel {
                        init {
                            hierarchyModes += loadHierarchy
                            browserScopes += userId to spaceId
                        }

                        override val state = MutableStateFlow<SpaceContentState>(SpaceContentState.Loading)

                        override fun select(item: SpaceBrowserItem) = onSelect(item)

                        override fun back() = onBack()

                        override fun refresh() {}

                        override val spaceImage = MutableStateFlow<ByteArray?>(null)

                        override fun avatar(userId: UserId, roomId: RoomId) = MutableStateFlow<ByteArray?>(null)

                        override fun reject(item: SpaceBrowserItem) = Unit

                        override fun join(item: SpaceBrowserItem) = error("Not used in navigation tests")
                    }
            }
        val accountFactory = mock<AccountViewModelFactory>()
        val account = mock<AccountViewModel>()
        every { account.activeAccount } returns MutableStateFlow<UserId?>(null)
        every { accountFactory.create(any(), any(), any(), any(), any()) } calls
            { args ->
                onBrowserAccountSelected(args.arg(1))
                account
            }
        val spaceRoomsFactory =
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
                ): SpaceRoomListViewModel =
                    object : SpaceRoomListViewModel {
                        override val content =
                            factory.create(viewModelContext, space.userId, space.roomId, onSelect, onBack, false)
                        override val exploreSelected = exploreSelected

                        override fun openExplore() = onOpenExplore()

                        override val rooms = mock<RoomListViewModel>()
                    }
            }
        val selectedRoom =
            object : SelectedRoomIdLogic {
                override val selectedRoomId: RoomId? = null

                override fun selectedRoomIdFlow() = flowOf<RoomId?>(null)
            }
        val di =
            koinApplication {
                    modules(
                        module {
                            single<BackHandler> { backHandler }
                            single<SpaceContentViewModelFactory> { factory }
                            single<AccountViewModelFactory> { accountFactory }
                        }
                    )
                }
                .koin
        val worker = FallbackBackCallbackWorker(navigation, backHandler, mock())
        backgroundScope.launch { worker.doWork() }
        runCurrent()
        val parameters = parametersOf(route, testViewModelContext(di))
        val content =
            when (route) {
                null ->
                    SpaceNavigationRailViewModelFactoryAdapter(SpaceNavigationRailViewModelFactory, navigation)
                        .create(parameters)
                is SpaceBrowserRoute ->
                    SpaceBrowserViewModelFactoryAdapter(SpaceBrowserViewModelFactory, navigation)
                        .create(parameters)
                        .content
                is SpaceExploreRoute ->
                    SpaceExploreViewModelFactoryAdapter(SpaceExploreViewModelFactory, navigation).create(parameters)
                is SpaceRoomListRoute ->
                    SpaceRoomListViewModelFactoryAdapter(spaceRoomsFactory, navigation, mock(), selectedRoom)
                        .create(parameters)
                        .also(onSpaceRoomListCreated)
                        .content
                else -> error("Unsupported test route")
            }
        return content to navigation
    }

    @Test
    fun `explore view model opens settings for its account and replaces existing extras`() = runTest {
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val (content, navigation) =
            browser(listOf(RoomListRoute, parent, explore, RoomSettingsRoute(user, child.spaceId)), explore)
        (content as SpaceExploreViewModel).openSettings()
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, explore, RoomSettingsRoute(user, parent.spaceId))
    }

    @Test
    fun `space room list opens explore and follows its selection`() = runTest {
        lateinit var viewModel: SpaceRoomListViewModel
        val (_, navigation) =
            browser(listOf(RoomListRoute, parent), parent, onSpaceRoomListCreated = { viewModel = it })
        var selected = false
        backgroundScope.launch { viewModel.exploreSelected.collect { selected = it } }
        runCurrent()
        selected shouldBe false
        viewModel.openExplore()
        runCurrent()
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, SpaceExploreRoute(user, parent.spaceId))
        selected shouldBe true
        navigation.updateNavigation { openRoom(user, RoomId("!room:example.org")) }
        runCurrent()
        selected shouldBe false
    }

    @Test
    fun `a departed space removes its list and explore without cancelling settings`() = runTest {
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val settings = RoomSettingsRoute(user, parent.spaceId)
        val (content, navigation) = browser(listOf(RoomListRoute, list, parent, explore, settings), parent)
        content.back()
        navigation.routes.value shouldBe listOf(RoomListRoute, list, settings)
        navigation.updateNavigation { closeRoom() }
        navigation.routes.value shouldBe listOf(RoomListRoute, list)
    }

    @Test
    fun `a departed ancestor does not close the current subspace`() = runTest {
        val explore = SpaceExploreRoute(user, child.spaceId)
        val settings = RoomSettingsRoute(user, child.spaceId)
        val (content, navigation) = browser(listOf(RoomListRoute, parent, child, explore, settings), parent)
        content.back()
        navigation.routes.value shouldBe listOf(RoomListRoute, child, explore, settings)
    }

    @Test
    fun `delayed overview account selection cannot replace a newer destination`() = runTest {
        val destinations: List<List<Route>> =
            listOf(
                listOf(RoomListRoute),
                listOf(RoomListRoute, list, parent),
                listOf(RoomListRoute, list, parent, SpaceExploreRoute(user, parent.spaceId)),
                listOf(RoomListRoute, list, UserSettingsRoute),
            )
        for (destination in destinations) {
            var onAccountSelected: ((UserId?) -> Unit)? = null
            val (_, navigation) =
                browser(listOf(RoomListRoute, list), list, onBrowserAccountSelected = { onAccountSelected = it })
            navigation.updateNavigation { items = destination }
            requireNotNull(onAccountSelected)(user)
            navigation.routes.value shouldBe destination
        }
    }

    @Test
    fun `switching space accounts clears the old account routes and preserves the explore selection`() = runTest {
        val target = UserId("other", "example.org")
        for (detail in
            listOf(null, SpaceExploreRoute(user, parent.spaceId), TimelineRoute(user, RoomId("!room:example.org")))) {
            val initial = listOf(RoomListRoute, parent) + listOfNotNull(detail)
            val (_, navigation) = browser(initial, parent)
            navigation.updateNavigation { selectSpaceAccount(parent, target, showExplore = detail != null) }
            navigation.routes.value shouldBe
                listOf(RoomListRoute, SpaceRoomListRoute(target, parent.spaceId)) +
                    if (detail != null) listOf(SpaceExploreRoute(target, parent.spaceId)) else emptyList()
            navigation.updateNavigation { returnHomeForUnavailableSpace(SpaceRoomListRoute(target, parent.spaceId)) }
            navigation.routes.value shouldBe listOf(RoomListRoute)
        }
    }

    @Test
    fun `late account switches cannot override a newer space or account destination`() = runTest {
        val replacement = SpaceRoomListRoute(UserId("other", "example.org"), parent.spaceId)
        val destinations: List<List<Route>> =
            listOf(
                listOf(RoomListRoute, parent, child),
                listOf(RoomListRoute, parent, child, SpaceExploreRoute(user, child.spaceId)),
                listOf(RoomListRoute, replacement),
                listOf(RoomListRoute, parent, SpaceBrowserRoute),
                listOf(RoomListRoute),
            )
        for (initial in destinations) {
            val (_, navigation) = browser(initial, null)
            navigation.updateNavigation { selectSpaceAccount(parent, user, showExplore = true) }
            navigation.routes.value shouldBe initial
            navigation.updateNavigation { returnHomeForUnavailableSpace(parent) }
            navigation.routes.value shouldBe initial
        }
    }

    @Test
    fun `opening the overview replaces space history and preserves the home anchor`() = runTest {
        val (_, navigation) =
            browser(listOf(RoomListRoute, parent, child, SpaceExploreRoute(user, child.spaceId)), null)
        navigation.updateNavigation { openSpaceBrowser() }
        navigation.routes.value shouldBe listOf(RoomListRoute, SpaceBrowserRoute)
        navigation.updateNavigation { openSpaceBrowser() }
        navigation.routes.value shouldBe listOf(RoomListRoute, SpaceBrowserRoute)
    }

    @Test
    fun `selecting a space without explore preserves the open room`() = runTest {
        val timeline = TimelineRoute(user, RoomId("!room:example.org"))
        val (_, navigation) = browser(listOf(RoomListRoute, parent, timeline), null)
        navigation.updateNavigation { selectSpace(child, showExplore = false) }
        navigation.routes.value shouldBe listOf(RoomListRoute, timeline, child)
        navigation.updateNavigation { openHome() }
        navigation.routes.value shouldBe listOf(RoomListRoute, timeline)
    }

    @Test
    fun `overview route serializes as a singleton`() {
        val overview = Json.encodeToString(SpaceBrowserRoute.serializer(), SpaceBrowserRoute)
        Json.decodeFromString(SpaceBrowserRoute.serializer(), overview) shouldBe SpaceBrowserRoute
    }

    @Test
    fun `selecting a space from the overview opens its room list`() = runTest {
        val (vm, navigation) = browser(listOf(RoomListRoute, list), list)
        vm.select(SpaceBrowserItem(user, child.spaceId, "Child", isSpace = true, joined = true))
        navigation.routes.value shouldBe listOf(RoomListRoute, list, child)
    }

    @Test
    fun `back from a selected space returns to the overview`() = runTest {
        val before = listOf(RoomListRoute, list)
        val handler = BackHandlerImpl()
        val (_, navigation) = browser(before + child, child, handler)
        handler.goBack()
        runCurrent()
        navigation.routes.value shouldBe before
    }

    @Test
    fun `selecting an unjoined discovered parent leaves navigation unchanged`() = runTest {
        val (browser, navigation) =
            browser(listOf(RoomListRoute, parent, SpaceExploreRoute(user, parent.spaceId)), null)
        browser.select(SpaceBrowserItem(user, child.spaceId, "Related space", isSpace = true, joined = false))
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, SpaceExploreRoute(user, parent.spaceId))
    }

    @Test
    fun `system back pops the active space destination only`() = runTest {
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val settings = RoomSettingsRoute(user, explore.spaceId)
        for (destination in listOf(parent, explore)) {
            val before =
                when (destination) {
                    parent -> listOf(RoomListRoute)
                    explore -> listOf(RoomListRoute, parent)
                    else -> listOf(RoomListRoute, parent, explore)
                }
            val handler = BackHandlerImpl()
            val (_, navigation) = browser(before + destination, destination, handler)
            handler.goBack()
            runCurrent()
            navigation.routes.value shouldBe before
        }
        val handler = BackHandlerImpl()
        val routes = listOf(RoomListRoute, parent, explore, settings)
        val (_, navigation) = browser(routes, explore, handler)
        handler.goBack()
        runCurrent()
        navigation.routes.value shouldBe routes.dropLast(1)
    }

    @Test
    fun `space changes and room navigation clear settings`() = runTest {
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val settings = RoomSettingsRoute(user, explore.spaceId)
        val initial = listOf(RoomListRoute, parent, explore, settings)
        val (_, allRoomsNavigation) = browser(initial, explore)
        allRoomsNavigation.updateNavigation { openHome() }
        allRoomsNavigation.routes.value shouldBe listOf(RoomListRoute)
        val (_, spaceNavigation) = browser(initial, explore)
        spaceNavigation.updateNavigation { selectSpace(child, showExplore = true) }
        spaceNavigation.routes.value shouldBe listOf(RoomListRoute, child, SpaceExploreRoute(user, child.spaceId))
        val (exploreBrowser, childNavigation) = browser(initial, explore)
        exploreBrowser.select(SpaceBrowserItem(user, child.spaceId, "Child", true, true))
        childNavigation.routes.value shouldBe
            listOf(RoomListRoute, parent, child, SpaceExploreRoute(user, child.spaceId))
        val (_, roomNavigation) = browser(initial, explore)
        val room = RoomId("!room:example.org")
        roomNavigation.updateNavigation { openRoom(user, room) }
        roomNavigation.routes.value shouldBe listOf(RoomListRoute, parent, TimelineRoute(user, room))
    }

    @Test
    fun `each browser context uses its account space and hierarchy mode`() = runTest {
        browser(listOf(RoomListRoute), null)
        browser(listOf(RoomListRoute, list), list)
        browser(listOf(RoomListRoute, parent), parent)
        browser(listOf(RoomListRoute, parent), SpaceExploreRoute(user, parent.spaceId))
        hierarchyModes shouldBe listOf(true, true, false, true)
        browserScopes shouldBe listOf(null to null, null to null, user to parent.spaceId, user to parent.spaceId)
    }

    @Test
    fun `explore replaces the timeline and details and opening a room closes explore`() = runTest {
        val roomId = RoomId("!room:example.org")
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val (browser, navigation) =
            browser(
                listOf(RoomListRoute, parent, TimelineRoute(user, roomId), RoomSettingsRoute(user, roomId)),
                explore,
            )
        navigation.updateNavigation { openSpaceExplore(user, explore.spaceId) }
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, explore)
        navigation.updateNavigation { openSpaceExplore(user, explore.spaceId) }
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, explore)
        browser.select(SpaceBrowserItem(user, roomId, "Room", false, true))
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, TimelineRoute(user, roomId))
    }

    @Test
    fun `explore back returns to the space sidebar`() = runTest {
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val (browser, navigation) = browser(listOf(RoomListRoute, parent, explore), explore)
        browser.back()
        navigation.routes.value shouldBe listOf(RoomListRoute, parent)
    }

    @Test
    fun `switching spaces from explore opens the selected spaces explore view`() = runTest {
        val (_, navigation) = browser(listOf(RoomListRoute, parent, SpaceExploreRoute(user, parent.spaceId)), null)
        navigation.updateNavigation { selectSpace(child, showExplore = true) }
        navigation.routes.value shouldBe listOf(RoomListRoute, child, SpaceExploreRoute(user, child.spaceId))
        navigation.updateNavigation { openHome() }
        navigation.routes.value shouldBe listOf(RoomListRoute)
    }

    @Test
    fun `opening a subspace from explore keeps explore in the main pane`() = runTest {
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val (browser, navigation) = browser(listOf(RoomListRoute, parent, explore), explore)
        val childId = child.spaceId
        browser.select(SpaceBrowserItem(user, childId, "Child", true, true))
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, child, SpaceExploreRoute(user, childId))
    }

    @Test
    fun `browser selection preserves the overview for back navigation`() = runTest {
        val (browser, navigation) = browser(listOf(RoomListRoute, list), list)
        browser.select(SpaceBrowserItem(user, parent.spaceId, "Parent", isSpace = true, joined = true))
        navigation.routes.value shouldBe listOf(RoomListRoute, list, parent)
    }

    @Test
    fun `rail selection replaces the open room and details with explore`() = runTest {
        val roomId = RoomId("!room:example.org")
        val timeline = TimelineRoute(user, roomId)
        val (browser, navigation) =
            browser(listOf(RoomListRoute, list, parent, child, timeline, RoomSettingsRoute(user, roomId)), null)
        val selected = SpaceBrowserItem(user, parent.spaceId, "Parent", isSpace = true, joined = true)
        browser.select(selected)
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, SpaceExploreRoute(user, parent.spaceId))
        browser.select(selected)
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, SpaceExploreRoute(user, parent.spaceId))
        val other = UserId("other", "example.org")
        browser.select(selected.copy(userId = other))
        navigation.routes.value shouldBe
            listOf(RoomListRoute, parent.copy(userId = other), SpaceExploreRoute(other, parent.spaceId))
        navigation.updateNavigation { openHome() }
        navigation.routes.value shouldBe listOf(RoomListRoute)
    }

    @Test
    fun `rail invitation opens explore without a joined space room list`() = runTest {
        val (browser, navigation) = browser(listOf(RoomListRoute, child), null)
        browser.select(
            SpaceBrowserItem(user, parent.spaceId, "Invited", isSpace = true, joined = false, invited = true)
        )
        navigation.routes.value shouldBe listOf(RoomListRoute, SpaceExploreRoute(user, parent.spaceId))
    }

    @Test
    fun `selecting the accepted invitation adds its room list beside explore`() = runTest {
        val explore = SpaceExploreRoute(user, parent.spaceId)
        val (browser, navigation) = browser(listOf(RoomListRoute, explore), explore)
        browser.select(SpaceBrowserItem(user, parent.spaceId, "Joined", isSpace = true, joined = true))
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, explore)
    }

    @Test
    fun `following a cycle preserves the open room`() = runTest {
        val timeline = TimelineRoute(user, RoomId("!room:example.org"))
        val (browser, navigation) = browser(listOf(RoomListRoute, parent, child, timeline), child)
        browser.select(SpaceBrowserItem(user, parent.spaceId, "Parent", isSpace = true, joined = true))
        navigation.routes.value shouldBe listOf(RoomListRoute, parent, timeline)
    }

    @Test
    fun `room navigation uses the selected item's account`() = runTest {
        val (browser, navigation) = browser(listOf(RoomListRoute, list, parent), parent)
        val room = SpaceBrowserItem(user, RoomId("!room:example.org"), "Room", isSpace = false, joined = true)
        browser.select(room)
        navigation.routes.value.last() shouldBe TimelineRoute(user, room.roomId)
    }

    @Test
    fun `subspace navigation preserves the parent for back navigation`() = runTest {
        val initial = listOf(RoomListRoute, list, parent)
        val (browser, navigation) = browser(initial, parent)
        browser.select(SpaceBrowserItem(user, child.spaceId, "Child", isSpace = true, joined = true))
        navigation.routes.value shouldBe initial + child
        val (childBrowser, childNavigation) = browser(navigation.routes.value, child)
        childBrowser.back()
        childNavigation.routes.value shouldBe initial
    }

    @Test
    fun `following a cycle returns to the existing ancestor`() = runTest {
        val (browser, navigation) = browser(listOf(RoomListRoute, list, parent, child), child)
        browser.select(SpaceBrowserItem(user, parent.spaceId, "Parent", isSpace = true, joined = true))
        navigation.routes.value shouldBe listOf(RoomListRoute, list, parent)
    }
}
