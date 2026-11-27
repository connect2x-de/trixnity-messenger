package de.connect2x.trixnity.messenger.compose.view.room.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import org.koin.core.module.Module

@TrixnityMessengerCallApi internal expect fun callMediaModule(): Module
