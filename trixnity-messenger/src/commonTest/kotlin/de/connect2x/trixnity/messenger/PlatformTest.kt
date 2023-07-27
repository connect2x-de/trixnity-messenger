package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.cleanAccountName
import de.connect2x.trixnity.messenger.util.getAccountName
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class PlatformTest : ShouldSpec() {

    init {
        should("encode and decode account names correctly") {
            "Standard".cleanAccountName().getAccountName() shouldBe "Standard"
            val cleanAccountName = "wild) 000 - and unprädict...,./".cleanAccountName()
            cleanAccountName shouldNotContain "/"
            cleanAccountName.getAccountName() shouldBe "wild) 000 - and unprädict...,./"
        }
    }

}