package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.trixnity.messenger.getAppFolder


fun cleanup() {
    deleteAppFolder()

}

fun deleteAppFolder() {
    getAppFolder(null).toFile().deleteRecursively()
}