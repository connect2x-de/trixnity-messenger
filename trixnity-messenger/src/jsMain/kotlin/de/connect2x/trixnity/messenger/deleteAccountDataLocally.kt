package de.connect2x.trixnity.messenger

import com.juul.indexeddb.deleteDatabase

actual suspend fun deleteAccountDataLocally(accountName: String) {
    LocalAccountNames.update { it - accountName }
    deleteDatabase(getDbName(accountName))
    deleteDatabase(getMediaStoreName(accountName))
}