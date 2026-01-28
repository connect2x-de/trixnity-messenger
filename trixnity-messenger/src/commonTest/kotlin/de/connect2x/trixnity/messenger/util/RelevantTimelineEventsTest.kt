package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.events.UnknownEventContent
import de.connect2x.trixnity.core.model.events.block.EventContentBlocks
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import de.connect2x.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.model.keys.KeyValue
import de.connect2x.trixnity.core.model.keys.MegolmMessageValue
import de.connect2x.trixnity.messenger.configureTestLogging
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test

class RelevantTimelineEventsTest {
    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

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
                blocks = EventContentBlocks(),
                raw = JsonObject(mapOf("dino" to JsonPrimitive("unicorn"))),
                eventType = "m.reaction",
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
