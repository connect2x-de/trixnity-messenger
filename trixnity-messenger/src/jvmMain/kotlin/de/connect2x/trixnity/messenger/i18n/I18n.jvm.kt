package de.connect2x.trixnity.messenger.i18n

import java.util.*

internal actual fun getSystemLang(): String? {
    return Locale.getDefault().language
}