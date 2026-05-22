package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.core.model.UserId
import org.koin.core.module.Module

fun interface DeleteAccountData {
    suspend operator fun invoke(userId: UserId)
}

internal expect fun platformDeleteAccountDataModule(): Module
