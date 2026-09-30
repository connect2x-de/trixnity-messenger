package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.push.PushAction
import de.connect2x.trixnity.core.model.push.PushCondition
import de.connect2x.trixnity.core.model.push.PushRule
import de.connect2x.trixnity.core.model.push.PushRuleSet
import de.connect2x.trixnity.core.model.push.toList
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PushRuleDiffTest {
    private val emptyDiff = PushRuleDiff(emptyList(), emptyList(), emptyList(), emptyList())
    private val overrideRule = PushRule.Override(ruleId = ".m.rule.override")
    private val underrideRule = PushRule.Underride(ruleId = ".m.rule.underride")
    private val contentRule = PushRule.Content(ruleId = "keyword", pattern = "keyword")
    private val notify = setOf(PushAction.Notify)

    @Test
    fun `identical and reordered rules produce no changes`() {
        val current =
            PushRuleSet(
                override = listOf(overrideRule, overrideRule.copy(ruleId = ".m.rule.other_override")),
                underride = listOf(underrideRule, underrideRule.copy(ruleId = ".m.rule.other_underride")),
                content = listOf(contentRule, contentRule.copy(ruleId = "other", pattern = "other")),
            )
        shouldBeDiff(diffPushRules(current, current), emptyDiff)
        shouldBeDiff(
            diffPushRules(
                current,
                current.copy(
                    override = current.override?.reversed(),
                    underride = current.underride?.reversed(),
                    content = current.content?.reversed(),
                ),
            ),
            emptyDiff,
        )
    }

    @Test
    fun `empty inputs produce no changes`() {
        val emptyRules = PushRuleSet(override = emptyList(), underride = emptyList(), content = emptyList())
        for (current in listOf(null, PushRuleSet(), emptyRules)) {
            shouldBeDiff(diffPushRules(current, PushRuleSet()), emptyDiff)
            shouldBeDiff(diffPushRules(current, emptyRules), emptyDiff)
        }
    }

    @Test
    fun `enabled changes only update enabled`() {
        val changed = overrideRule.copy(enabled = true)
        shouldBeDiff(
            diffPushRules(PushRuleSet(override = listOf(overrideRule)), PushRuleSet(override = listOf(changed))),
            emptyDiff.copy(enabledUpdates = listOf(changed)),
        )
    }

    @Test
    fun `action changes only update actions`() {
        val changed = underrideRule.copy(actions = notify)
        shouldBeDiff(
            diffPushRules(PushRuleSet(underride = listOf(underrideRule)), PushRuleSet(underride = listOf(changed))),
            emptyDiff.copy(actionUpdates = listOf(changed)),
        )
    }

    @Test
    fun `enabled and action changes produce both updates`() {
        val changed = overrideRule.copy(enabled = true, actions = notify)
        shouldBeDiff(
            diffPushRules(PushRuleSet(override = listOf(overrideRule)), PushRuleSet(override = listOf(changed))),
            emptyDiff.copy(enabledUpdates = listOf(changed), actionUpdates = listOf(changed)),
        )
    }

    @Test
    fun `rules with the same id in different kinds are compared independently`() {
        val current =
            PushRuleSet(
                override = listOf(overrideRule),
                underride = listOf(underrideRule.copy(ruleId = overrideRule.ruleId, enabled = true, actions = notify)),
            )
        val changed = overrideRule.copy(enabled = true, actions = notify)
        shouldBeDiff(
            diffPushRules(current, current.copy(override = listOf(changed))),
            emptyDiff.copy(enabledUpdates = listOf(changed), actionUpdates = listOf(changed)),
        )
    }

    @Test
    fun `a rule in another kind does not count as an existing rule`() {
        val current = PushRuleSet(override = listOf(overrideRule))
        val added = underrideRule.copy(ruleId = overrideRule.ruleId)
        shouldBeDiff(
            diffPushRules(current, current.copy(underride = listOf(added))),
            emptyDiff.copy(enabledUpdates = listOf(added), actionUpdates = listOf(added)),
        )
    }

    @Test
    fun `default rule conditions and metadata do not produce updates`() {
        val conditions = setOf(PushCondition.EventMatch(key = "type", pattern = "m.room.message"))
        val current = PushRuleSet(override = listOf(overrideRule), underride = listOf(underrideRule))
        val desired =
            current.copy(
                override = listOf(overrideRule.copy(default = true, conditions = conditions)),
                underride = listOf(underrideRule.copy(default = true, conditions = conditions)),
            )
        shouldBeDiff(diffPushRules(current, desired), emptyDiff)
    }

    @Test
    fun `content additions modifications and deletions are detected together`() {
        val removed = contentRule.copy(ruleId = "removed")
        val unchanged = contentRule.copy(ruleId = "unchanged")
        val added = contentRule.copy(ruleId = "added")
        val changed = contentRule.copy(pattern = "changed", actions = notify)
        val current = PushRuleSet(content = listOf(contentRule, removed, unchanged))
        val desired = PushRuleSet(content = listOf(changed, added, unchanged))

        shouldBeDiff(
            diffPushRules(current, desired),
            emptyDiff.copy(contentUpdates = listOf(changed, added), deletions = listOf(removed)),
        )
    }

    @Test
    fun `each content field change triggers an upsert`() {
        val current = PushRuleSet(content = listOf(contentRule))
        for (changed in
            listOf(
                contentRule.copy(pattern = "changed"),
                contentRule.copy(actions = notify),
                contentRule.copy(enabled = true),
                contentRule.copy(default = true),
            )) {
            shouldBeDiff(
                diffPushRules(current, PushRuleSet(content = listOf(changed))),
                emptyDiff.copy(contentUpdates = listOf(changed)),
            )
        }
    }

    @Test
    fun `removing all keywords only deletes content rules`() {
        shouldBeDiff(
            diffPushRules(PushRuleSet(content = listOf(contentRule)), PushRuleSet()),
            emptyDiff.copy(deletions = listOf(contentRule)),
        )
    }

    @Test
    fun `unmanaged rules are ignored when added changed or removed`() {
        val current =
            PushRuleSet(
                override = listOf(overrideRule.copy(ruleId = "custom_override")),
                underride = listOf(underrideRule.copy(ruleId = "custom_underride")),
                content = listOf(contentRule.copy(ruleId = ".m.rule.contains_user_name")),
                room = listOf(PushRule.Room(roomId = RoomId("!room:example.org"))),
                sender = listOf(PushRule.Sender(userId = UserId("@alice:example.org"))),
            )
        val changed =
            current.copy(
                override = current.override?.map { it.copy(enabled = true, actions = notify) },
                underride = current.underride?.map { it.copy(enabled = true, actions = notify) },
                content = current.content?.map { it.copy(pattern = "changed", actions = notify) },
                room = current.room?.map { it.copy(enabled = true, actions = notify) },
                sender = current.sender?.map { it.copy(enabled = true, actions = notify) },
            )
        shouldBeDiff(diffPushRules(PushRuleSet(), current), emptyDiff)
        shouldBeDiff(diffPushRules(current, changed), emptyDiff)
        shouldBeDiff(diffPushRules(current, PushRuleSet()), emptyDiff)
    }

    @Test
    fun `omitted server default rules are not deleted`() {
        val current = PushRuleSet(override = listOf(overrideRule), underride = listOf(underrideRule))
        shouldBeDiff(diffPushRules(current, PushRuleSet()), emptyDiff)
    }

    @Test
    fun `null and empty current rules produce all initial updates`() {
        val desired =
            PushRuleSet(
                override = listOf(overrideRule),
                underride = listOf(underrideRule),
                content = listOf(contentRule),
            )
        val expected =
            emptyDiff.copy(
                enabledUpdates = listOf(overrideRule, underrideRule),
                actionUpdates = listOf(overrideRule, underrideRule),
                contentUpdates = listOf(contentRule),
            )
        shouldBeDiff(diffPushRules(null, desired), expected)
        shouldBeDiff(diffPushRules(PushRuleSet(), desired), expected)
    }

    @Test
    fun `empty diff is applied to any rule set`() {
        isPushRuleDiffApplied(emptyDiff, PushRuleSet().toList()) shouldBe true
        isPushRuleDiffApplied(emptyDiff, PushRuleSet(override = listOf(overrideRule)).toList()) shouldBe true
    }

    @Test
    fun `enabled confirmation requires the rule and matching enabled value`() {
        val changes = emptyDiff.copy(enabledUpdates = listOf(overrideRule))
        isPushRuleDiffApplied(changes, PushRuleSet().toList()) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(override = listOf(overrideRule.copy(enabled = true))).toList(),
        ) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(override = listOf(overrideRule.copy(actions = notify))).toList(),
        ) shouldBe true
    }

    @Test
    fun `action confirmation requires the rule and matching actions`() {
        val changes = emptyDiff.copy(actionUpdates = listOf(underrideRule))
        isPushRuleDiffApplied(changes, PushRuleSet().toList()) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(underride = listOf(underrideRule.copy(actions = notify))).toList(),
        ) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(underride = listOf(underrideRule.copy(enabled = true))).toList(),
        ) shouldBe true
    }

    @Test
    fun `content confirmation requires matching pattern and actions but ignores enabled and default`() {
        val changes = emptyDiff.copy(contentUpdates = listOf(contentRule))
        isPushRuleDiffApplied(changes, PushRuleSet().toList()) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(content = listOf(contentRule.copy(pattern = "other"))).toList(),
        ) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(content = listOf(contentRule.copy(actions = notify))).toList(),
        ) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(content = listOf(contentRule.copy(enabled = true, default = true))).toList(),
        ) shouldBe true
    }

    @Test
    fun `confirmation matches rules by kind and id`() {
        val changes = emptyDiff.copy(enabledUpdates = listOf(overrideRule), actionUpdates = listOf(overrideRule))
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(underride = listOf(underrideRule.copy(ruleId = overrideRule.ruleId))).toList(),
        ) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(override = listOf(overrideRule.copy(ruleId = ".m.rule.other"))).toList(),
        ) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(
                    override = listOf(overrideRule),
                    underride =
                        listOf(underrideRule.copy(ruleId = overrideRule.ruleId, enabled = true, actions = notify)),
                )
                .toList(),
        ) shouldBe true
        isPushRuleDiffApplied(
            emptyDiff.copy(contentUpdates = listOf(contentRule)),
            PushRuleSet(content = listOf(contentRule.copy(ruleId = "other"))).toList(),
        ) shouldBe false
    }

    @Test
    fun `deletion confirmation only requires absence of the matching kind and id`() {
        val changes = emptyDiff.copy(deletions = listOf(contentRule))
        isPushRuleDiffApplied(changes, PushRuleSet(content = listOf(contentRule)).toList()) shouldBe false
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(content = listOf(contentRule.copy(pattern = "other", enabled = true, actions = notify)))
                .toList(),
        ) shouldBe false
        isPushRuleDiffApplied(changes, PushRuleSet().toList()) shouldBe true
        isPushRuleDiffApplied(
            changes,
            PushRuleSet(
                    override = listOf(overrideRule.copy(ruleId = contentRule.ruleId)),
                    content = listOf(contentRule.copy(ruleId = "other")),
                )
                .toList(),
        ) shouldBe true
    }

    @Test
    fun `confirmation requires every update and deletion to be applied`() {
        val enabled = overrideRule.copy(enabled = true)
        val actions = underrideRule.copy(actions = notify)
        val added = contentRule.copy(ruleId = "added")
        val removed = contentRule.copy(ruleId = "removed")
        val changes =
            PushRuleDiff(
                enabledUpdates = listOf(enabled),
                actionUpdates = listOf(actions),
                contentUpdates = listOf(contentRule, added),
                deletions = listOf(removed),
            )
        val applied =
            PushRuleSet(override = listOf(enabled), underride = listOf(actions), content = listOf(contentRule, added))

        isPushRuleDiffApplied(changes, applied.copy(override = listOf(overrideRule)).toList()) shouldBe false
        isPushRuleDiffApplied(changes, applied.copy(underride = listOf(underrideRule)).toList()) shouldBe false
        isPushRuleDiffApplied(changes, applied.copy(content = listOf(contentRule)).toList()) shouldBe false
        isPushRuleDiffApplied(changes, applied.copy(content = listOf(contentRule, added, removed)).toList()) shouldBe
            false
        isPushRuleDiffApplied(changes, applied.toList()) shouldBe true
    }

    @Test
    fun `confirmation ignores ordering unrelated rules and unsent metadata`() {
        val other = overrideRule.copy(ruleId = ".m.rule.other", enabled = true)
        val changes = emptyDiff.copy(enabledUpdates = listOf(overrideRule, other))
        val conditions = setOf(PushCondition.EventMatch(key = "type", pattern = "m.room.message"))
        val rules =
            PushRuleSet(
                override =
                    listOf(
                        other.copy(actions = notify),
                        overrideRule.copy(ruleId = "unrelated", enabled = true),
                        overrideRule.copy(default = true, conditions = conditions),
                    ),
                underride = listOf(underrideRule.copy(enabled = true, actions = notify)),
                content = listOf(contentRule.copy(pattern = "unrelated")),
                room = listOf(PushRule.Room(roomId = RoomId("!room:example.org"))),
                sender = listOf(PushRule.Sender(userId = UserId("@alice:example.org"))),
            )
        isPushRuleDiffApplied(changes, rules.toList()) shouldBe true
    }

    private fun shouldBeDiff(actual: PushRuleDiff, expected: PushRuleDiff) {
        actual shouldBe expected
        actual.isEmpty() shouldBe (expected == emptyDiff)
    }
}
