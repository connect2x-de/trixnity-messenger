package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2
import kotlin.test.BeforeTest
import kotlin.test.Test

class SecretByteArraysTest {
    private val aesKey1 = GetKey { ByteArray(32) { (it + 1).toByte() } }
    private val aesKey2 = GetKey { ByteArray(32) { (it + 2).toByte() } }
    private val aesKey3 = GetKey { ByteArray(32) { (it + 3).toByte() } }

    private lateinit var settings: MatrixMessengerSettingsHolder
    private lateinit var secretByteArrayKeyProviders: List<SecretByteArrayKeyProvider>
    private lateinit var cut: SecretByteArrays

    private val provider1 = mock<SecretByteArrayKeyProvider>()
    private val provider2 = mock<SecretByteArrayKeyProvider>()

    @BeforeTest
    fun setup() {
        resetMocks(provider1, provider2)

        every { provider1.id } returns "provider-1"
        every { provider1.level } returns 0
        everySuspend { provider1.get(any(), any()) } returns aesKey1
        every { provider2.id } returns "provider-2"
        every { provider2.level } returns 1
        everySuspend { provider2.get(any(), any()) } returns aesKey2

        settings = createTestMatrixMessengerSettingsHolder()
        secretByteArrayKeyProviders = listOf()
        cut = SecretByteArraysImpl(settings, lazy { secretByteArrayKeyProviders })
    }

    @Test
    fun `set - should remove secret on null`() = runTest {
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrays = mapOf("secret" to SecretByteArray.Unencrypted("***".encodeToByteArray()))
            )
        }
        cut.set("secret", null)
        settings.value.base.secretByteArrays.shouldBeEmpty()
    }

    @Test
    fun `set - should store secret unencrypted when no provider given`() = runTest {
        cut.set("secret", "***".encodeToByteArray())
        settings.value.base.secretByteArrays["secret"] shouldBe SecretByteArray.Unencrypted("***".encodeToByteArray())
    }

    @Test
    fun `set - should store secret encrypted when provider provides a key`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf("provider-1" to SecretByteArrayKeyInfo())
            )
        }
        cut.set("my.secret", "***".encodeToByteArray())
        val secret = settings.value.base.secretByteArrays["my.secret"].shouldBeInstanceOf<SecretByteArray.AesHmacSha2>()
        decryptAesHmacSha2(
            content = AesHmacSha2EncryptedData(secret.iv, secret.ciphertext, secret.mac),
            key = aesKey1.invoke(32),
            name = "my.secret"
        ) shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `get - should be null when there is no secret`() = runTest {
        cut.get("secret") shouldBe null
    }

    @Test
    fun `get - should get unencrypted secret`() = runTest {
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrays = mapOf("secret" to SecretByteArray.Unencrypted("***".encodeToByteArray()))
            )
        }
        cut.get("secret") shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `get - should get encrypted secret`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        val encryptedSecret = encryptAesHmacSha2("***".encodeToByteArray(), aesKey1.invoke(32), "secret")
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf("provider-1" to SecretByteArrayKeyInfo()),
                secretByteArrays = mapOf(
                    "secret" to SecretByteArray.AesHmacSha2(
                        encryptedSecret.iv,
                        encryptedSecret.ciphertext,
                        encryptedSecret.mac
                    )
                )
            )
        }

        cut.get("secret") shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `get - should get after set`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf("provider-1" to SecretByteArrayKeyInfo())
            )
        }
        cut.set("my.secret", "***".encodeToByteArray())
        cut.get("my.secret") shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `getKey - should use key chain`() = runTest {
        val extra1 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        secretByteArrayKeyProviders = listOf(provider1, provider2)
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf(
                    "provider-1" to SecretByteArrayKeyInfo(extra = extra1),
                    "provider-2" to SecretByteArrayKeyInfo(dependsOn = "provider-1"),
                )
            )
        }
        cut.set("my.secret", "***".encodeToByteArray())
        cut.get("my.secret") shouldBe "***".encodeToByteArray()

        verifySuspend {
            provider1.get(extra1, null)
            provider2.get(null, aesKey1)
        }
    }

    @Test
    fun `getInputKeyAndExtra - should use key chain`() = runTest {
        val extra1 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        val extra2 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        secretByteArrayKeyProviders = listOf(provider1, provider2)
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf(
                    "provider-1" to SecretByteArrayKeyInfo(extra = extra1),
                    "provider-2" to SecretByteArrayKeyInfo(dependsOn = "provider-1", extra2),
                )
            )
        }
        cut.getInputKeyAndExtra("provider-2") shouldBe
                SecretByteArrays.GetInputKeyAndExtraResult(getInputKey = aesKey1, extra = extra2)
    }

    @Test
    fun `rotateKeys - should use key chain`() = runTest {
        val extra1 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        val extra2 = JsonObject(mapOf("p2" to JsonPrimitive("v2")))
        secretByteArrayKeyProviders = listOf(provider1, provider2)
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf(
                    "provider-1" to SecretByteArrayKeyInfo(extra = extra1),
                )
            )
        }
        everySuspend { provider1.rotate(any(), any(), any()) } returns
                SecretByteArrayKeyProvider.RotateResult(aesKey3, aesKey1, null)

        cut.rotateKeys(
            "provider-2",
            { oldExtra, getOldInputKey, getNewInputKey ->
                oldExtra shouldBe null
                getOldInputKey shouldBe aesKey3
                getNewInputKey shouldBe aesKey1
                SecretByteArrayKeyProvider.RotateResult(null, aesKey2, extra2)
            }
        )

        verifySuspend {
            provider1.rotate(extra1, null, null)
        }

        settings.value.base.secretByteArrayKeyInfos shouldBe mapOf(
            "provider-1" to SecretByteArrayKeyInfo(),
            "provider-2" to SecretByteArrayKeyInfo(dependsOn = "provider-1", extra = extra2),
        )
    }
}
