package de.connect2x.trixnity.messenger.util

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Ignore
import kotlin.test.Test

class GetPlatformSecretTest {
    val cut = koinApplication {
        modules(platformGetPlatformSecret())
    }.koin.get<GetPlatformSecret>()

    val id: String = GetPlatformSecretTest::class.qualifiedName ?: "trixnity-messenger-GetPlatformSecretTest"

    // disabled, because iOSSimulator seems to have no working keychain for this test
    // enable for local testing
    @Test
    @Ignore
    fun `create and get secret`() = runTest {
        val createdSecret = cut(id, 32).shouldNotBeNull()
        createdSecret.size shouldBe 32

        cut(id, 16) shouldBe createdSecret

        cut("$id-other", 32) shouldNotBe createdSecret
    }
}
