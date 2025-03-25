package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.util.createTestMatrixMessengerSettingsHolder
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import net.folivo.trixnity.crypto.core.AesHmacSha2EncryptedData
import net.folivo.trixnity.crypto.core.decryptAesHmacSha2
import net.folivo.trixnity.crypto.core.encryptAesHmacSha2
import kotlin.test.BeforeTest
import kotlin.test.Test

class SecretByteArraysTest {
    private val aesKey = ByteArray(32) { (it + 1).toByte() }
    private lateinit var settings: MatrixMessengerSettingsHolder
    private lateinit var secretByteArrayKeyProviders: List<SecretByteArrayKeyProvider>
    private lateinit var cut: SecretByteArrays

    @BeforeTest
    fun setup() {
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
        secretByteArrayKeyProviders = listOf(
            TestSecretByteArrayKeyProvider(key = aesKey)
        )
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf("provider-1" to SecretByteArrayKeyInfo())
            )
        }
        cut.set("my.secret", "***".encodeToByteArray())
        val secret = settings.value.base.secretByteArrays["my.secret"].shouldBeInstanceOf<SecretByteArray.AesHmacSha2>()
        decryptAesHmacSha2(
            content = AesHmacSha2EncryptedData(secret.iv, secret.ciphertext, secret.mac),
            key = aesKey,
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
        secretByteArrayKeyProviders = listOf(
            TestSecretByteArrayKeyProvider(key = aesKey)
        )
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf("provider-1" to SecretByteArrayKeyInfo())
            )
        }
        val encryptedSecret = encryptAesHmacSha2("***".encodeToByteArray(), aesKey, "secret")
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
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
        secretByteArrayKeyProviders = listOf(
            TestSecretByteArrayKeyProvider(key = aesKey)
        )
        settings.update<MatrixMessengerSettingsBase> {
            MatrixMessengerSettingsBase(
                secretByteArrayKeyInfos = mapOf("provider-1" to SecretByteArrayKeyInfo())
            )
        }
        cut.set("my.secret", "***".encodeToByteArray())
        cut.get("my.secret") shouldBe "***".encodeToByteArray()
    }

    private class TestSecretByteArrayKeyProvider(
        override val id: String = "provider-1",
        override val level: Int = 0,
        private val key: ByteArray,
    ) : SecretByteArrayKeyProvider {
        override suspend fun get(extra: JsonObject?, getInputKey: GetKey?): GetKey = GetKey { key }

        override suspend fun rotate(
            oldExtra: JsonObject?,
            getOldInputKey: GetKey?,
            getNewInputKey: GetKey?
        ): SecretByteArrayKeyProvider.RotateResult {
            TODO("Not yet implemented")
        }

        override suspend fun getLegacy(): ByteArray? {
            TODO("Not yet implemented")
        }
    }
}
