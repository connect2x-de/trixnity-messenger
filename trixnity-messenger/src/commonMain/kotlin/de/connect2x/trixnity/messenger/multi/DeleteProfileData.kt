package de.connect2x.trixnity.messenger.multi

import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

fun interface DeleteProfileData {
    suspend operator fun invoke(profile: String, userId: UserId)
}

internal expect fun platformDeleteProfileDataModule(): Module
