package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JoinRuleTest {
    @Test
    fun `isKnock - should return true for knocks and false for everything else`() {
        JoinRulesEventContent.JoinRule.Knock.isKnock shouldBe true
        JoinRulesEventContent.JoinRule.KnockRestricted.isKnock shouldBe true

        JoinRulesEventContent.JoinRule.Invite.isKnock shouldBe false
        JoinRulesEventContent.JoinRule.Private.isKnock shouldBe false
        JoinRulesEventContent.JoinRule.Public.isKnock shouldBe false
        JoinRulesEventContent.JoinRule.Restricted.isKnock shouldBe false
        JoinRulesEventContent.JoinRule.Unknown("").isKnock shouldBe false
    }
}
