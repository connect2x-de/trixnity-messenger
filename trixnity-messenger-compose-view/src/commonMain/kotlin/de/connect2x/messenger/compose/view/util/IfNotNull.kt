package de.connect2x.messenger.compose.view.util

fun <T> T?.ifNotNull(defaultValue: String = "", builder: (T) -> String): String = if (this != null) builder(this) else defaultValue
