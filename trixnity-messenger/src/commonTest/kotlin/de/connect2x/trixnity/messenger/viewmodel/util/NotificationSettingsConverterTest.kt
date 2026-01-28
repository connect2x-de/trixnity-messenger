package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountNotificationSettings.Activity
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.PushRulesEventContent
import de.connect2x.trixnity.core.serialization.createMatrixEventJson
import kotlin.test.BeforeTest
import kotlin.test.Test

class NotificationSettingsConverterTest {
    val json = createMatrixEventJson()
    val userId = UserId("alice", "dino.unicorn")

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `parse push rules 1`() = runTest {
        val pushRulSet = json.decodeFromString<PushRulesEventContent>(notificationSettingsConverterJsonSample1).global
        pushRulSet.shouldNotBeNull()
        pushRulSet.toNotificationSettings() shouldBe AccountNotificationSettings(
            defaultLevel = AccountNotificationSettings.DefaultLevel.ROOM, sound = AccountNotificationSettings.Sound(
                room = false,
                dm = true,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = false,
            ), mention = AccountNotificationSettings.Mention(
                user = true,
                room = true,
                keyword = true,
            ), keywords = setOf("keyword1", "keyword2")
        )
    }

    @Test
    fun `parse push rules 2`() = runTest {
        val pushRulSet = json.decodeFromString<PushRulesEventContent>(notificationSettingsConverterJsonSample2).global
        pushRulSet.shouldNotBeNull()
        pushRulSet.toNotificationSettings() shouldBe AccountNotificationSettings(
            defaultLevel = AccountNotificationSettings.DefaultLevel.DM, sound = AccountNotificationSettings.Sound(
                room = false,
                dm = false,
                mention = false,
                call = false,
            ), activity = Activity(
                invite = false,
                status = false,
                notice = false,
            ), mention = AccountNotificationSettings.Mention(
                user = false,
                room = false,
                keyword = false,
            ), keywords = setOf()
        )
    }

    @Test
    fun `parse push rules 3`() = runTest {
        val pushRulSet = json.decodeFromString<PushRulesEventContent>(notificationSettingsConverterJsonSample3).global
        pushRulSet.shouldNotBeNull()
        pushRulSet.toNotificationSettings() shouldBe AccountNotificationSettings(
            defaultLevel = AccountNotificationSettings.DefaultLevel.NONE, sound = AccountNotificationSettings.Sound(
                room = false,
                dm = false,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = true,
            ), mention = AccountNotificationSettings.Mention(
                user = true,
                room = true,
                keyword = false,
            ), keywords = setOf()
        )
    }

    @Test
    fun `parse NotificationSettings 1`() = runTest {
        val accountNotificationSettings = AccountNotificationSettings(
            defaultLevel = AccountNotificationSettings.DefaultLevel.ROOM, sound = AccountNotificationSettings.Sound(
                room = true,
                dm = true,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = false,
            ), mention = AccountNotificationSettings.Mention(
                user = true,
                room = true,
                keyword = true,
            ), keywords = setOf("keyword1", "keyword2")
        )
        accountNotificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe accountNotificationSettings
    }

    @Test
    fun `parse NotificationSettings 2`() = runTest {
        val accountNotificationSettings = AccountNotificationSettings(
            defaultLevel = AccountNotificationSettings.DefaultLevel.DM, sound = AccountNotificationSettings.Sound(
                room = false,
                dm = false,
                mention = false,
                call = false,
            ), activity = Activity(
                invite = false,
                status = false,
                notice = false,
            ), mention = AccountNotificationSettings.Mention(
                user = false,
                room = false,
                keyword = false,
            ), keywords = setOf()
        )

        accountNotificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe accountNotificationSettings
    }

    @Test
    fun `parse NotificationSettings 3`() = runTest {
        val accountNotificationSettings = AccountNotificationSettings(
            defaultLevel = AccountNotificationSettings.DefaultLevel.NONE, sound = AccountNotificationSettings.Sound(
                room = true,
                dm = false,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = true,
            ), mention = AccountNotificationSettings.Mention(
                user = true,
                room = true,
                keyword = false,
            ), keywords = setOf()
        )

        accountNotificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe accountNotificationSettings
    }
}
