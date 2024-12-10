package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.viewmodel.util.toPushRuleSet
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.PushApiClient
import net.folivo.trixnity.clientserverapi.model.push.SetPushRule
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.push.PushAction
import net.folivo.trixnity.core.model.push.PushRuleKind
import net.folivo.trixnity.core.model.push.PushRuleSet
import net.folivo.trixnity.core.model.push.ServerDefaultPushRules
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class NotificationSettingsSingleAccountViewModelBaseTest : ShouldSpec() {
    private val userId = UserId("alice", "dino.unicorn")

    private val sampleSettings =
        NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.MENTION,
            sound = NotificationSettings.Sound(
                room = false,
                dm = false,
                mention = false,
                call = false,
            ),
            activity = NotificationSettings.Activity(
                invite = false,
                status = false,
                notice = false,
            ),
            mention = NotificationSettings.Mention(
                user = false,
                room = false,
                keyword = false
            ),
            keywords = setOf("alice1")
        )

    private val samplePushRuleSet = sampleSettings.toPushRuleSet(userId)

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val pushApiClientMock = mock<PushApiClient>()

    private val continueHandlePushRuleRequest = MutableStateFlow(false)
    private val pushRulesEventContentState = MutableStateFlow<PushRuleSet?>(samplePushRuleSet)

    init {
        coroutineTestScope = true
        timeout = 20_000 // virtual time!

        beforeTest {
            resetMocks(matrixClientMock, userServiceMock, matrixClientServerApiClientMock, pushApiClientMock)

            continueHandlePushRuleRequest.value = false
            pushRulesEventContentState.value = samplePushRuleSet
            every { matrixClientMock.di } returns koinApplication {
                modules(
                    module {
                        single { userServiceMock }
                    }
                )
            }.koin
            every { matrixClientMock.userId } returns userId
            every { matrixClientMock.api } returns matrixClientServerApiClientMock
            every { matrixClientServerApiClientMock.push } returns pushApiClientMock
            every { userServiceMock.getAccountData(PushRulesEventContent::class, "") } returns
                    pushRulesEventContentState.map { PushRulesEventContent((it)) }
            everySuspend {
                pushApiClientMock.setPushRule(any(), any(), any(), any(), any(), any(), any())
            } calls {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
            everySuspend {
                pushApiClientMock.deletePushRule(any(), any(), any(), any())
            } calls {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
            everySuspend {
                pushApiClientMock.setPushRuleActions(any(), any(), any(), any(), any())
            } calls {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
            everySuspend {
                pushApiClientMock.setPushRuleEnabled(any(), any(), any(), any(), any())
            } calls {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
        }

        should("get settings") {
            val cut = createCut()
            cut.accountSettings.firstWithClue(sampleSettings)

            cancelNeverEndingCoroutines()
        }
        should("update settings") {
            val cut = createCut()
            launch { cut.accountSettings.collect { } }

            delay(500.milliseconds)
            cut.accountSettings.value shouldBe sampleSettings

            val newSettings = sampleSettings.copy(
                sound = sampleSettings.sound.copy(
                    call = true,
                ),
                activity = sampleSettings.activity.copy(
                    notice = true,
                ),
                keywords = setOf("alice2")
            )
            cut.updateAccountSettings(newSettings)

            cut.accountSettingsIsUpdating.value shouldBe true
            continueHandlePushRuleRequest.value = true
            cut.accountSettingsIsUpdating.value shouldBe true

            delay(500.milliseconds) // server sets data!
            pushRulesEventContentState.value = newSettings.toPushRuleSet(userId)
            delay(500.milliseconds)
            cut.accountSettingsIsUpdating.value shouldBe false

            verifySuspend {
                pushApiClientMock.setPushRule(
                    scope = "global",
                    kind = PushRuleKind.CONTENT,
                    ruleId = "alice2",
                    pushRule = SetPushRule.Request(
                        actions = actions(notify = true, highlight = true),
                        pattern = "alice2"
                    ),
                    beforeRuleId = null,
                    afterRuleId = null,
                    asUserId = null
                )
                pushApiClientMock.deletePushRule(
                    scope = "global",
                    kind = PushRuleKind.CONTENT,
                    ruleId = "alice1",
                    asUserId = null
                )
                pushApiClientMock.setPushRuleEnabled(
                    scope = "global",
                    kind = PushRuleKind.OVERRIDE,
                    ruleId = ServerDefaultPushRules.SuppressNotice.id,
                    enabled = false,
                    asUserId = null
                )
                pushApiClientMock.setPushRuleActions(
                    scope = "global",
                    kind = PushRuleKind.UNDERRIDE,
                    ruleId = ServerDefaultPushRules.Call.id,
                    actions = actions(notify = true, sound = true, soundType = "ring"),
                    asUserId = null
                )
            }
            cut.accountSettingsUpdateError.value shouldBe null

            cancelNeverEndingCoroutines()
        }
        should("update settings with timeout") {
            val cut = createCut()
            cut.accountSettings.firstWithClue(sampleSettings)
            val newSettings = sampleSettings.copy(
                sound = sampleSettings.sound.copy(
                    call = true,
                ),
                activity = sampleSettings.activity.copy(
                    notice = true,
                ),
                keywords = setOf("alice2")
            )

            cut.updateAccountSettings(newSettings)

            cut.accountSettingsIsUpdating.value shouldBe true
            continueHandlePushRuleRequest.value = true
            cut.accountSettingsIsUpdating.value shouldBe true

            pushRulesEventContentState.value = PushRuleSet()
            cut.accountSettingsIsUpdating.firstWithClue(false, 11.seconds)
            testCoroutineScheduler.currentTime shouldBeGreaterThanOrEqual 10_000
            cut.accountSettingsUpdateError.value shouldContain "timeout"

            cancelNeverEndingCoroutines()
        }
    }

    private fun TestScope.createCut(): NotificationSettingsSingleAccountViewModelBase {
        val di = koinApplication {
            modules(createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock)))
        }.koin
        return NotificationSettingsSingleAccountViewModelBaseImpl(
            viewModelContext = testMatrixClientViewModelContext(
                di = di,
                userId = userId,
            )
        )
    }

    private fun actions(
        notify: Boolean = false,
        sound: Boolean = false,
        soundType: String = "default",
        highlight: Boolean = false,
    ): Set<PushAction> =
        setOfNotNull(
            if (notify) PushAction.Notify else null,
            if (sound) PushAction.SetSoundTweak(soundType) else null,
            if (highlight) PushAction.SetHighlightTweak() else null,
        )
}
