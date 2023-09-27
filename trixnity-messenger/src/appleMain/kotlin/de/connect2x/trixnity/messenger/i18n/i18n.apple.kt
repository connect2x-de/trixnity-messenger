package de.connect2x.trixnity.messenger.i18n

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

internal actual fun getSystemLang(): String? {
    return NSLocale.currentLocale.languageCode
}
