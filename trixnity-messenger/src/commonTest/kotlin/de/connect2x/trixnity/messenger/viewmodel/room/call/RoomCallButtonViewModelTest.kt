package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.rtc.RtcService
import de.connect2x.trixnity.client.store.ServerData
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.model.media.GetMediaConfig
import de.connect2x.trixnity.clientserverapi.model.server.GetVersions
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.MSC4195
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.LiveKitRtcTransport
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotId
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerExperimentalSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.call.CallServiceImpl
import de.connect2x.trixnity.messenger.call.CallSessionFactory
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.call.testCall
import de.connect2x.trixnity.messenger.call.testCallClients
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.removeCall
import de.connect2x.trixnity.messenger.internal.routes.RoomCallRoute
import de.connect2x.trixnity.messenger.internal.routes.TimelineRoute
import de.connect2x.trixnity.messenger.internal.routes.roomlist.RoomListRoute
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallButtonViewModel.State
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.ktor.http.Url
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(MSC4143::class, MSC4195::class, TrixnityMessengerCallApi::class, TrixnityMessengerPrivateApi::class)
class RoomCallButtonViewModelTest {
    @Test
    fun `room header reopens its call and blocks new calls elsewhere until it ends`() = runTest {
        val user = UserId("alice", "example.org")
        val room = RoomId("!call:example.org")
        val otherRoom = RoomId("!other:example.org")
        val target = CallTarget(user, room, CallRtcApplication.SLOT_ID)
        val chatRoutes = listOf(RoomListRoute, TimelineRoute(user, room))
        val navigation = RouteNavigation(InitialRoutes(chatRoutes))
        val calls = CallServiceImpl(navigation)
        val session = testCall(target)
        val sessionFactory = mock<CallSessionFactory>()
        every { sessionFactory.create(target) } returns session
        val settings = createTestMatrixMessengerSettingsHolder()
        settings.update<MatrixMessengerExperimentalSettings> { it.copy(enableVideoCalls = true) }
        val di =
            koinApplication {
                    modules(
                        module {
                            single { MatrixMessengerConfiguration() }
                            single<MatrixMessengerSettingsHolder> { settings }
                            single<RouteNavigation> { navigation }
                            single<BackHandler> { BackHandlerImpl() }
                            single { testCallClients(MutableStateFlow(mapOf(user to matrixClient()))) }
                        }
                    )
                }
                .koin
        val context = testMatrixClientViewModelContext(di, user)
        val header = RoomCallButtonViewModelImpl(context, room, calls, sessionFactory)
        val otherHeader = RoomCallButtonViewModelImpl(context, otherRoom, calls, sessionFactory)

        delay(2.seconds)
        assertEquals(State.CallAvailable(hasParticipants = false), header.state.value)
        header.join()
        assertSame(session, calls.session.value)
        assertEquals(chatRoutes + RoomCallRoute(target), navigation.routes.value)
        navigation.updateNavigation { removeCall(target) }
        delay(2.seconds)

        assertEquals(State.ActiveCall, header.state.value)
        assertEquals(State.CallInAnotherRoom, otherHeader.state.value)
        otherHeader.join()
        assertEquals(chatRoutes, navigation.routes.value)
        header.join()
        assertEquals(chatRoutes + RoomCallRoute(target), navigation.routes.value)
        assertSame(session, calls.session.value)

        calls.unregister(session)
        delay(2.seconds)
        assertEquals(State.CallAvailable(hasParticipants = false), otherHeader.state.value)

        val breakout = testCall(target.copy(slotId = RtcSlotId("breakout")))
        calls.register(breakout)
        delay(2.seconds)
        assertEquals(State.CallInAnotherRoom, header.state.value)
        header.join()
        header.leave()
        delay(2.seconds)
        assertSame(breakout, calls.session.value)
        assertEquals(chatRoutes, navigation.routes.value)
        calls.unregister(breakout)
    }

    private fun matrixClient(): MatrixClient {
        val rtc = mock<RtcService>()
        every { rtc.getCurrentMemberships() } returns flowOf(emptySet())
        every { rtc.getSlot(any(), any()) } returns flowOf(null)
        every { rtc.getMembers(any(), any()) } returns flowOf(emptyMap())
        val users = mock<UserService>()
        every { users.canSendEvent(any(), any<KClass<out RoomEventContent>>()) } returns flowOf(true)
        val clientDi =
            koinApplication {
                    modules(
                        module {
                            single { rtc }
                            single { users }
                        }
                    )
                }
                .koin
        val client = mock<MatrixClient>()
        every { client.di } returns clientDi
        every { client.serverData } returns
            MutableStateFlow(
                ServerData(
                    versions = GetVersions.Response(),
                    mediaConfig = GetMediaConfig.Response(),
                    capabilities = null,
                    rtcTransports = listOf(LiveKitRtcTransport(Url("https://rtc.example.org"))),
                )
            )
        return client
    }
}
