package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ReferencedMessage.ReferencedTextMessage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class ReferencedTextMessageTest : ShouldSpec() {

    init {
        should("not shorten a referenced message when message length does not exceed maxLines") {
            val cut = ReferencedTextMessage("Martin", "Hello World!\n2\n3\n4")
            cut.messageShortened(maxLines = 4) shouldBe "Hello World!\n2\n3\n4"
        }

        should("shorten referenced message when message length exceeds maxLines") {
            val cut = ReferencedTextMessage("Martin", "Hello World!\n2\n3\n4\n5")
            cut.messageShortened(maxLines = 4) shouldBe "Hello World!\n2\n3\n..."
        }

    }

}