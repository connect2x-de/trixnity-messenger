package de.connect2x.trixnity.messenger.i18n

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages.EN
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import de.connect2x.trixnity.messenger.viewmodel.util.timezoneOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.TimeZone

private val log = KotlinLogging.logger { }

abstract class I18nBase(
    private val languages: Languages,
    private val settings: MatrixMessengerSettingsHolder,
    private val getSystemLang: GetSystemLang
) {

    val currentLang: Language
        get() = getLang(languages, settings, getSystemLang)

    val currentTimezone = timezoneOf(timezone())

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
