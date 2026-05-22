package de.connect2x.trixnity.messenger.secrets

import de.connect2x.trixnity.core.model.UserId

private const val ID = "de.connect2x.trixnity.messenger.secrets.database"

suspend fun SecretByteArrays.setDatabaseKey(userId: UserId, key: ByteArray) = set("$ID-$userId", key)

suspend fun SecretByteArrays.deleteDatabaseKey(userId: UserId) = set("$ID-$userId", null)

suspend fun SecretByteArrays.getDatabaseKey(userId: UserId): ByteArray? =
    get("$ID-$userId") ?: get(ID) // TODO legacy path can be removed in future
