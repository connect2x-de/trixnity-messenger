package de.connect2x.trixnity.messenger.viewmodel.util

import dev.mokkery.matcher.*

import dev.mokkery.answering.*

import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettings.Activity
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.PushRulesEventContent
import net.folivo.trixnity.core.model.push.PushRule
import net.folivo.trixnity.core.model.push.ServerDefaultPushRules
import net.folivo.trixnity.core.serialization.createMatrixEventJson

class NotificationSettingsConverterTest : ShouldSpec({
    timeout = 4_000
    val json = createMatrixEventJson()
    val userId = UserId("alice", "dino.unicorn")

    infix fun List<PushRule>?.shouldContainServerDefaultRulesIn(other: List<PushRule>?) {
        shouldNotBeNull()
        val allRules =
            ServerDefaultPushRules.all(userId) -
                    ServerDefaultPushRules.Reaction -
                    ServerDefaultPushRules.ServerAcl -
                    ServerDefaultPushRules.SuppressEdits
        other?.filter { rule -> allRules.any { it.id == rule.ruleId } }
            ?.forEach { rule ->
                find { it.ruleId == rule.ruleId } shouldBe rule
            }
    }

    should("parse push rules 1") {
        val pushRulSet = json.decodeFromString<PushRulesEventContent>(notificationSettingsConverterJsonSample1).global
        pushRulSet.shouldNotBeNull()
        pushRulSet.toNotificationSettings() shouldBe NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.ROOM,
            sound = NotificationSettings.Sound(
                room = false,
                dm = true,
                mention = true,
                call = true,
            ),
            activity = Activity(
                invite = true,
                status = true,
                notice = false,
            ),
            mention = NotificationSettings.Mention(
                user = true,
                room = true,
                keyword = true,
            ),
            keywords = setOf("keyword1", "keyword2")
        )
    }
    should("parse push rules 2") {
        val pushRulSet = json.decodeFromString<PushRulesEventContent>(notificationSettingsConverterJsonSample2).global
        pushRulSet.shouldNotBeNull()
        pushRulSet.toNotificationSettings() shouldBe NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.DM,
            sound = NotificationSettings.Sound(
                room = false,
                dm = false,
                mention = false,
                call = false,
            ),
            activity = Activity(
                invite = false,
                status = false,
                notice = false,
            ),
            mention = NotificationSettings.Mention(
                user = false,
                room = false,
                keyword = false,
            ),
            keywords = setOf()
        )
    }
    should("parse push rules 3") {
        val pushRulSet = json.decodeFromString<PushRulesEventContent>(notificationSettingsConverterJsonSample3).global
        pushRulSet.shouldNotBeNull()
        pushRulSet.toNotificationSettings() shouldBe NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.NONE,
            sound = NotificationSettings.Sound(
                room = false,
                dm = false,
                mention = true,
                call = true,
            ),
            activity = Activity(
                invite = true,
                status = true,
                notice = true,
            ),
            mention = NotificationSettings.Mention(
                user = true,
                room = true,
                keyword = false,
            ),
            keywords = setOf()
        )
    }
    should("parse NotificationSettings 1") {
        val notificationSettings = NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.ROOM,
            sound = NotificationSettings.Sound(
                room = true,
                dm = true,
                mention = true,
                call = true,
            ),
            activity = Activity(
                invite = true,
                status = true,
                notice = false,
            ),
            mention = NotificationSettings.Mention(
                user = true,
                room = true,
                keyword = true,
            ),
            keywords = setOf("keyword1", "keyword2")
        )
        notificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe notificationSettings
    }
    should("parse NotificationSettings 2") {
        val notificationSettings = NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.DM,
            sound = NotificationSettings.Sound(
                room = false,
                dm = false,
                mention = false,
                call = false,
            ),
            activity = Activity(
                invite = false,
                status = false,
                notice = false,
            ),
            mention = NotificationSettings.Mention(
                user = false,
                room = false,
                keyword = false,
            ),
            keywords = setOf()
        )

        notificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe notificationSettings
    }
    should("parse NotificationSettings 3") {
        val notificationSettings = NotificationSettings(
            defaultLevel = NotificationSettings.DefaultLevel.NONE,
            sound = NotificationSettings.Sound(
                room = true,
                dm = false,
                mention = true,
                call = true,
            ),
            activity = Activity(
                invite = true,
                status = true,
                notice = true,
            ),
            mention = NotificationSettings.Mention(
                user = true,
                room = true,
                keyword = false,
            ),
            keywords = setOf()
        )

        notificationSettings.toPushRuleSet(userId).toNotificationSettings() shouldBe notificationSettings
    }
})
