package de.connect2x.trixnity.messenger

internal actual suspend fun getAccountNames(): List<String> = LocalAccountNames.get().toList()
