package de.connect2x.trixnity.messenger.i18n

data class Language(val code: String)

interface Languages {
    fun langOf(lang: String): Language?
}

object DefaultLanguages : Languages {
    val DE = Language("de")
    val EN = Language("en")

    override fun langOf(lang: String): Language? {
        val languageCode = lang.substringBefore('-').lowercase()

        return when (languageCode) {
            DE.code -> DE
            EN.code -> EN
            else -> null
        }
    }
}
