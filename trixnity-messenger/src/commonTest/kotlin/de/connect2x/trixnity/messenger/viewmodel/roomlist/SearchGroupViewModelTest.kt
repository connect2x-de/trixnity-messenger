package de.connect2x.trixnity.messenger.viewmodel.roomlist

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.RoomApiClient
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.coroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class SearchGroupViewModelTest {
    private val roomId = RoomId("room1", "localhost")
    private val room = Room(roomId)
    private val group = SearchGroupViewModel.SearchGroup(
        roomId = roomId,
        groupName = roomId.full,
        topic = null,
        image = MutableStateFlow(null),
        initials = Initials.compute(roomId.full),
        joinedMembersCount = 0L,
        joinRule = JoinRule.Unknown("null")
    )

    private val matrixClientMock = mock<MatrixClient>()
    private val matrixApiClientMock = mock<MatrixClientServerApiClient>()
    private val roomApiClientMock = mock<RoomApiClient>()
    private val roomServiceMock = mock<RoomService>()
    private val i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.of("CET"),
    ) {}
    val onGroupJoinedMock: (UserId, RoomId) -> Unit = mock()
    var joined = false
    val onGroupKnockedMock: (RoomId) -> Unit = mock()
    var knocked = false

    @BeforeTest
    fun setup() {
        resetMocks(matrixClientMock, matrixApiClientMock, roomApiClientMock, roomServiceMock)

        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single<I18n> { i18n }
                }
            )
        }.koin

        every { matrixClientMock.api } returns matrixApiClientMock
        every { matrixApiClientMock.room } returns roomApiClientMock

        every { onGroupJoinedMock.invoke(any(), any()) } calls {
            joined = true
        }
        joined = false
        every { onGroupKnockedMock.invoke(any()) } calls {
            knocked = true
        }
        knocked = false

        everySuspend { roomServiceMock.getById(eq(roomId)) } returns flowOf(room)
    }

    @Test
    fun `adding groups - should handle successful joining`() = runTest {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.success(roomId)

        val cut = searchGroupViewModel()

        listOf(JoinRule.Public, JoinRule.Invite, JoinRule.Restricted, JoinRule.KnockRestricted).forEach { rule ->
            joined = false
            cut.addGroup(group.copy(joinRule = rule))
            delay(500.milliseconds)
            joined shouldBe true
            knocked shouldBe false
        }
    }

    @Test
    fun `adding groups - should handle successful knocking`() = runTest {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(MatrixServerException(HttpStatusCode.Forbidden, ErrorResponse.Forbidden("")))
        everySuspend { roomApiClientMock.knockRoom(eq(roomId), any(), any(), any()) } returns
                Result.success(roomId)

        val cut = searchGroupViewModel()

        listOf(JoinRule.Knock, JoinRule.KnockRestricted).forEach { rule ->
            knocked = false
            cut.addGroup(group.copy(joinRule = rule))
            delay(500.milliseconds)
            joined shouldBe false
            knocked shouldBe true
        }
    }

    @Test
    fun `adding groups - should handle failure`() = runTest {
        val cut = searchGroupViewModel()
        cut.addGroup(group)
        delay(500.milliseconds)
        cut.error.value shouldBe i18n.joinRoomFailedGenericJoin()
    }

    @Test
    fun `adding groups - should handle error`() = runTest {
        everySuspend { roomApiClientMock.joinRoom(eq(roomId), any(), any(), any(), any()) } returns
                Result.failure(Throwable("something went wrong :("))

        val cut = searchGroupViewModel()
        cut.addGroup(group.copy(joinRule = JoinRule.Public))
        delay(500.milliseconds)
        cut.error.value shouldBe i18n.joinRoomFailedGenericJoin()
    }

    private suspend fun searchGroupViewModel() =
        SearchGroupViewModelFactory.create(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        )
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            onGroupJoined = onGroupJoinedMock,
            onGroupKnocked = onGroupKnockedMock,
            onBack = mock()
        )
}
