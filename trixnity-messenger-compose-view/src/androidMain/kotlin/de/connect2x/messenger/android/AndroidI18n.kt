package de.connect2x.messenger.android

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.DE
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import de.connect2x.trixnity.messenger.i18n.I18nBase.TranslateBuilder
import de.connect2x.trixnity.messenger.i18n.Language
import io.github.oshai.kotlinlogging.KotlinLogging
import net.folivo.trixnity.core.model.UserId
import java.util.*

internal object AndroidI18n {
    private val log = KotlinLogging.logger { }

    private fun translate(block: TranslateBuilder.() -> Unit): String {
        return TranslateBuilder().apply(block).map.translate()
    }

    private fun Map<Language, String>.translate(): String {
        val currentLang = Language(Locale.getDefault().language) // Always use the system language here
        val translated = this[currentLang]
        return if (translated == null) {
            log.warn { "cannot find translation for language $currentLang: $this" }
            this[EN] ?: "<missing translation>"
        } else {
            translated
        }
    }

    fun notificationInitialSyncTitle() = translate {
        EN - "Initial Sync"
        DE - "Initialer Sync"
    }

    fun notificationInitialSyncContentTitle(userId: UserId) = translate {
        EN - "Loading account $userId"
        DE - "Laden des Kontos $userId"
    }

    fun notificationInitialSyncDescription(appName: String) = translate {
        EN - "Setting up $appName: Loading account data from server"
        DE - "Einrichten von $appName: Lade Kontodaten vom Server"
    }
}
