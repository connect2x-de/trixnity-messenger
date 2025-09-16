package de.connect2x.trixnity.messenger.viewmodel.search

import net.folivo.trixnity.core.model.UserId

interface UserSearchResult {
    val id: String // unique ID, if userId is unique, you can return it
    val userId: UserId
    val displayName: String?
    val initials: String
    val image: ByteArray?
}
