package de.connect2x.trixnity.messenger.search.user

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.search.provider.SearchContext

/** In order for the homeserver search to function, access to the current account is needed. */
data class UserSearchContext(val activeAccount: UserId) : SearchContext
