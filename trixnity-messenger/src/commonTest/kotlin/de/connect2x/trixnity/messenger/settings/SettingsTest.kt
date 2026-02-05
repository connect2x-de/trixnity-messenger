package de.connect2x.trixnity.messenger.settings

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.update
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import de.connect2x.trixnity.core.model.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

class SettingsTest {
    class MockSettingsStorage : SettingsStorage {
        var value: String? = null
        override suspend fun write(settings: String) {
            value = settings
        }

        override suspend fun read(): String? = value
    }

    val userId = UserId("alice", "dino.unicorn")
    lateinit var settings: MutableStateFlow<MatrixMessengerSettings?>
    lateinit var settingsStorage: MockSettingsStorage
    lateinit var cut: MatrixMessengerSettingsHolder

    @BeforeTest
    fun setup() {
        configureTestLogging()
        settings = MutableStateFlow(MatrixMessengerSettings(mapOf()))
        settingsStorage = MockSettingsStorage()
        cut = MatrixMessengerSettingsHolderImpl(settingsStorage, settings)
    }

    @Test
    fun `read base settings`() = runTest {
        cut.value.base shouldBe MatrixMessengerSettingsBase()
    }

    @Test
    fun `update base settings`() = runTest {
        cut.update<MatrixMessengerSettingsBase> { it.copy(accentColor = 1) }
        cut.value.base.accentColor shouldBe 1
        settingsStorage.value shouldBe """
            {
                "accounts": {},
                "preferredLang": null,
                "selectedAccount": null,
                "ssoLoginState": null,
                "oAuth2LoginState": null,
                "themeMode": "default",
                "isHighContrast": false,
                "isFocusHighlighting": false,
                "accentColor": 1,
                "fontSize": null,
                "displaySize": null,
                "applySystemSizes": true
            }
        """.trimIndent()
    }

    @Test
    fun `update view`() = runTest {
        cut.update<MatrixMessengerSettingsBase> {
            it.copy(preferredLang = "dino")
        }
        println(settingsStorage.value)
        settings.value?.base?.preferredLang shouldBe "dino"
    }

    @Test
    fun `don't update view of account when not set yet`() = runTest {
        cut.update<MatrixMessengerAccountSettingsBase>(userId) {
            it.copy(displayName = "DINO")
        }
        settings.value?.base?.accounts?.values shouldBe emptyList()
    }

    @Test
    fun `set view of account`() = runTest {
        cut.create(userId, MatrixMessengerAccountSettingsBase(displayName = "DINO"))
        settings.value?.base?.accounts?.values?.first()?.base?.displayName shouldBe "DINO"
        settings.value?.base?.accounts?.values?.first()?.get("displayName") shouldBe JsonPrimitive("DINO")
    }

    @Test
    fun `update view of account`() = runTest {
        cut.create(userId, MatrixMessengerAccountSettingsBase())
        cut.update<MatrixMessengerAccountSettingsBase>(userId) {
            it.copy(displayName = "DINO")
        }
        settings.value?.base?.accounts?.values?.first()?.base?.displayName shouldBe "DINO"
        settings.value?.base?.accounts?.values?.first()?.get("displayName") shouldBe JsonPrimitive("DINO")
    }

    @Test
    fun `delete view of account`() = runTest {
        cut.create(userId, MatrixMessengerAccountSettingsBase())
        cut.delete(userId)
        settings.value?.base?.accounts?.values shouldBe emptyList()
    }

    @Test
    fun `update nested settings view`() = runTest {
        @NestedSettingsView("dino")
        @Serializable
        data class TestNestedSettingsView(
            val enabled: Boolean = false
        ) : SettingsView<MatrixMessengerSettings>

        cut.update<TestNestedSettingsView> {
            it.copy(enabled = true)
        }
        settings.value?.get<MatrixMessengerSettings, TestNestedSettingsView>()?.enabled shouldBe true
        settings.value?.get("dino")?.jsonObject?.get("enabled")
            .shouldBeInstanceOf<JsonPrimitive>().boolean shouldBe true
    }

    @Test
    fun `update deep nested settings view`() = runTest {
        @NestedSettingsView("dino", "unicorn")
        @Serializable
        data class TestNestedSettingsView(
            val enabled: Boolean = false
        ) : SettingsView<MatrixMessengerSettings>

        cut.update<TestNestedSettingsView> {
            it.copy(enabled = true)
        }
        settings.value?.get<MatrixMessengerSettings, TestNestedSettingsView>()?.enabled shouldBe true
        settings.value?.get("dino")?.jsonObject?.get("unicorn")?.jsonObject?.get("enabled")
            .shouldBeInstanceOf<JsonPrimitive>().boolean shouldBe true
    }
}
