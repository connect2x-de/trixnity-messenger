package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.secrets.SecretByteArrayKeyProvider
import de.connect2x.trixnity.messenger.secrets.platformSecretByteArrayKeyProviderModule
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class GetPlatformSecretTest : ShouldSpec({
    val cut = koinApplication {
        modules(
            platformSecretByteArrayKeyProviderModule(),
            module {
                single { MatrixMessengerConfiguration() }
            },
        )
    }.koin.getAll<SecretByteArrayKeyProvider>().first()

    // disabled, because iOSSimulator seems to have no working keychain for this test
    // enable for local testing
    xshould("create and get secret") {
        val createdSecret = cut.get(null, null)?.invoke(32)
        createdSecret?.size shouldBe 32

        cut.get(null, null)?.invoke(16) shouldBe createdSecret?.copyOf(16)
    }
})
