package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.matcher.eq
import dev.mokkery.mock
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
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
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RoomSettingsNotificationsViewModelTest : ShouldSpec() {
    override fun timeout(): Long = 4_000

    private val roomId = RoomId("room", "localhost")

    private val me = UserId("user1", "localhost")

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    init {
        coroutineTestScope = true

        beforeTest {
            resetMocks(matrixClientMock, userServiceMock)
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns me
        }

        should("set room's push rule to DEFAULT'") {
            every {
                userServiceMock.getAccountData(eq(PushRulesEventContent::class), any())
            } returns MutableStateFlow(
                PushRulesEventContent(
                    global = PushRuleSet()
                )
            )
            val cut = roomSettingsNotificationsViewModel(coroutineContext, MutableStateFlow(null))
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.DEFAULT

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set room's push rule to ALL'") {
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
            val cut = roomSettingsNotificationsViewModel(coroutineContext, MutableStateFlow(null))
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.ALL

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set room's push rule to MENTIONS'") {
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
            val cut = roomSettingsNotificationsViewModel(coroutineContext, MutableStateFlow(null))
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.MENTIONS

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }

        should("set room's push rule to SILENT'") {
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
            val cut = roomSettingsNotificationsViewModel(coroutineContext, MutableStateFlow(null))
            val subscriberJob = launch { cut.selectedRoomNotificationsLevel.collect {} }
            testCoroutineScheduler.advanceUntilIdle()

            cut.selectedRoomNotificationsLevel.value.key shouldBe NotificationLevels.SILENT

            subscriberJob.cancel()
            cancelNeverEndingCoroutines()
        }
    }

    private suspend fun roomSettingsNotificationsViewModel(
        coroutineContext: CoroutineContext,
        error: MutableStateFlow<String?>,
    ): RoomSettingsNotificationsViewModelImpl {
        Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher]))
        return RoomSettingsNotificationsViewModelImpl(
            viewModelContext = MatrixClientViewModelContextImpl(
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                di = koinApplication {
                    modules(
                        createTestDefaultTrixnityMessengerModules(mapOf(UserId("test", "server") to matrixClientMock)),
                    )
                }.koin,
                userId = UserId("test", "server"),
                coroutineContext = coroutineContext,
            ),
            selectedRoomId = roomId,
            error = error,
        )
    }

}
