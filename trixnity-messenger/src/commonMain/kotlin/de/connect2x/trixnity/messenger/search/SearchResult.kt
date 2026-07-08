package de.connect2x.trixnity.messenger.search

import de.connect2x.trixnity.messenger.search.user.UserSearchResult

/** Any result of a search, e.g. a [UserSearchResult]. */
interface SearchResult {
    val id: String
}
