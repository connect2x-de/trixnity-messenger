package de.connect2x.trixnity.messenger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.realm.kotlin.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.crypto.core.SecureRandom
import org.koin.core.module.Module

private val log = KotlinLogging.logger { }
actual suspend fun createRepositoriesModule(accountName: String, dbPassword: DbPassword): Module =
    withContext(Dispatchers.IO) {
        val messengerConfig = MessengerConfig.instance
        val dbFolder = getDbPath(accountName).toString()
        log.debug { "createRepositoriesModule with config: $messengerConfig" }

        val password = if (messengerConfig.encryptDb) {
            dbPassword.getPassword(accountName)
                ?: createPassword().also {
                    dbPassword.setPassword(accountName, it)
                }
        } else null
        createRealmRepositoriesModule {
            directory(dbFolder)
            if (password != null) encryptionKey(password.toByteArray())
        }
    }

private fun createPassword(): String {
    val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return generateSequence { alphabet[SecureRandom.nextInt(alphabet.size)] }
        .take(Realm.ENCRYPTION_KEY_LENGTH).joinToString("")
}