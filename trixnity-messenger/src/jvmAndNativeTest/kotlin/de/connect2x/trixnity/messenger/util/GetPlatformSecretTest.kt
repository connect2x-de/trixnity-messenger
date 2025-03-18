package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.secrets.platformPlatformSecretByteArrayKeyProviderModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.koin.dsl.koinApplication

class GetPlatformSecretTest : ShouldSpec({
    val cut = koinApplication {
        modules(platformPlatformSecretByteArrayKeyProviderModule())
    }.koin.get<GetPlatformSecret>()

    val id: String = GetPlatformSecretTest::class.qualifiedName ?: "trixnity-messenger-GetPlatformSecretTest"

    // disabled, because iOSSimulator seems to have no working keychain for this test
    // enable for local testing
    xshould("create and get secret") {
        val createdSecret = cut(id, 32).shouldNotBeNull()
        createdSecret.size shouldBe 32

        cut(id, 16) shouldBe createdSecret

        cut("$id-other", 32) shouldNotBe createdSecret
    }
})
