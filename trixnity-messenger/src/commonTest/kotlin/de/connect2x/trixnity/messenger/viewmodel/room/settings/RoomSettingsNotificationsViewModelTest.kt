package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.PushRulesEventContent
import de.connect2x.trixnity.core.model.push.PushAction
import de.connect2x.trixnity.core.model.push.PushCondition
import de.connect2x.trixnity.core.model.push.PushRule
import de.connect2x.trixnity.core.model.push.PushRuleSet
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RoomSettingsNotificationsViewModelTest {
    private val roomId = RoomId("!room")

    private val me = UserId("user1", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    init {
        resetMocks(matrixClientMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication { modules(module { single { userServiceMock } }) }.koin
        every { matrixClientMock.userId } returns me
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `set room's push rule to DEFAULT'`() = runTest {
        every { userServiceMock.getAccountData(PushRulesEventContent::class, any()) } returns
            MutableStateFlow(PushRulesEventContent(global = PushRuleSet()))
        val cut = roomSettingsNotificationsViewModel(MutableStateFlow(null))
        backgroundScope.launch { cut.selectedRoomNotificationsLevel.collect {} }

        eventually(100.milliseconds) {
            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.DEFAULT
        }
    }

    @Test
    fun `set room's push rule to ALL'`() = runTest {
        every { userServiceMock.getAccountData(PushRulesEventContent::class, any()) } returns
            MutableStateFlow(
                PushRulesEventContent(
                    global =
                        PushRuleSet(
                            room =
                                listOf(
                                    PushRule.Room(
                                        actions = setOf(PushAction.Notify),
                                        enabled = true,
                                        default = false,
                                        roomId = RoomId("!room"),
                                    )
                                )
                        )
                )
            )
        val cut = roomSettingsNotificationsViewModel(MutableStateFlow(null))
        backgroundScope.launch { cut.selectedRoomNotificationsLevel.collect {} }

        eventually(100.milliseconds) { cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.ALL }
    }

    @Test
    fun `set room's push rule to MENTIONS'`() = runTest {
        every { userServiceMock.getAccountData(PushRulesEventContent::class, any()) } returns
            MutableStateFlow(
                PushRulesEventContent(
                    global =
                        PushRuleSet(
                            room =
                                listOf(
                                    PushRule.Room(
                                        actions = setOf(),
                                        enabled = true,
                                        default = false,
                                        roomId = RoomId("!room"),
                                    )
                                )
                        )
                )
            )
        val cut = roomSettingsNotificationsViewModel(MutableStateFlow(null))
        backgroundScope.launch { cut.selectedRoomNotificationsLevel.collect {} }

        eventually(100.milliseconds) {
            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.MENTIONS
        }
    }

    @Test
    fun `set room's push rule to OFF'`() = runTest {
        every { userServiceMock.getAccountData(PushRulesEventContent::class, any()) } returns
            MutableStateFlow(
                PushRulesEventContent(
                    global =
                        PushRuleSet(
                            override =
                                listOf(
                                    PushRule.Override(
                                        actions = setOf(),
                                        conditions = setOf(PushCondition.EventMatch("room_id", "!room")),
                                        enabled = true,
                                        default = false,
                                        ruleId = "!room",
                                    )
                                )
                        )
                )
            )
        val cut = roomSettingsNotificationsViewModel(MutableStateFlow(null))
        backgroundScope.launch { cut.selectedRoomNotificationsLevel.collect {} }

        eventually(100.milliseconds) { cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.OFF }
    }

    private fun TestScope.roomSettingsNotificationsViewModel(
        error: MutableStateFlow<String?>
    ): RoomSettingsNotificationsViewModelImpl {
        return RoomSettingsNotificationsViewModelImpl(
            viewModelContext =
                testMatrixClientViewModelContext(
                    di =
                        koinApplication {
                                modules(
                                    createTestDefaultTrixnityMessengerModules(
                                        mapOf(UserId("test", "server") to matrixClientMock)
                                    )
                                )
                            }
                            .koin,
                    userId = UserId("test", "server"),
                ),
            selectedRoomId = roomId,
            error = error,
        )
    }
}
