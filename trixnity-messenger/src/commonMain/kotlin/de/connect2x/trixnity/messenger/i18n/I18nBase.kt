package de.connect2x.trixnity.messenger.i18n

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import kotlinx.datetime.TimeZone

private val log: Logger = Logger("de.connect2x.trixnity.messenger.i18n.I18nBase")

abstract class I18nBase(
    private val languages: Languages,
    private val settings: MatrixMessengerSettingsHolder,
    private val getSystemLang: GetSystemLang,
    timeZone: TimeZone,
) {
    val currentLang: Language
        get() = getLang(languages, settings, getSystemLang)

    val currentTimezone = timeZone

    suspend fun setCurrentLang(language: Language) {
        setLang(language, settings)
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
