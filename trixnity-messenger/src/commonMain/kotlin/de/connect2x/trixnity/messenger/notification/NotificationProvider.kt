package de.connect2x.trixnity.messenger.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import de.connect2x.trixnity.core.model.UserId

interface NotificationProvider {
    val id: String
    val displayName: String

    val canBeEnabled: Boolean
    val isEnabled: StateFlow<Boolean>
    fun isEnabled(userId: UserId): Flow<Boolean>
    suspend fun enable(userId: UserId)
    suspend fun disable(userId: UserId)
}
