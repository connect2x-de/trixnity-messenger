package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.core.MegolmMessageValue
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.keys.KeyValue
import kotlin.test.Test

class RelevantTimelineEventsTest {
    val cut = object : RelevantTimelineEvents {}

    @Test
    fun `consider text messages as relevant`() = runTest {
        cut.isRelevantTimelineEvent(RoomMessageEventContent.TextBased.Text(body = "Hola")) shouldBe true
    }

    @Test
    fun `consider encrypted event as relevant`() = runTest {
        cut.isRelevantTimelineEvent(
            MegolmEncryptedMessageEventContent(
                ciphertext = MegolmMessageValue("cipherCipher"),
                senderKey = KeyValue.Curve25519KeyValue(""),
                deviceId = "",
                sessionId = ""
            )
        ) shouldBe true
    }

    @Test
    fun `consider unknown message events as not relevant`() = runTest {
        cut.isRelevantTimelineEvent(
            UnknownEventContent(
                raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))), "m.reaction"
            )
        ) shouldBe false
    }

    @Test
    fun `consider replies as not relevant`() = runTest {
        cut.isRelevantTimelineEvent(
            RoomMessageEventContent.TextBased.Text(
                body = "Hola", relatesTo = RelatesTo.Reply(
                    RelatesTo.ReplyTo(EventId(""))
                )
            )
        ) shouldBe true
    }

    @Test
    fun `consider state events as not relevant`() = runTest {
        cut.isRelevantTimelineEvent(CanonicalAliasEventContent()) shouldBe false
    }
}
