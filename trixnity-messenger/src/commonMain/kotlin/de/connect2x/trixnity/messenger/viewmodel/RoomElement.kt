package de.connect2x.trixnity.messenger.viewmodel

import net.folivo.trixnity.core.model.events.m.Presence

data class RoomNameElement(
    val roomName: String,
)

data class RoomHeaderElement(
    val roomName: String,
    val roomImageInitials: String,
    val roomImage: ByteArray?,
    val presence: Presence?,
)
