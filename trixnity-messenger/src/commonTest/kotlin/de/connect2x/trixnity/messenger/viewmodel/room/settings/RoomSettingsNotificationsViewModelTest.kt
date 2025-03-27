package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.eventually
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.push.PushAction
import net.folivo.trixnity.core.model.push.PushCondition
import net.folivo.trixnity.core.model.push.PushRule
import net.folivo.trixnity.core.model.push.PushRuleSet
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class RoomSettingsNotificationsViewModelTest {
    private val roomId = RoomId("room", "localhost")

    private val me = UserId("user1", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    init {
        resetMocks(matrixClientMock, userServiceMock)
        every { matrixClientMock.di } returns koinApplication {
            modules(
                module {
                    single { userServiceMock }
                })
        }.koin
        every { matrixClientMock.userId } returns me
    }

    @Test
    fun `set room's push rule to DEFAULT'`() = runTest {
        every {
            userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
        } returns MutableStateFlow(
            PushRulesEventContent(
                global = PushRuleSet()
            )
        )
        val cut = roomSettingsNotificationsViewModel(MutableStateFlow(null))
        backgroundScope.launch { cut.selectedRoomNotificationsLevel.collect {} }

        eventually(100.milliseconds) {
            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.DEFAULT
        }
    }

    @Test
    fun `set room's push rule to ALL'`() = runTest {
        every {
            userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
        } returns MutableStateFlow(
            PushRulesEventContent(
                global = PushRuleSet(
                    room = listOf(
                        PushRule.Room(
                            actions = setOf(PushAction.Notify),
                            enabled = true,
                            default = false,
                            roomId = RoomId("!room:localhost"),
                        ),
                    ),
                )
            )
        )
        val cut = roomSettingsNotificationsViewModel(MutableStateFlow(null))
        backgroundScope.launch { cut.selectedRoomNotificationsLevel.collect {} }

        eventually(100.milliseconds) {
            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.ALL
        }
    }

    @Test
    fun `set room's push rule to MENTIONS'`() = runTest {
        every {
            userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
        } returns MutableStateFlow(
            PushRulesEventContent(
                global = PushRuleSet(
                    room = listOf(
                        PushRule.Room(
                            actions = setOf(),
                            enabled = true,
                            default = false,
                            roomId = RoomId("!room:localhost"),
                        ),
                    ),
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
    fun `set room's push rule to SILENT'`() = runTest {
        every {
            userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
        } returns MutableStateFlow(
            PushRulesEventContent(
                global = PushRuleSet(
                    override = listOf(
                        PushRule.Override(
                            actions = setOf(),
                            conditions = setOf(PushCondition.EventMatch("room_id", "!room:localhost")),
                            enabled = true,
                            default = false,
                            ruleId = "!room:localhost",
                        ),
                    ),
                )
            )
        )
        val cut = roomSettingsNotificationsViewModel(MutableStateFlow(null))
        backgroundScope.launch { cut.selectedRoomNotificationsLevel.collect {} }

        eventually(100.milliseconds) {
            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.SILENT
        }
    }

    private fun TestScope.roomSettingsNotificationsViewModel(
        error: MutableStateFlow<String?>,
    ): RoomSettingsNotificationsViewModelImpl {
        return RoomSettingsNotificationsViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(
                            mapOf(UserId("test", "server") to matrixClientMock)
                        ),
                    )
                }.koin,
                userId = UserId("test", "server"),
            ),
            selectedRoomId = roomId,
            error = error,
        )
    }

}
