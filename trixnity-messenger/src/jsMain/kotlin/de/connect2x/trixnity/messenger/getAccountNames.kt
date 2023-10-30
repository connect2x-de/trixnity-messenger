package de.connect2x.trixnity.messenger

actual suspend fun getAccountNames(): List<String> = LocalAccountNames.get().toList()
