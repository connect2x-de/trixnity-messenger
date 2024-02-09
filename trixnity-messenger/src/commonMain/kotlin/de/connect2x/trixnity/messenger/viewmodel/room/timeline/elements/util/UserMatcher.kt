package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import korlibs.io.util.getOrNullLoggingError
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId

// https://spec.matrix.org/v1.8/appendices/#user-identifiers
private const val baseLocalpartRegex = """[a-z0-9.-_=/+]+"""

// https://spec.matrix.org/latest/appendices/#server-name
private const val basePortRegex = """:\d{4}"""
private const val baseDomainRegex = """(?:[\w-]+\.)?[\w-]+\.[\w-]+"""
private const val baseIPV4Regex = """\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""
private const val baseIPV6Regex = """\[[0-9a-fA-F:]+\]"""
private const val baseServernameRegex = """(?:$baseDomainRegex)|(?:$baseIPV4Regex)|(?:$baseIPV6Regex)(?:$basePortRegex)?"""

// https://spec.matrix.org/v1.8/appendices/#user-identifiers
private const val baseUserIdRegex = """@($baseLocalpartRegex):($baseServernameRegex)"""

// https://spec.matrix.org/v1.8/appendices/#matrix-uri-scheme
private const val baseUserUriRegex =
    """matrix:u/($baseLocalpartRegex):($baseServernameRegex)(?:\?(?:action=chat)|(?:via=$baseServernameRegex)(?:&(?:action=chat)|(?:via=$baseServernameRegex))?)"""

// https://spec.matrix.org/v1.8/appendices/#matrixto-navigation
private const val baseUserLinkRegex = """https?://matrix\.to/#/@($baseLocalpartRegex):($baseServernameRegex)"""
private const val baseUserHtmlAnchorRegex = """<a href="$baseUserLinkRegex">.*</a>"""


private val userRegex by lazy {
    """(?:$baseUserIdRegex)|(?:$baseUserUriRegex)|(?:$baseUserLinkRegex)|(?:$baseUserHtmlAnchorRegex)""".toRegex()
}

fun matchUsers(message: String): Map<String, UserId> {
    val matches = userRegex.findAll(message)
    return matches.associate {
        val matched = it.groupValues[0]
        val localpart = it.groupValues[1] + it.groupValues[3] + it.groupValues[5] + it.groupValues[7]
        val domain = it.groupValues[2] + it.groupValues[4] + it.groupValues[6] + it.groupValues[8]

        matched to UserId(localpart, domain)
    }
}

fun UserId.toUserInfoElement(matrixClient: MatrixClient): UserInfoElement {
    return UserInfoElement(
        name = "",
        userId = this
    )
}