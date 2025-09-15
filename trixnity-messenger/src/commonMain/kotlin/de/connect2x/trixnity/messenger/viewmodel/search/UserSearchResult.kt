package de.connect2x.trixnity.messenger.viewmodel.search

import net.folivo.trixnity.core.model.UserId

interface UserSearchResult {
    val userId: UserId
    val displayName: String?
    val initials: String
    val image: ByteArray?
}
