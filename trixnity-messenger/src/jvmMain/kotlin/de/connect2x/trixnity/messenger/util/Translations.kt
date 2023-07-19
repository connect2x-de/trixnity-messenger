package de.connect2x.trixnity.messenger.util

import java.util.*

actual fun getSystemLang(): String? {
    return Locale.getDefault().language
}
