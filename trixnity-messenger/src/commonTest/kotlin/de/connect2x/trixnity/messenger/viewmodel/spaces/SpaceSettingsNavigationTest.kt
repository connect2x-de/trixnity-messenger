package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.adapters.SpaceSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.pop
import de.connect2x.trixnity.messenger.internal.routes.extras.AddMembersRoute
import de.connect2x.trixnity.messenger.internal.routes.extras.PowerlevelRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceExploreRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceRoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceSettingsRoute
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.OpenAvatarCutterCallback
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PreviewRoomSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SpaceSettingsNavigationTest {
    @Test
    fun `settings preserve the space and account through members roles and close`() = runTest {
        val user = UserId("alice", "elsewhere.org")
        val space = RoomId("!space:example.org")
        val route = SpaceSettingsRoute(user, space)
        val base = listOf(RoomListRoute, SpaceRoomListRoute(user, space), SpaceExploreRoute(user, space))
        val navigation =
            RouteNavigation(
                object : InitialRoutes {
                    override val routes = base + route
                }
            )
        val detailsFactory =
            object : RoomSettingsViewModelFactory {
                override fun create(
                    viewModelContext: MatrixClientViewModelContext,
                    selectedRoomId: RoomId,
                    onCloseRoom: () -> Unit,
                    onOpenAddMembers: () -> Unit,
                    onOpenDevInfo: () -> Unit,
                    onOpenExportRoom: () -> Unit,
                    onCloseRoomSettings: () -> Unit,
                    onOpenUserProfile: (UserId) -> Unit,
                    onOpenAvatarCutter: OpenAvatarCutterCallback,
                    onOpenPowerLevel: () -> Unit,
                    onOpenMention: OpenMentionCallback,
                ): RoomSettingsViewModel {
                    viewModelContext.userId shouldBe user
                    selectedRoomId shouldBe space
                    return object : RoomSettingsViewModel by PreviewRoomSettingsViewModel() {
                        override fun openAddMembersView() = onOpenAddMembers()

                        override fun openPowerLevelView() = onOpenPowerLevel()

                        override fun close() = onCloseRoomSettings()
                    }
                }
            }
        val di = koinApplication { modules(module { single<BackHandler> { BackHandlerImpl() } }) }.koin
        try {
            val vm =
                SpaceSettingsViewModelFactoryAdapter(SpaceSettingsViewModelFactory, detailsFactory, navigation, mock())
                    .create(parametersOf(route, testViewModelContext(di)))
            vm.details.openAddMembersView()
            navigation.routes.value shouldBe base + route + AddMembersRoute(user, space)
            navigation.updateNavigation { pop(AddMembersRoute(user, space)) }
            vm.details.openPowerLevelView()
            navigation.routes.value shouldBe base + route + PowerlevelRoute(user, space)
            vm.details.close()
            navigation.routes.value shouldBe base
        } finally {
            di.close()
        }
    }
}
