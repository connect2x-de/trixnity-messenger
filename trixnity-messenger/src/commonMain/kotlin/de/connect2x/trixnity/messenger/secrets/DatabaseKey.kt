package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.core.model.UserId

private const val ID = "de.connect2x.trixnity.messenger.secrets.database"

suspend fun SecretByteArrays.setDatabaseKey(userId: UserId, key: ByteArray) = set(SecretId(userId, ID), key)

suspend fun SecretByteArrays.deleteDatabaseKey(userId: UserId) = set(SecretId(userId, ID), null)

suspend fun SecretByteArrays.getDatabaseKey(userId: UserId): ByteArray? =
    get(SecretId(userId, ID)) ?: get(ID) // TODO legacy path can be removed in future
