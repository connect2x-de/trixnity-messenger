package de.connect2x.trixnity.messenger.viewmodel.spaces

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.adapters.SpaceBrowserViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.viewModelFactoryAdapterOf
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.internal.routes.spaces.SpaceBrowserRoute
import de.connect2x.trixnity.messenger.testViewModelContext
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SpaceAccountNavigationTest {
    private fun contentFactory(): SpaceContentViewModelFactory {
        val factory = mock<SpaceContentViewModelFactory>()
        every { factory.create(any(), any(), any(), any(), any(), any()) } returns mock()
        return factory
    }

    @Test
    fun `space account registration does not replace the regular account view model`() = runTest {
        val regular =
            object : AccountViewModel by mock<AccountViewModel>() {
                override val activeAccount = MutableStateFlow<UserId?>(null)
            }
        val factory = mock<AccountViewModelFactory>()
        every { factory.create(any(), any(), any(), any(), any()) } returns regular
        val navigation =
            RouteNavigation(
                object : InitialRoutes {
                    override val routes: List<Route> = listOf(RoomListRoute, SpaceBrowserRoute)
                }
            )
        val di =
            koinApplication {
                    allowOverride(false)
                    modules(
                        module {
                            single<AccountViewModel> { regular }
                            single { factory }
                            single<SpaceContentViewModelFactory> { contentFactory() }
                            single<SpaceBrowserViewModelFactory> { SpaceBrowserViewModelFactory }
                            single { navigation }
                            single<BackHandler> { BackHandlerImpl() }
                            viewModelFactoryAdapterOf(::SpaceBrowserViewModelFactoryAdapter)
                        }
                    )
                }
                .koin
        try {
            val context = testViewModelContext(di)
            val spaces = di.get<SpaceBrowserViewModel> { parametersOf(SpaceBrowserRoute, context) }.account
            assertSame(regular, di.get<AccountViewModel>())
            assertSame(regular.activeAccount, spaces.activeAccount)
        } finally {
            di.close()
        }
    }

    @Test
    fun `browser content and account scopes close with their shared owner`() = runTest {
        val contexts = mutableListOf<ViewModelContext>()
        val contentFactory = mock<SpaceContentViewModelFactory>()
        every { contentFactory.create(any(), any(), any(), any(), any(), any()) } calls
            { args ->
                contexts += args.arg<ViewModelContext>(0)
                mock<SpaceContentViewModel>()
            }
        val accountFactory = mock<AccountViewModelFactory>()
        val account = mock<AccountViewModel>()
        every { account.activeAccount } returns MutableStateFlow<UserId?>(null)
        every { accountFactory.create(any(), any(), any(), any(), any()) } calls
            { args ->
                contexts += args.arg<ViewModelContext>(0)
                account
            }
        val di =
            koinApplication {
                    modules(
                        module {
                            single<BackHandler> { BackHandlerImpl() }
                            single<SpaceContentViewModelFactory> { contentFactory }
                            single<AccountViewModelFactory> { accountFactory }
                        }
                    )
                }
                .koin
        val owner = InstanceKeeperDispatcher()
        val context =
            ViewModelContextImpl(
                di,
                DefaultComponentContext(LifecycleRegistry(), instanceKeeper = owner),
                backgroundScope.coroutineContext,
                "Browser",
            )
        try {
            SpaceBrowserViewModelFactory.create(context, {}, {}, {}, {}, {}, {})
            contexts.size shouldBe 2
            val contentJob = contexts[0].coroutineScope.coroutineContext[Job]!!
            val accountJob = contexts[1].coroutineScope.coroutineContext[Job]!!
            assertNotSame(contentJob, accountJob)
            assertTrue(contentJob.isActive)
            assertTrue(accountJob.isActive)
            owner.destroy()
            assertFalse(contentJob.isActive)
            assertFalse(accountJob.isActive)
        } finally {
            owner.destroy()
            di.close()
        }
    }
}
