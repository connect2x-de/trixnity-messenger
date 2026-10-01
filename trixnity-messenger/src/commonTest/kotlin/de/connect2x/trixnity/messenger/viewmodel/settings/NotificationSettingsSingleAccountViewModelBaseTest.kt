package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.clientserverapi.client.MatrixClientServerApiClient
import de.connect2x.trixnity.clientserverapi.client.PushApiClient
import de.connect2x.trixnity.clientserverapi.model.push.SetPushRule
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.PushRulesEventContent
import de.connect2x.trixnity.core.model.push.PushAction
import de.connect2x.trixnity.core.model.push.PushRule
import de.connect2x.trixnity.core.model.push.PushRuleKind
import de.connect2x.trixnity.core.model.push.PushRuleSet
import de.connect2x.trixnity.core.model.push.ServerDefaultPushRules
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.firstWithClue
import de.connect2x.trixnity.messenger.notification.AccountNotificationPushRuleModifier
import de.connect2x.trixnity.messenger.notification.NoopAccountNotificationPushRuleModifier
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.testMatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.toNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.util.toPushRuleSet
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifyNoMoreCalls
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class NotificationSettingsSingleAccountViewModelBaseTest {
    private val userId = UserId("alice", "dino.unicorn")

    private val sampleSettings =
        AccountNotificationSettings(
            defaultLevel = AccountNotificationSettings.DefaultLevel.MENTION,
            sound = AccountNotificationSettings.Sound(room = false, dm = false, mention = false, call = false),
            activity = AccountNotificationSettings.Activity(invite = false, status = false, notice = false),
            mention = AccountNotificationSettings.Mention(user = false, room = false, keyword = false),
            keywords = setOf("alice1"),
        )

    private val samplePushRuleSet = sampleSettings.toPushRuleSet(userId)

    val matrixClientMock = mock<MatrixClient>()

    val userServiceMock = mock<UserService>()

    val matrixClientServerApiClientMock = mock<MatrixClientServerApiClient>()

    val pushApiClientMock = mock<PushApiClient>()

    private val continueHandlePushRuleRequest = MutableStateFlow(false)
    private val pushRulesEventContentState = MutableStateFlow<PushRuleSet?>(samplePushRuleSet)

    init {
        resetMocks(matrixClientMock, userServiceMock, matrixClientServerApiClientMock, pushApiClientMock)

        continueHandlePushRuleRequest.value = false
        pushRulesEventContentState.value = samplePushRuleSet
        every { matrixClientMock.di } returns koinApplication { modules(module { single { userServiceMock } }) }.koin
        every { matrixClientMock.userId } returns userId
        every { matrixClientMock.api } returns matrixClientServerApiClientMock
        every { matrixClientServerApiClientMock.push } returns pushApiClientMock
        every { userServiceMock.getAccountData(PushRulesEventContent::class, "") } returns
            pushRulesEventContentState.map { PushRulesEventContent((it)) }
        everySuspend { pushApiClientMock.setPushRule(any(), any(), any(), any(), any(), any()) } calls
            {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
        everySuspend { pushApiClientMock.deletePushRule(any(), any(), any()) } calls
            {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
        everySuspend { pushApiClientMock.setPushRuleActions(any(), any(), any(), any()) } calls
            {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
        everySuspend { pushApiClientMock.setPushRuleEnabled(any(), any(), any(), any()) } calls
            {
                continueHandlePushRuleRequest.first { it }
                Result.success(Unit)
            }
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `get settings`() = runTest {
        val cut = createCut()
        cut.accountSettings.firstWithClue(sampleSettings)
    }

    @Test
    fun `update settings`() = runTest {
        val cut = createCut()
        backgroundScope.launch { cut.accountSettings.collect {} }

        delay(500.milliseconds)
        cut.accountSettings.value shouldBe sampleSettings

        val newSettings =
            sampleSettings.copy(
                sound = sampleSettings.sound.copy(call = true),
                activity = sampleSettings.activity.copy(notice = true),
                keywords = setOf("alice2"),
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
                pushRule = SetPushRule.Request(actions = actions(notify = true, highlight = true), pattern = "alice2"),
                beforeRuleId = null,
                afterRuleId = null,
            )
            pushApiClientMock.deletePushRule(scope = "global", kind = PushRuleKind.CONTENT, ruleId = "alice1")
            pushApiClientMock.setPushRuleEnabled(
                scope = "global",
                kind = PushRuleKind.OVERRIDE,
                ruleId = ServerDefaultPushRules.SuppressNotice.id,
                enabled = false,
            )
            pushApiClientMock.setPushRuleActions(
                scope = "global",
                kind = PushRuleKind.UNDERRIDE,
                ruleId = ServerDefaultPushRules.Call.id,
                actions = actions(notify = true, sound = true, soundType = "ring"),
            )
        }
        cut.updateAccountSettingsError.value shouldBe null
    }

    @Test
    fun `update settings with timeout`() = runTest {
        val cut = createCut()
        cut.accountSettings.firstWithClue(sampleSettings)
        val newSettings =
            sampleSettings.copy(
                sound = sampleSettings.sound.copy(call = true),
                activity = sampleSettings.activity.copy(notice = true),
                keywords = setOf("alice2"),
            )

        cut.updateAccountSettings(newSettings)

        cut.accountSettingsIsUpdating.value shouldBe true
        continueHandlePushRuleRequest.value = true
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value = PushRuleSet()
        delay(11.seconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldContain "timeout"
    }

    @Test
    fun `unchanged settings ignore server managed rules and rule order`() = runTest {
        pushRulesEventContentState.value = samplePushRuleSet.withServerManagedRules()
        val cut = createCut()

        cut.updateAccountSettings(sampleSettings)
        delay(500.milliseconds)

        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
        verifyNoMoreCalls(pushApiClientMock)
    }

    @Test
    fun `sync confirmation ignores server managed rules and waits for keyword deletion`() = runTest {
        pushRulesEventContentState.value = samplePushRuleSet.withServerManagedRules()
        val cut = createCut()
        val settings =
            sampleSettings.copy(sound = sampleSettings.sound.copy(call = true), keywords = setOf("alice2", "alice3"))
        val syncedRules = settings.toPushRuleSet(userId).withServerManagedRules()
        continueHandlePushRuleRequest.value = true

        cut.updateAccountSettings(settings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value =
            syncedRules.copy(content = syncedRules.content.orEmpty() + samplePushRuleSet.content.orEmpty())
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value = syncedRules
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
    }

    @Test
    fun `modifier result is saved and used for sync confirmation and no-op detection`() = runTest {
        val settings = sampleSettings.copy(activity = sampleSettings.activity.copy(invite = true))
        pushRulesEventContentState.value = settings.toPushRuleSet(userId).withServerManagedRules()
        val cut =
            createCut(
                AccountNotificationPushRuleModifier { account, _, rules ->
                    account shouldBe userId
                    rules.copy(
                        override =
                            rules.override?.map {
                                if (it.ruleId == ServerDefaultPushRules.InviteForMe.id) it.copy(enabled = false) else it
                            }
                    )
                }
            )
        continueHandlePushRuleRequest.value = true
        cut.updateAccountSettings(settings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true
        verifySuspend {
            pushApiClientMock.setPushRuleEnabled(
                "global",
                PushRuleKind.OVERRIDE,
                ServerDefaultPushRules.InviteForMe.id,
                false,
            )
        }
        pushRulesEventContentState.value = samplePushRuleSet.withServerManagedRules()
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null

        continueHandlePushRuleRequest.value = false
        cut.updateAccountSettings(settings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
    }

    @Test
    fun `modifier only enabled and action changes are sent and awaited`() = runTest {
        val modifiedRules =
            samplePushRuleSet.copy(
                underride =
                    samplePushRuleSet.underride?.map {
                        if (it.ruleId == ServerDefaultPushRules.Call.id)
                            it.copy(enabled = true, actions = actions(notify = true, highlight = true))
                        else it
                    }
            )
        modifiedRules.toNotificationSettings() shouldBe sampleSettings
        val cut = createCut(AccountNotificationPushRuleModifier { _, _, _ -> modifiedRules })
        continueHandlePushRuleRequest.value = true

        cut.updateAccountSettings(sampleSettings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true
        verifySuspend {
            pushApiClientMock.setPushRuleEnabled("global", PushRuleKind.UNDERRIDE, ServerDefaultPushRules.Call.id, true)
            pushApiClientMock.setPushRuleActions(
                "global",
                PushRuleKind.UNDERRIDE,
                ServerDefaultPushRules.Call.id,
                actions(notify = true, highlight = true),
            )
        }
        verifyNoMoreCalls(pushApiClientMock)

        pushRulesEventContentState.value =
            samplePushRuleSet.copy(
                underride =
                    samplePushRuleSet.underride?.map {
                        if (it.ruleId == ServerDefaultPushRules.Call.id) it.copy(enabled = true) else it
                    }
            )
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value = modifiedRules
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null

        cut.updateAccountSettings(sampleSettings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        verifyNoMoreCalls(pushApiClientMock)
    }

    @Test
    fun `partial sync stays pending even when converted settings match`() = runTest {
        val cut = createCut()
        val settings = sampleSettings.copy(defaultLevel = AccountNotificationSettings.DefaultLevel.ROOM)
        val syncedRules = settings.toPushRuleSet(userId)
        continueHandlePushRuleRequest.value = true

        cut.updateAccountSettings(settings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        val partialRules =
            samplePushRuleSet.copy(
                underride =
                    samplePushRuleSet.underride?.map {
                        if (it.ruleId == ServerDefaultPushRules.Message.id) it.copy(enabled = true) else it
                    }
            )
        partialRules.toNotificationSettings() shouldBe settings
        pushRulesEventContentState.value = partialRules
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value = syncedRules
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
    }

    @Test
    fun `enabled updates identify rules by kind and id`() = runTest {
        val ruleId = ServerDefaultPushRules.Call.id
        val modifiedRules =
            samplePushRuleSet.copy(
                override = samplePushRuleSet.override.orEmpty() + PushRule.Override(ruleId = ruleId, enabled = true)
            )
        val cut = createCut(AccountNotificationPushRuleModifier { _, _, _ -> modifiedRules })
        continueHandlePushRuleRequest.value = true

        cut.updateAccountSettings(sampleSettings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true
        verifySuspend {
            pushApiClientMock.setPushRuleEnabled("global", PushRuleKind.OVERRIDE, ruleId, true)
            pushApiClientMock.setPushRuleActions("global", PushRuleKind.OVERRIDE, ruleId, emptySet())
        }
        verifyNoMoreCalls(pushApiClientMock)

        pushRulesEventContentState.value =
            samplePushRuleSet.copy(
                underride =
                    samplePushRuleSet.underride?.map {
                        if (it.ruleId == ruleId) it.copy(enabled = true, actions = emptySet()) else it
                    }
            )
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value = modifiedRules
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
    }

    @Test
    fun `content upserts await the sent pattern and actions for the rule id`() = runTest {
        val contentRule = samplePushRuleSet.content.orEmpty().single()
        val modifiedContentRule = contentRule.copy(pattern = "modified", actions = actions(notify = true))
        val modifiedRules = samplePushRuleSet.copy(content = listOf(modifiedContentRule))
        val cut = createCut(AccountNotificationPushRuleModifier { _, _, _ -> modifiedRules })
        continueHandlePushRuleRequest.value = true

        cut.updateAccountSettings(sampleSettings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true
        verifySuspend {
            pushApiClientMock.setPushRule(
                "global",
                PushRuleKind.CONTENT,
                contentRule.ruleId,
                SetPushRule.Request(actions = modifiedContentRule.actions, pattern = modifiedContentRule.pattern),
                beforeRuleId = null,
                afterRuleId = null,
            )
        }
        verifyNoMoreCalls(pushApiClientMock)

        pushRulesEventContentState.value =
            samplePushRuleSet.copy(content = listOf(modifiedContentRule.copy(ruleId = "other")))
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value =
            samplePushRuleSet.copy(content = listOf(modifiedContentRule.copy(pattern = contentRule.pattern)))
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value =
            samplePushRuleSet.copy(content = listOf(modifiedContentRule.copy(actions = contentRule.actions)))
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        // Content upserts do not send enabled or default.
        pushRulesEventContentState.value =
            samplePushRuleSet.copy(content = listOf(modifiedContentRule.copy(enabled = true, default = true)))
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
    }

    @Test
    fun `deletion needs a synced rule set and ignores other rules with the deleted pattern`() = runTest {
        val cut = createCut()
        continueHandlePushRuleRequest.value = true

        cut.updateAccountSettings(sampleSettings.copy(keywords = emptySet()))
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true
        verifySuspend { pushApiClientMock.deletePushRule("global", PushRuleKind.CONTENT, "alice1") }
        verifyNoMoreCalls(pushApiClientMock)

        pushRulesEventContentState.value = null
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true

        pushRulesEventContentState.value =
            samplePushRuleSet.copy(content = samplePushRuleSet.content?.map { it.copy(ruleId = "other") })
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
    }

    @Test
    fun `sync confirmation ignores untouched rules and fields`() = runTest {
        val cut = createCut()
        val settings =
            sampleSettings.copy(
                sound = sampleSettings.sound.copy(room = true),
                activity = sampleSettings.activity.copy(notice = true),
            )
        val syncedRules = settings.toPushRuleSet(userId)
        continueHandlePushRuleRequest.value = true

        cut.updateAccountSettings(settings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe true
        verifySuspend {
            pushApiClientMock.setPushRuleEnabled(
                "global",
                PushRuleKind.OVERRIDE,
                ServerDefaultPushRules.SuppressNotice.id,
                false,
            )
            pushApiClientMock.setPushRuleActions(
                "global",
                PushRuleKind.UNDERRIDE,
                ServerDefaultPushRules.Message.id,
                actions(notify = true, sound = true),
            )
            pushApiClientMock.setPushRuleActions(
                "global",
                PushRuleKind.UNDERRIDE,
                ServerDefaultPushRules.Encrypted.id,
                actions(notify = true, sound = true),
            )
        }
        verifyNoMoreCalls(pushApiClientMock)

        pushRulesEventContentState.value =
            syncedRules
                .copy(
                    override =
                        syncedRules.override?.map {
                            if (it.ruleId == ServerDefaultPushRules.SuppressNotice.id)
                                it.copy(actions = actions(notify = true), conditions = null, default = false)
                            else it
                        },
                    underride = syncedRules.underride?.map { it.copy(enabled = true) },
                    content = syncedRules.content.orEmpty() + PushRule.Content(ruleId = "unrelated", pattern = "new"),
                )
                .withServerManagedRules()
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
    }

    @Test
    fun `no requests return immediately even when converted settings differ`() = runTest {
        val settings = sampleSettings.copy(activity = sampleSettings.activity.copy(invite = true))
        pushRulesEventContentState.value = settings.toPushRuleSet(userId)
        val cut =
            createCut(
                AccountNotificationPushRuleModifier { _, _, rules ->
                    // Removing a server default rule does not result in a delete request.
                    rules.copy(
                        override = rules.override?.filterNot { it.ruleId == ServerDefaultPushRules.InviteForMe.id }
                    )
                }
            )

        cut.updateAccountSettings(settings)
        delay(500.milliseconds)
        cut.accountSettingsIsUpdating.value shouldBe false
        cut.updateAccountSettingsError.value shouldBe null
        verifyNoMoreCalls(pushApiClientMock)
    }

    private fun PushRuleSet.withServerManagedRules(): PushRuleSet {
        return copy(
            override =
                (override.orEmpty().map {
                        // The server owns default rule conditions, which are not updated by the settings UI.
                        if (it.ruleId == ServerDefaultPushRules.Master.id) it.copy(conditions = null) else it
                    } + ServerDefaultPushRules.Reaction.rule + PushRule.Override(ruleId = "custom"))
                    .reversed(),
            underride = underride.orEmpty().reversed(),
            content =
                (content.orEmpty() +
                        PushRule.Content(
                            ruleId = ".m.rule.contains_user_name",
                            default = true,
                            enabled = true,
                            pattern = userId.localpart,
                            actions = setOf(PushAction.Notify),
                        ))
                    .reversed(),
            room = listOf(PushRule.Room(roomId = RoomId("!room:example.org"))),
            sender = listOf(PushRule.Sender(userId = UserId("@bob:example.org"))),
        )
    }

    private fun TestScope.createCut(
        accountNotificationPushRuleModifier: AccountNotificationPushRuleModifier =
            NoopAccountNotificationPushRuleModifier()
    ): NotificationSettingsSingleAccountViewModel {
        val di =
            koinApplication {
                    modules(createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock)))
                    modules(
                        module { single<AccountNotificationPushRuleModifier> { accountNotificationPushRuleModifier } }
                    )
                }
                .koin
        return NotificationSettingsSingleAccountViewModelImpl(
            viewModelContext = testMatrixClientViewModelContext(di = di, userId = userId)
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
