package de.connect2x.trixnity.messenger.internal.verification

import de.connect2x.trixnity.core.model.UserId
import kotlinx.serialization.Serializable

@Serializable
internal data class ChangeVerificationCompleteStatus(val userId: UserId, val newVerificationCompleteStatus: Boolean)
