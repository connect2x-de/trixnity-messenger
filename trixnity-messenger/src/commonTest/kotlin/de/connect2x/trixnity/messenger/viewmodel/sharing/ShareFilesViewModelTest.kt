package de.connect2x.trixnity.messenger.viewmodel.sharing

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.room.RoomService
import de.connect2x.trixnity.client.room.message.MessageBuilder
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.RoomApiClient
import de.connect2x.trixnity.clientserverapi.model.user.Profile
import de.connect2x.trixnity.clientserverapi.model.user.ProfileField
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixClientsImpl
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.SharedData
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonPrimitive
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class ShareFilesViewModelTest {

    private var coroutineScope: CoroutineScope? = null
    private val ourUserId = UserId("me", "localhost")
    private val ourRoomId = RoomId("!myroom")
    private val ourRoom = Room(roomId = ourRoomId)

    val matrixClientMock = mock<MatrixClient>()

    val roomServiceMock = mock<RoomService>()

    val userServiceMock = mock<UserService>()

    val mediaServiceMock = mock<MediaService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val roomsApiClientMock = mock<RoomApiClient>()

    val i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.of("CET"),
    ) {}

    var formattedBody: String? = null
    var body = ""

    init {
        resetMocks(
            matrixClientMock,
            roomServiceMock,
            userServiceMock,
            mediaServiceMock,
            matrixClientServerApiClientMock,
            roomsApiClientMock,
        )
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { roomServiceMock }
                    single { userServiceMock }
                    single { mediaServiceMock }
                    single<MatrixClients> {
                        MatrixClientsImpl(
                            matrixClientFactory = get(),
                            deleteAccountData = get(),
                            settings = get(),
                            config = get(),
                            secretByteArrays = get(),
                            i18n = i18n,
                            matrixClients = MutableStateFlow(mapOf(ourUserId to matrixClientMock))
                        )
                    }
                })
        }.koin

        every { matrixClientMock.userId } returns ourUserId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.room } returns roomsApiClientMock

        val profile = Profile(ProfileField.DisplayName("Me ^^"))
        every { matrixClientMock.profile } returns MutableStateFlow(profile)

        every { roomServiceMock.getAll() } returns flowOf(
            mapOf(
                ourRoomId to flowOf(ourRoom)
            )
        )
        every { roomServiceMock.getById(ourRoomId) } returns MutableStateFlow(ourRoom)
        everySuspend { roomServiceMock.sendMessage(any(), any(), any()) } calls {
            val roomId = it.arg<RoomId>(0)
            val builderFunction = it.arg<suspend MessageBuilder.() -> Unit>(2)
            val builder = MessageBuilder(roomId, roomServiceMock, mediaServiceMock, ourUserId)
            val message = builder.build(builderFunction)

            if (message is RoomMessageEventContent.TextBased) {
                formattedBody = message.formattedBody
                body = message.body
            }

            ""
        }
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `Not format markdown`() = runTest {
        val vm = shareDataViewModel(SharedData.PlainText("**haii~**"))

        vm.roomList.selectRoom(ourRoomId)

        eventually(500.milliseconds) {
            vm.selectedRoomId.value shouldBe ourRoomId
        }

        vm.send()

        eventually(3000.milliseconds) {
            formattedBody shouldBe null
            body shouldBe "**haii~**"
        }
    }

    private suspend fun TestScope.shareDataViewModel(data: SharedData): ShareDataViewModel {
        val settings = matrixMessengerSettingsHolder()
        return SharedDataViewModelImpl(
            testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(ourUserId to matrixClientMock), settings
                        ),
                    )
                }.koin,
                userId = ourUserId,
            ), data
        ) { }
    }

    private suspend fun matrixMessengerSettingsHolder(): MatrixMessengerSettingsHolder {
        val settingsHolder: MutableStateFlow<MatrixMessengerSettings?> = MutableStateFlow(
            MatrixMessengerSettings(
                mapOf(
                    "preferredLang" to JsonPrimitive("en"),
                )
            )
        )
        val dummyStorage = object : SettingsStorage {
            override suspend fun read(): String? = null
            override suspend fun write(settings: String) {}
        }
        val delegate = MatrixMessengerSettingsHolderImpl(dummyStorage, settingsHolder)
        delegate.update<MatrixMessengerSettingsBase> { it.copy(selectedAccount = ourUserId) }
        return object : MatrixMessengerSettingsHolder by delegate {
            override fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> = flow {
                val hasNoEntry = delegate[userId].first() == null
                if (hasNoEntry) delegate.update<MatrixMessengerAccountSettingsBase>(userId) { it.copy() }
                emitAll(delegate[userId])
            }
        }
    }
}
