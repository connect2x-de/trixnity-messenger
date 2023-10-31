package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.Secrets
import de.connect2x.trixnity.messenger.util.cleanAccountName
import org.koin.core.module.Module

internal expect suspend fun createRepositoriesModule(accountName: String, dbPassword: DbPassword): Module

interface DbPassword {
    suspend fun getPassword(accountName: String): String?
    suspend fun setPassword(accountName: String, password: String)
}

class DbPasswordImpl(private val secrets: Secrets) : DbPassword {

    private fun getSecretName(accountName: String): String {
        val messengerConfig = MessengerConfig.instance
        return "${messengerConfig.packageName}.${messengerConfig.appName}.${accountName.cleanAccountName()}.db"
    }

    override suspend fun getPassword(accountName: String): String? {
        return secrets.getSecret(getSecretName(accountName))
    }

    override suspend fun setPassword(accountName: String, password: String) {
        return secrets.setSecret(getSecretName(accountName), password)
    }

}