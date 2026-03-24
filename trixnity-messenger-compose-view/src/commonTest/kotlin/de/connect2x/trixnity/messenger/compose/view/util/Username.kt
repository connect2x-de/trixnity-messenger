package de.connect2x.trixnity.messenger.compose.view.util

private const val TST_PREFIX = "tst_"

private val charPool: List<Char> = ('a'..'z').toList()

fun generateUsername(prefix: String = "generated"): String {
    val extendedPrefix = "$TST_PREFIX${prefix}_" // "-" in names will not be found in local search
    return extendedPrefix + List(24 - extendedPrefix.length) { charPool.random() }.joinToString("")
}
