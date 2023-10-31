package de.connect2x.trixnity.messenger

import web.navigator.navigator

actual fun isNetworkAvailable(): Boolean {
    return navigator.onLine
}