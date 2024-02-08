package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import net.folivo.trixnity.core.model.UserId

class UserMatcherTest : ShouldSpec() {
    init {
        should("match user identifier") {
            val message = "Hello @user:example.com"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["@user:example.com"] shouldBe UserId("user", "example.com")
        }

        should("match user identifier with matrix.to link") {
            val message = "Hello <a href=\"https://matrix.to/#/@user:example.com\">Hallo</a>"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["<a href=\"https://matrix.to/#/@user:example.com\">Hallo</a>"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match user identifier with matrix.to link without href") {
            val message = "Hello https://matrix.to/#/@user:example.com"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["https://matrix.to/#/@user:example.com"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match user identifier with matrix:u link") {
            val message = "Hello matrix:u/user:example.com?action=chat"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["matrix:u/user:example.com?action=chat"]
                .shouldBe(UserId("user", "example.com"))
        }
    }
}
