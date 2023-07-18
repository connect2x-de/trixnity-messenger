package de.connect2x.trixnity.messenger.util

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getSystemLang(): String? {
    return NSLocale.currentLocale.languageCode
}
