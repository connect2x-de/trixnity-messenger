package de.connect2x.messenger.compose.view.util

fun String?.ifNotBlank(defaultValue: String = "", builder: (String) -> String): String = if (this?.isNotBlank() == true) builder(this) else defaultValue
