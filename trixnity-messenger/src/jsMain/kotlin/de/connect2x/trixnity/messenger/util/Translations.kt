package de.connect2x.trixnity.messenger.util

import kotlinx.browser.window

actual fun getSystemLang(): String? {
    return window.navigator.language
}
