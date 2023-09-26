package de.connect2x.trixnity.messenger.i18n

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.TimeZone

private val log = KotlinLogging.logger { }

abstract class I18nBase(private val languages: Languages, messengerSettings: MessengerSettings) {

    var currentLang: Language = getLang(languages, messengerSettings)
        private set

    val currentTimezone = TimeZone.of(timezone())

    /**
     * Used to explicitly set the language, e.g., for testing.
     */
    fun setCurrentLang(newLang: String) {
        this.currentLang =
            languages.langOf(newLang) ?: throw IllegalArgumentException("language $newLang not supported")
    }

    fun translate(block: TranslateBuilder.() -> Unit): String {
        return TranslateBuilder().apply(block).map.translate()
    }

    class TranslateBuilder {

        val map: MutableMap<Language, String> = mutableMapOf()
        operator fun Language.minus(translation: String) {
            map[this] = translation
        }
    }

    private fun Map<Language, String>.translate(): String {
        val translated = this[currentLang]
        return if (translated == null) {
            log.warn { "cannot find translation for language $currentLang: $this" }
            this[EN] ?: "<missing translation>"
        } else {
            translated
        }
    }
}