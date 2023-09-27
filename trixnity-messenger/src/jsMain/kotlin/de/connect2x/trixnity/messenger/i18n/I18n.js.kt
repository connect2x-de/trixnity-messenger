package de.connect2x.trixnity.messenger.i18n

import kotlinx.browser.window

internal actual fun getSystemLang(): String? {
    return window.navigator.language
}
