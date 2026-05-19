package de.connect2x.trixnity.messenger.viewmodel.search

import de.connect2x.trixnity.core.model.UserId
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a search result for a user.
 */
interface UserSearchResult {
    /**
     * unique ID; if [userId] is unique, it can be used here
     */
    val id: String

    /**
     * the [UserId] of the user
     */
    val userId: UserId

    /**
     * the display name of the user (in Matrix `display_name`)
     */
    val displayName: String?

    /**
     * the initials of the user, shown if no [image] is available
     */
    val initials: String

    /**
     * an image of the user, shown if available
     */
    val image: StateFlow<ByteArray?>
}
