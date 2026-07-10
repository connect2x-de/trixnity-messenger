package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.crypto.core.AesHmacSha2EncryptedData
import de.connect2x.trixnity.crypto.core.decryptAesHmacSha2
import de.connect2x.trixnity.crypto.core.encryptAesHmacSha2
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.SecretByteArraySettings
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
        configureTestLogging()
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
    fun `set - should initialized`() = runTest {
        cut.set(SecretId(null, "secret"), null)
        val settings = settings.value.secretByteArrays.shouldNotBeNull()
        settings.secrets shouldBe emptyMap()
        settings.keyInfo shouldBe emptyMap()
    }

    @Test
    fun `set - should initialize null returning provider`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        everySuspend { provider1.rotate(any(), any(), any()) } returns
            SecretByteArrayKeyProvider.RotateResult(null, null, null)

        cut.set(SecretId(null, "secret"), null)
        val settings = settings.value.secretByteArrays.shouldNotBeNull()
        settings.secrets shouldBe emptyMap()
        settings.keyInfo shouldBe emptyMap()
    }

    @Test
    fun `set - should remove secret on null`() = runTest {
        settings.update<SecretByteArraySettings> {
            SecretByteArraySettings(
                secrets = mapOf(SecretId(null, "secret") to SecretByteArray.Unencrypted("***".encodeToByteArray())),
                keyInfo = mapOf(),
                mac = null,
            )
        }
        cut.set(SecretId(null, "secret"), null)
        settings.value.secretByteArrays.shouldNotBeNull().secrets shouldBe emptyMap()
    }

    @Test
    fun `set - should store secret unencrypted when no provider given`() = runTest {
        settings.update<SecretByteArraySettings> { SecretByteArraySettings(mapOf(), mapOf(), null) }
        cut.set(SecretId(null, "secret"), "***".encodeToByteArray())
        settings.value.secretByteArrays.secrets?.get(SecretId(null, "secret")) shouldBe
            SecretByteArray.Unencrypted("***".encodeToByteArray())
    }

    @Test
    fun `set - should store secret unencrypted when null returning provider given`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        everySuspend { provider1.rotate(any(), any(), any()) } returns
            SecretByteArrayKeyProvider.RotateResult(null, null, null)

        cut.set(SecretId(null, "secret"), "***".encodeToByteArray())
        settings.value.secretByteArrays.secrets?.get(SecretId(null, "secret")) shouldBe
            SecretByteArray.Unencrypted("***".encodeToByteArray())
    }

    @Test
    fun `set - should store secret encrypted when provider provides a key`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        val secretByteArraysSettings =
            SecretByteArraySettings(
                secrets = mapOf(),
                keyInfo = mapOf("provider-1" to SecretByteArrayKeyInfo()),
                key = aesKey1.invoke(32),
            )
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }
        cut.set(SecretId(null, "my.secret"), "***".encodeToByteArray())
        val secret =
            settings.value.secretByteArrays.secrets
                ?.get(SecretId(null, "my.secret"))
                .shouldBeInstanceOf<SecretByteArray.AesHmacSha2>()
        decryptAesHmacSha2(
            content = AesHmacSha2EncryptedData(secret.iv, secret.ciphertext, secret.mac),
            key = aesKey1.invoke(32),
            name = SecretId(null, "my.secret").value,
        ) shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `get - should initialized`() = runTest {
        cut.get(SecretId(null, "secret"))
        val settings = settings.value.secretByteArrays.shouldNotBeNull()
        settings.secrets shouldBe emptyMap()
        settings.keyInfo shouldBe emptyMap()
    }

    @Test
    fun `get - should be null when there is no secret`() = runTest { cut.get(SecretId(null, "secret")) shouldBe null }

    @Test
    fun `get - should get unencrypted secret`() = runTest {
        settings.update<SecretByteArraySettings> {
            SecretByteArraySettings(
                mapOf(SecretId(null, "secret") to SecretByteArray.Unencrypted("***".encodeToByteArray())),
                mapOf(),
                null,
            )
        }
        cut.get(SecretId(null, "secret")) shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `get - should get encrypted secret`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        val encryptedSecret = encryptAesHmacSha2("***".encodeToByteArray(), aesKey1.invoke(32), "secret")
        val secretByteArraysSettings =
            SecretByteArraySettings(
                secrets =
                    mapOf(
                        SecretId(null, "secret") to
                            SecretByteArray.AesHmacSha2(
                                encryptedSecret.iv,
                                encryptedSecret.ciphertext,
                                encryptedSecret.mac,
                            )
                    ),
                keyInfo = mapOf("provider-1" to SecretByteArrayKeyInfo()),
                key = aesKey1.invoke(32),
            )
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }

        cut.get(SecretId(null, "secret")) shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `get - should get after set`() = runTest {
        secretByteArrayKeyProviders = listOf(provider1)
        val secretByteArraysSettings =
            SecretByteArraySettings(
                secrets = mapOf(),
                keyInfo = mapOf("provider-1" to SecretByteArrayKeyInfo()),
                key = aesKey1.invoke(32),
            )
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }
        cut.set(SecretId(null, "my.secret"), "***".encodeToByteArray())
        cut.get(SecretId(null, "my.secret")) shouldBe "***".encodeToByteArray()
    }

    @Test
    fun `getKey - should use key chain`() = runTest {
        val extra1 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        secretByteArrayKeyProviders = listOf(provider1, provider2)
        val secretByteArraysSettings =
            SecretByteArraySettings(
                secrets = mapOf(),
                keyInfo =
                    mapOf(
                        "provider-1" to SecretByteArrayKeyInfo(extra = extra1),
                        "provider-2" to SecretByteArrayKeyInfo(dependsOn = "provider-1"),
                    ),
                key = aesKey2.invoke(32),
            )
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }
        cut.set(SecretId(null, "my.secret"), "***".encodeToByteArray())
        cut.get(SecretId(null, "my.secret")) shouldBe "***".encodeToByteArray()

        verifySuspend {
            provider1.get(extra1, null)
            provider2.get(null, aesKey1)
        }
    }

    private suspend fun `getKey - should do integrity check`(
        manipulate: (SecretByteArraySettings) -> SecretByteArraySettings
    ) {
        secretByteArrayKeyProviders = listOf(provider1)
        val encryptedSecret = encryptAesHmacSha2("***".encodeToByteArray(), aesKey1.invoke(32), "secret")
        val secretByteArraysSettings =
            SecretByteArraySettings(
                    secrets =
                        mapOf(
                            SecretId(null, "secret") to
                                SecretByteArray.AesHmacSha2(
                                    encryptedSecret.iv,
                                    encryptedSecret.ciphertext,
                                    encryptedSecret.mac,
                                )
                        ),
                    keyInfo = mapOf("provider-1" to SecretByteArrayKeyInfo()),
                    mac = "abc".encodeToByteArray(),
                )
                .let { manipulate(it) }
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }

        shouldThrow<SecretByteArrayManipulationException> {
            cut.set(SecretId(null, "secret"), "***".encodeToByteArray())
        }
        shouldThrow<SecretByteArrayManipulationException> { cut.get(SecretId(null, "secret")) }
    }

    @Test
    fun `getKey - should do integrity check with null mac`() = runTest {
        `getKey - should do integrity check` { it.copy(mac = null) }
    }

    @Test
    fun `getKey - should do integrity check with wrong mac`() = runTest {
        `getKey - should do integrity check` { it.copy(mac = "wrong".encodeToByteArray()) }
    }

    @Test
    fun `getKey - should do integrity check with wrong data`() = runTest {
        `getKey - should do integrity check` {
            it.copy(secrets = mapOf(SecretId(null, "secret") to SecretByteArray.Unencrypted("***".encodeToByteArray())))
        }
    }

    @Test
    fun `getInputKeyAndExtra - should use key chain`() = runTest {
        val extra1 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        val extra2 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        secretByteArrayKeyProviders = listOf(provider1, provider2)
        val secretByteArraysSettings =
            SecretByteArraySettings(
                secrets = mapOf(),
                keyInfo =
                    mapOf(
                        "provider-1" to SecretByteArrayKeyInfo(extra = extra1),
                        "provider-2" to SecretByteArrayKeyInfo(dependsOn = "provider-1", extra2),
                    ),
                key = aesKey2.invoke(32),
            )
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }
        cut.getInputKeyAndExtra("provider-2") shouldBe
            SecretByteArrays.GetInputKeyAndExtraResult(getInputKey = aesKey1, extra = extra2)
    }

    @Test
    fun `rotateKeys - should use key chain`() = runTest {
        val extra1 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        val extra2 = JsonObject(mapOf("p2" to JsonPrimitive("v2")))
        secretByteArrayKeyProviders = listOf(provider1, provider2)
        val secretByteArraysSettings =
            SecretByteArraySettings(
                secrets = mapOf(),
                keyInfo = mapOf("provider-1" to SecretByteArrayKeyInfo(extra = extra1)),
                key = aesKey1.invoke(32),
            )
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }
        everySuspend { provider1.rotate(any(), any(), any()) } returns
            SecretByteArrayKeyProvider.RotateResult(aesKey3, aesKey1, null)

        cut.rotateKeys("provider-2") { oldExtra, getOldInputKey, getNewInputKey ->
            oldExtra shouldBe null
            getOldInputKey shouldBe aesKey3
            getNewInputKey shouldBe aesKey1
            SecretByteArrayKeyProvider.RotateResult(null, aesKey2, extra2)
        }

        verifySuspend { provider1.rotate(extra1, null, null) }

        settings.value.secretByteArrays shouldBe
            de.connect2x.trixnity.messenger.secrets.SecretByteArraySettings(
                mapOf(),
                mapOf(
                    "provider-1" to SecretByteArrayKeyInfo(),
                    "provider-2" to SecretByteArrayKeyInfo(dependsOn = "provider-1", extra = extra2),
                ),
                aesKey2.invoke(32),
            )
    }

    private fun `rotateKeys - should do integrity check`(
        manipulate: (SecretByteArraySettings) -> SecretByteArraySettings
    ) = runTest {
        val extra1 = JsonObject(mapOf("p1" to JsonPrimitive("v1")))
        val extra2 = JsonObject(mapOf("p2" to JsonPrimitive("v2")))
        secretByteArrayKeyProviders = listOf(provider1, provider2)
        val secretByteArraysSettings =
            SecretByteArraySettings(
                    secrets = mapOf(),
                    keyInfo = mapOf("provider-1" to SecretByteArrayKeyInfo(extra = extra1)),
                    mac = "abc".encodeToByteArray(),
                )
                .also { manipulate(it) }
        settings.update<SecretByteArraySettings> { secretByteArraysSettings }
        everySuspend { provider1.rotate(any(), any(), any()) } returns
            SecretByteArrayKeyProvider.RotateResult(aesKey3, aesKey1, null)

        shouldThrow<SecretByteArrayManipulationException> {
            cut.rotateKeys("provider-2") { oldExtra, getOldInputKey, getNewInputKey ->
                oldExtra shouldBe null
                getOldInputKey shouldBe aesKey3
                getNewInputKey shouldBe aesKey1
                SecretByteArrayKeyProvider.RotateResult(null, aesKey2, extra2)
            }
        }
    }

    @Test
    fun `rotateKeys - should do integrity check with null mac`() = runTest {
        `rotateKeys - should do integrity check` { it.copy(mac = null) }
    }

    @Test
    fun `rotateKeys - should do integrity check with wrong mac`() = runTest {
        `rotateKeys - should do integrity check` { it.copy(mac = "wrong".encodeToByteArray()) }
    }

    @Test
    fun `rotateKeys - should do integrity check with wrong data`() = runTest {
        `rotateKeys - should do integrity check` {
            it.copy(secrets = mapOf(SecretId(null, "secret") to SecretByteArray.Unencrypted("***".encodeToByteArray())))
        }
    }

    @Test
    fun `removeSecretsForUser should remove all secrets for the user and leave the settings integrity correctly`() =
        runTest {
            val unaffectedUser = UserId("notAffected", "localhost")
            val myUser = UserId("myUser", "localhost")

            secretByteArrayKeyProviders = listOf(provider1)
            val secretByteArraysSettings =
                SecretByteArraySettings(
                    secrets = mapOf(),
                    keyInfo = mapOf("provider-1" to SecretByteArrayKeyInfo()),
                    key = aesKey1.invoke(32),
                )
            settings.update<SecretByteArraySettings> { secretByteArraysSettings }
            cut.set(SecretId(unaffectedUser, "unaffected.secret"), "unaffected".encodeToByteArray())
            cut.set(SecretId(myUser, "my.secret"), "secret".encodeToByteArray())
            cut.set(SecretId(myUser, "my.secret2"), "secret2".encodeToByteArray())

            cut.removeSecretsForUser(myUser)

            settings.value.secretByteArrays.secrets shouldNotBeNull { shouldHaveSize(1) }
            val secret =
                settings.value.secretByteArrays.secrets
                    ?.get(SecretId(unaffectedUser, "unaffected.secret"))
                    .shouldBeInstanceOf<SecretByteArray.AesHmacSha2>()
            decryptAesHmacSha2(
                content = AesHmacSha2EncryptedData(secret.iv, secret.ciphertext, secret.mac),
                key = aesKey1.invoke(32),
                name = SecretId(unaffectedUser, "unaffected.secret").value,
            ) shouldBe "unaffected".encodeToByteArray()
        }
}
