package de.connect2x.trixnity.messenger.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.keys.Key
import net.folivo.trixnity.core.model.keys.KeyAlgorithm

class RelevantTimelineEventsTest : ShouldSpec() {

    private val roomId = RoomId("room1", "localhost")
    private val eventId = EventId("eventId")
    private val alice = UserId("alice", "localhost")

    private val cut = object : RelevantTimelineEvents {}

    init {
        should("consider text messages as relevant") {
            cut.isRelevantTimelineEvent(RoomMessageEventContent.TextBased.Text(body = "Hola")) shouldBe true
        }

        should("consider encrypted event as relevant") {
            cut.isRelevantTimelineEvent(
                MegolmEncryptedMessageEventContent(
                    ciphertext = "cipherCipher",
                    senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                    deviceId = "",
                    sessionId = ""
                )
            ) shouldBe true
        }

        should("consider unknown message events as not relevant") {
            cut.isRelevantTimelineEvent(
                UnknownEventContent(
                    raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))),
                    "m.reaction"
                )
            ) shouldBe false
        }

        should("consider replies as not relevant") {
            cut.isRelevantTimelineEvent(
                RoomMessageEventContent.TextBased.Text(
                    body = "Hola", relatesTo = RelatesTo.Reply(
                        RelatesTo.ReplyTo(EventId(""))
                    )
                )
            ) shouldBe true
        }

        should("consider member events as relevant") {
            cut.isRelevantTimelineEvent(MemberEventContent(membership = Membership.JOIN)) shouldBe true
        }

        should("consider some state events as not relevant") {
            cut.isRelevantTimelineEvent(CanonicalAliasEventContent()) shouldBe false
        }
    }
}
