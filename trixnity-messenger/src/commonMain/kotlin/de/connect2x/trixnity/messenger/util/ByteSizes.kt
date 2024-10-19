package de.connect2x.trixnity.messenger.util

fun Long.gb() = mb() * 1_000
fun Long.mb() = kb() * 1_000
fun Long.kb() = this * 1_000

fun Int.gb() = toLong().gb()
fun Int.mb() = toLong().mb()
fun Int.kb() = toLong().kb()
