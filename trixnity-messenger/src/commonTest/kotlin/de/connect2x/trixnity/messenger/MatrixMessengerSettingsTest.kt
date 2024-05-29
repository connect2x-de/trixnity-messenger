package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.SettingsStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.core.model.UserId

class MatrixMessengerSettingsTest : ShouldSpec() {
    object MockSettingsStorage : SettingsStorage {
        override suspend fun write(settings: String) {}
        override suspend fun read(): String? = null
    }

    val userId = UserId("alice", "dino.unicorn")
    lateinit var settings: MutableStateFlow<MatrixMessengerSettings?>
    lateinit var cut: MatrixMessengerSettingsHolder

    init {
        beforeTest {
            settings = MutableStateFlow(MatrixMessengerSettings(mapOf()))
            cut = MatrixMessengerSettingsHolderImpl(MockSettingsStorage, settings)
        }


        should("updateView") {
            cut.updateView<MatrixMessengerSettingsBase> {
                it.copy(preferredLang = "dino")
            }
            settings.value?.base?.preferredLang shouldBe "dino"
        }
        should("updateView of account") {
            cut.updateView<MatrixMessengerAccountSettingsBase>(userId) {
                it.copy(displayName = "DINO")
            }
            settings.value?.base?.accounts?.values?.first()?.get("displayName") shouldBe JsonPrimitive("DINO")
        }
    }
}
