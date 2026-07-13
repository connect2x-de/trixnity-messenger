package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.core.model.UserId

private const val ID = "de.connect2x.trixnity.messenger.secrets.database"

suspend fun SecretByteArrays.setDatabaseKey(userId: UserId, key: ByteArray) = set(SecretId(ID, userId), key)

suspend fun SecretByteArrays.getDatabaseKey(userId: UserId): ByteArray? =
    get(SecretId(ID, userId)) ?: get(SecretId(ID, null)) // TODO legacy path can be removed in future
