package de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.Presence
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlinx.coroutines.flow.StateFlow

data class HomeserverUserSearchResult(
    override val userId: UserId,
    override val displayName: String,
    override val initials: String,
    override val image: StateFlow<ByteArray?>,
    val presence: StateFlow<Presence?>
) : UserSearchResult {

    override val id: String = userId.full
    override val sortingFields: List<Pair<String, String>> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HomeserverUserSearchResult

        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (initials != other.initials) return false
        if (!image.value.contentEquals(other.image.value)) return false
        if (presence.value != other.presence.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + initials.hashCode()
        result = 31 * result + (image.value?.contentHashCode() ?: 0)
        result = 31 * result + presence.hashCode()
        return result
    }

    override fun toString(): String {
        return "HomeserverUserSearchResult(userId=$userId, displayName='$displayName', initials='$initials')"
    }
}
