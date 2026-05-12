package de.connect2x.trixnity.messenger.compose.view.messenger

import de.connect2x.trixnity.messenger.compose.view.util.SynapseAdmin

suspend fun createUser(username: String, password: String) {
    SynapseAdmin.registerNewUser(username, password)
}
