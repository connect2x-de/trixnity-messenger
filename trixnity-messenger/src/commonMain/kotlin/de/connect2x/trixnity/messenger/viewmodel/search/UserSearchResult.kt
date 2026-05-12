package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.core.model.UserId

interface UserSearchResult {
    val id: String // unique ID; if userId is unique, it can be used here
    val userId: UserId
    val displayName: String?
    val initials: String
    val image: ByteArray?
    val sortingFields: List<Pair<String, String>>
}
