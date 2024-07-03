package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import dev.mokkery.matcher.*

import dev.mokkery.answering.*

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.UnknownEventContent
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
        should("consider 'null' as not relevant") {
            cut.isRelevantTimelineEvent(null) shouldBe false
        }

        should("consider text messages as relevant") {
            val timelineEvent = timelineEvent(
                MessageEvent(RoomMessageEventContent.TextBased.Text(body = "Hola"), eventId, alice, roomId, 0L),
                content = Result.success(RoomMessageEventContent.TextBased.Text(body = "Hola"))
            )
            cut.isRelevantTimelineEvent(timelineEvent) shouldBe true
        }

        should("consider decrypted text messages as relevant") {
            val timelineEvent = timelineEvent(
                MessageEvent(
                    MegolmEncryptedMessageEventContent(
                        ciphertext = "cipherCipher",
                        senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                        deviceId = "",
                        sessionId = ""
                    ),
                    eventId, alice, roomId, 0L
                ),
                content = Result.success(RoomMessageEventContent.TextBased.Text(body = "Hola"))
            )
            cut.isRelevantTimelineEvent(timelineEvent) shouldBe true
        }

        should("consider unknown message events as not relevant") {
            val timelineEvent = timelineEvent(
                MessageEvent(
                    UnknownEventContent(
                        raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))),
                        "m.reaction"
                    ),
                    eventId, alice, roomId, 0L
                )
            )
            cut.isRelevantTimelineEvent(timelineEvent) shouldBe false
        }

        should("consider unknown decrypted message events as not relevant") {
            val timelineEvent = timelineEvent(
                MessageEvent(
                    MegolmEncryptedMessageEventContent(
                        ciphertext = "cipherCipher",
                        senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                        deviceId = "",
                        sessionId = ""
                    ),
                    eventId, alice, roomId, 0L
                ),
                content = Result.success(
                    UnknownEventContent(
                        raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))), eventType = "m.reaction"
                    ),
                )
            )
            cut.isRelevantTimelineEvent(timelineEvent) shouldBe false
        }

        should("consider member events as relevant") {
            val timelineEvent = timelineEvent(
                StateEvent(
                    MemberEventContent(membership = Membership.JOIN), eventId, alice, roomId, 0L, stateKey = "",
                ),
                content = Result.success(MemberEventContent(membership = Membership.JOIN))
            )
            cut.isRelevantTimelineEvent(timelineEvent) shouldBe true
        }

        should("consider some state events as not relevant") {
            val timelineEvent = timelineEvent(
                StateEvent(
                    RedactedEventContent(eventType = "m.redacted"), eventId, alice, roomId, 0L, stateKey = "",
                )
            )
            cut.isRelevantTimelineEvent(timelineEvent) shouldBe false
        }

        should("consider encrypted messages as relevant as we do not know anything about their type yet") {
            val timelineEvent = timelineEvent(
                MessageEvent(
                    MegolmEncryptedMessageEventContent(
                        ciphertext = "cipherCipher",
                        senderKey = Key.Curve25519Key(value = "", algorithm = KeyAlgorithm.Curve25519),
                        deviceId = "",
                        sessionId = ""
                    ),
                    eventId, alice, roomId, 0L
                ),
                content = null
            )
            cut.isRelevantTimelineEvent(timelineEvent) shouldBe true
        }
    }

    private fun timelineEvent(
        roomEvent: RoomEvent<*>,
        content: Result<RoomEventContent>? = null
    ): TimelineEvent = TimelineEvent(
        roomEvent,
        content,
        previousEventId = null,
        nextEventId = null,
        gap = null,
    )

}
