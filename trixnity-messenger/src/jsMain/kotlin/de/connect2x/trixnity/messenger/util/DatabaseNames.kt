package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.core.model.UserId

fun getRepositoryDatabaseName(prefix: String, userId: UserId) = "$prefix$userId/database"
fun getMediaDatabaseName(prefix: String, userId: UserId) = "$prefix$userId/media"