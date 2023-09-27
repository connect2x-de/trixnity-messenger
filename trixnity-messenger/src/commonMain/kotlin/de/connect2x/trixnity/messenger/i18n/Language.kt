package de.connect2x.trixnity.messenger.i18n

data class Language(val code: String)

interface Languages {
    fun langOf(lang: String): Language?
}

object DefaultLanguages : Languages {
    val DE = Language("de")
    val EN = Language("en")

    override fun langOf(lang: String): Language? {
        return when (lang) {
            DE.code -> DE
            EN.code -> EN
            else -> null
        }
    }
}