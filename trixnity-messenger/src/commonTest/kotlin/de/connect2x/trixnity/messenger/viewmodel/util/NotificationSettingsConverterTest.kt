package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettings.Activity
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.serialization.createMatrixEventJson
import kotlin.test.Test

class NotificationSettingsConverterTest {
    val json = createMatrixEventJson()
    val userId = UserId("alice", "dino.unicorn")

    @Test
    fun `parse push rules 1`() = runTest {
        val pushRulSet = json.decodeFromString<PushRulesEventContent>(notificationSettingsConverterJsonSample1).global
        pushRulSet.shouldNotBeNull()
        pushRulSet.toNotificationSettings() shouldBe NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.ROOM, sound = NotificationSettings.Sound(
                room = false,
                dm = true,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = false,
            ), mention = NotificationSettings.Mention(
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
        pushRulSet.toNotificationSettings() shouldBe NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.DM, sound = NotificationSettings.Sound(
                room = false,
                dm = false,
                mention = false,
                call = false,
            ), activity = Activity(
                invite = false,
                status = false,
                notice = false,
            ), mention = NotificationSettings.Mention(
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
        pushRulSet.toNotificationSettings() shouldBe NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.NONE, sound = NotificationSettings.Sound(
                room = false,
                dm = false,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = true,
            ), mention = NotificationSettings.Mention(
                user = true,
                room = true,
                keyword = false,
            ), keywords = setOf()
        )
    }

    @Test
    fun `parse NotificationSettings 1`() = runTest {
        val notificationSettings = NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.ROOM, sound = NotificationSettings.Sound(
                room = true,
                dm = true,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = false,
            ), mention = NotificationSettings.Mention(
                user = true,
                room = true,
                keyword = true,
            ), keywords = setOf("keyword1", "keyword2")
        )
        notificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe notificationSettings
    }

    @Test
    fun `parse NotificationSettings 2`() = runTest {
        val notificationSettings = NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.DM, sound = NotificationSettings.Sound(
                room = false,
                dm = false,
                mention = false,
                call = false,
            ), activity = Activity(
                invite = false,
                status = false,
                notice = false,
            ), mention = NotificationSettings.Mention(
                user = false,
                room = false,
                keyword = false,
            ), keywords = setOf()
        )

        notificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe notificationSettings
    }

    @Test
    fun `parse NotificationSettings 3`() = runTest {
        val notificationSettings = NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.NONE, sound = NotificationSettings.Sound(
                room = true,
                dm = false,
                mention = true,
                call = true,
            ), activity = Activity(
                invite = true,
                status = true,
                notice = true,
            ), mention = NotificationSettings.Mention(
                user = true,
                room = true,
                keyword = false,
            ), keywords = setOf()
        )

        notificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe notificationSettings
    }
}
