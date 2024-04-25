package de.connect2x.trixnity.messenger.multi

import org.koin.core.module.Module

fun interface DeleteProfileData {
    suspend operator fun invoke(profile: String)
}

internal expect fun platformDeleteProfileDataModule(): Module
