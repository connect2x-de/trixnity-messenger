package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.core.model.UserId

fun getRepositoryDatabaseName(userId: UserId) = "$userId/database"
fun getMediaDatabaseName(userId: UserId) = "$userId/media"