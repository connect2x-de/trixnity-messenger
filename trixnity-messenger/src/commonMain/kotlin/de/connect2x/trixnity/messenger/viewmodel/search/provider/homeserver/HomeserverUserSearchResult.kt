package de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver

import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence

data class HomeserverUserSearchResult(
    override val userId: UserId,
    override val displayName: String,
    override val initials: String,
    override val image: ByteArray?,
    val presence: StateFlow<Presence?>
) : UserSearchResult {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HomeserverUserSearchResult

        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (initials != other.initials) return false
        if (!image.contentEquals(other.image)) return false
        if (presence.value != other.presence.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + initials.hashCode()
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + presence.hashCode()
        return result
    }
}
