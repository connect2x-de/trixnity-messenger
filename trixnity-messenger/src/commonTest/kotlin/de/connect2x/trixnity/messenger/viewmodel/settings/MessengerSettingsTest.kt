package de.connect2x.trixnity.messenger.viewmodel.settings

import com.russhwolf.settings.MapSettings
import de.connect2x.trixnity.messenger.MessengerConfig
import io.kotest.assertions.timing.continually
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

class MessengerSettingsTest: ShouldSpec() {
    override fun timeout(): Long = 2000L

    private val account1 = "account1"
    private val account2 = "account2"

    init {
        beforeTest {
            MessengerConfig.instance.apply {
                defaultPushMode = PushMode.PUSH
                defaultPresenceIsPublic = false
            }
        }

        should("set and retrieve settings for an account") {
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            val cut = messengerSettings()
            val flow = cut.presenceIsPublicFlow(account1).stateIn(coroutineScope, SharingStarted.Eagerly, false)
            continually(200.milliseconds) {
                flow.value shouldBe false
            }
            cut.setPresenceIsPublic(account1, true)
            cut.presenceIsPublic(account1) shouldBe true
            eventually(200.milliseconds) {
                flow.value shouldBe true
            }

            cut.setPresenceIsPublic(account1, false)
            cut.presenceIsPublic(account1) shouldBe false
            eventually(200.milliseconds) {
                flow.value shouldBe false
            }
        }

        should("set and retrieve settings for push mode") {
            val coroutineScope = CoroutineScope(Dispatchers.Default)
            val cut = messengerSettings()
            val flow = cut.pushModeFlow().stateIn(coroutineScope, SharingStarted.Eagerly, mapOf(null to PushMode.PUSH))
            continually(200.milliseconds) {
                flow.value shouldBe mapOf(null to PushMode.PUSH)
            }
            cut.setPushMode(account1, PushMode.POLLING)
            cut.pushMode(account1) shouldBe PushMode.POLLING
            eventually(200.milliseconds) {
                // the default `null` is not returned anymore since we have at least 1 active account
                flow.value shouldBe mapOf(account1 to PushMode.POLLING)
            }

            cut.setPushMode(account2, PushMode.NONE)
            cut.pushMode(account2) shouldBe PushMode.NONE
            eventually(200.milliseconds) {
                flow.value shouldBe mapOf(account1 to PushMode.POLLING, account2 to PushMode.NONE)
            }

            cut.setPushMode(account1, null)
            eventually(200.milliseconds) {
                flow.value shouldBe mapOf(account2 to PushMode.NONE)
            }
            cut.setPushMode(account2, null)
            eventually(200.milliseconds) {
                // no active account settings for push mode -> return `null` (default) value
                flow.value shouldBe mapOf(null to PushMode.PUSH)
            }
        }
    }

    private fun messengerSettings(): MessengerSettings {
        return MessengerSettingsImpl(MapSettings())
    }
}