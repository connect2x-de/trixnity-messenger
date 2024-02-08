package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import korlibs.io.util.getOrNullLoggingError
import net.folivo.trixnity.api.client.MatrixApiClient
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.UserId

private val userRegex by lazy {
    // @user:domain
    ("(?:@([\\w\\.\\-\\+=_/]+):((?:[\\w-]+\\.)?[\\w-]+\\.[\\w-]+))|" +
            // matrix:u/user:domain?action=chat
            "(?:matrix:u/([\\w\\.\\-\\+=_/]+):((?:[\\w-]+\\.)?[\\w-]+\\.[\\w-]+)\\?action=chat)|" +
            // <a href="https://matrix.to/#/@user:domain">Hallo</a> or https://matrix.to/#/@user:domain
            "(?:(?:<a href=\")?https?://matrix\\.to/#/@([\\w\\.\\-\\+=_/]+):((?:[\\w-]+\\.)?[\\w-]+\\.[\\w-]+)(?:\">.*</a>)?)")
        .toRegex()
}

fun matchUsers(message: String): Map<String, UserId> {
    val matches = userRegex.findAll(message)

    return matches.associate {
        val matched = it.groupValues[0]
        val localpart = it.groupValues[1] + it.groupValues[3] + it.groupValues[5]
        val domain = it.groupValues[2] + it.groupValues[4] + it.groupValues[6]
        println(matched)
        matched to UserId(localpart, domain)
    }
}

fun UserId.toUserInfoElement(matrixClient: MatrixClient): UserInfoElement {
    return UserInfoElement(
        name = matrixClient.api.user.getProfile(this).getOrNullLoggingError()?.displayName ?: "",
        userId = this
    )
}