package de.connect2x.trixnity.messenger.util

import org.koin.core.module.Module

fun interface SendLogToDevs {
    suspend operator fun invoke(emailAddress: String, subject: String)
}

expect fun platformSendLogToDevsModule(): Module
