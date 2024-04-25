package de.connect2x.trixnity.messenger.multi

import net.folivo.trixnity.core.model.UserId

fun getProfileDatabaseName(prefix: String, profile:String,userId: UserId) = "$prefix$profile$userId/database"
fun getProfileMediaDatabaseName(prefix: String, profile:String,userId: UserId) = "$prefix$profile$userId/media"
