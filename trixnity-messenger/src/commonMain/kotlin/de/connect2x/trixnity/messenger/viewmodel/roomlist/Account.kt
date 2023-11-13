package de.connect2x.trixnity.messenger.viewmodel.roomlist

import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.core.model.UserId

data class Account(
    val accountName: String, // primary key
    val userId: UserId,
    val displayName: StateFlow<String>,
    val initials: StateFlow<String>,
    val avatar: StateFlow<ByteArray?>
)