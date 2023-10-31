package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val log = KotlinLogging.logger { }

internal object LocalAccountNames {
    private const val KEY = "accountNames"

    fun get() = localStorage.getItem(KEY)
        ?.let {
            try {
                Json.decodeFromString<Set<String>>(it)
            } catch (_: Exception) {
                log.warn { "failed loading account list -> create a new empty one" }
                emptySet()
            }
        }
        ?: emptySet()

    fun update(updater: (Set<String>) -> Set<String>) =
        localStorage.setItem(KEY, Json.encodeToString(updater(get())))
}