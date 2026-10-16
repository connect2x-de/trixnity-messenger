package de.connect2x.trixnity.messenger.notification

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.push.PushRuleSet
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountNotificationSettings

fun interface AccountNotificationPushRuleModifier {
    fun modify(userId: UserId, settings: AccountNotificationSettings, rules: PushRuleSet): PushRuleSet
}

class NoopAccountNotificationPushRuleModifier : AccountNotificationPushRuleModifier {
    override fun modify(userId: UserId, settings: AccountNotificationSettings, rules: PushRuleSet): PushRuleSet {
        return rules
    }
}
