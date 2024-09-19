package de.connect2x.messenger.compose.view

fun String.cleanName(): String {
    return this.replace("[-.\\s]".toRegex(), "").lowercase()
}
