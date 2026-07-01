package de.connect2x.trixnity.messenger.compose.view.messenger

import de.connect2x.trixnity.messenger.compose.view.util.SynapseAdmin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun createUser(username: String, password: String) = withContext(Dispatchers.Default) {
    SynapseAdmin.registerNewUser(username, password)
}
