package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.messenger.MatrixMessenger
import org.koin.core.module.Module

fun interface MatrixMessengerFactory {
    suspend operator fun invoke(profileId: String): MatrixMessenger
}

expect fun platformMatrixMessengerFactory(): Module
