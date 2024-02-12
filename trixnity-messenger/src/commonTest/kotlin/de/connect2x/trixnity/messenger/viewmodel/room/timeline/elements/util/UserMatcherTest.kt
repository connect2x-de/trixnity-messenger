package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import net.folivo.trixnity.core.model.UserId

class UserMatcherTest : ShouldSpec() {
    init {
        should("match valid user identifier") {
            val message = "Hello @u1ser:example.com"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["@u1ser:example.com"] shouldBe UserId("u1ser", "example.com")
        }

        should("match valid user identifier with matrix.to link") {
            val message = "Hello <a href=\"https://matrix.to/#/@user:example.com\">Hallo</a>"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["<a href=\"https://matrix.to/#/@user:example.com\">Hallo</a>"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match valid user identifier with matrix.to link without href") {
            val message = "Hello https://matrix.to/#/@user:example.com"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["https://matrix.to/#/@user:example.com"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match valid user identifier with matrix:u link and action attribute") {
            val message = "Hello matrix:u/user:example.com?action=chat"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["matrix:u/user:example.com?action=chat"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match valid user identifier with matrix:u link and via attribute") {
            val message = "Hello matrix:u/user:example.com?via=example.com"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["matrix:u/user:example.com?via=example.com"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match valid user identifier with matrix:u link, via and action attribute") {
            val message1 = "Hello matrix:u/user:example.com?action=chat&via=example.com"
            val result1 = matchUsers(message1)
            result1.size shouldBe 1
            result1["matrix:u/user:example.com?action=chat&via=example.com"]
                .shouldBe(UserId("user", "example.com"))

            val message2 = "Hello matrix:u/user:example.com?via=example.com&action=chat"
            val result2 = matchUsers(message2)
            result2.size shouldBe 1
            result2["matrix:u/user:example.com?via=example.com&action=chat"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match valid user identifier with matrix:u link") {
            val message = "Hello matrix:u/user:example.com"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["matrix:u/user:example.com"]
                .shouldBe(UserId("user", "example.com"))
        }

        should("match valid user identifier with special characters") {
            val message = "Hello @a9._=-/+:sub.example.com:8000"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["@a9._=-/+:sub.example.com:8000"]
                .shouldBe(UserId("a9._=-/+", "sub.example.com:8000"))
        }

        should("match valid user identifier with IPV4") {
            val message = "Hello @a9._=-/+:1.1.1.1"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["@a9._=-/+:1.1.1.1"]
                .shouldBe(UserId("a9._=-/+", "1.1.1.1"))
        }

        should("match valid user identifier with IPV6") {
            val message = "Hello @a9._=-/+:[2001:0db8:85a3:0000:0000:8a2e:0370:7334]"
            val result = matchUsers(message)
            result.size shouldBe 1
            result["@a9._=-/+:[2001:0db8:85a3:0000:0000:8a2e:0370:7334]"]
                .shouldBe(UserId("a9._=-/+", "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]"))
        }

        should("not match invalid user identifier") {
            val message = "Hello @user&:ex&mple.com"
            val result = matchUsers(message)
            result.size shouldBe 0
        }
    }
}
