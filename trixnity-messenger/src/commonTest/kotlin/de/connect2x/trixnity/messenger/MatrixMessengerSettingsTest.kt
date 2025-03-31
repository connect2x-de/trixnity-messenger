package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.SettingsStorage
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.core.model.UserId
import kotlin.test.Test

class MatrixMessengerSettingsTest {
    object MockSettingsStorage : SettingsStorage {
        override suspend fun write(settings: String) {}
        override suspend fun read(): String? = null
    }

    val userId = UserId("alice", "dino.unicorn")
    val settings = MutableStateFlow<MatrixMessengerSettings?>(MatrixMessengerSettings(mapOf()))
    val cut = MatrixMessengerSettingsHolderImpl(MockSettingsStorage, settings)

    @Test
    fun `update view`() = runTest {
        cut.update<MatrixMessengerSettingsBase> {
            it.copy(preferredLang = "dino")
        }
        settings.value?.base?.preferredLang shouldBe "dino"
    }

    @Test
    fun `update view of account`() = runTest {
        cut.update<MatrixMessengerAccountSettingsBase>(userId) {
            it.copy(displayName = "DINO")
        }
        settings.value?.base?.accounts?.values?.first()?.get("displayName") shouldBe JsonPrimitive("DINO")
    }
}
