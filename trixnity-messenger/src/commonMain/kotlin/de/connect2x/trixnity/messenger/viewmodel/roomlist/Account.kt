package de.connect2x.trixnity.messenger.viewmodel.roomlist

import kotlinx.coroutines.flow.StateFlow

data class Account(
    val accountName: String, // primary key
    val userId: UserId,
    val displayName: StateFlow<String>,
    val initials: StateFlow<String>,
    val avatar: StateFlow<ByteArray?>
)