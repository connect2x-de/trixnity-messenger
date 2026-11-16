package de.connect2x.trixnity.messenger.compose.view.room.call

import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.core.WebLiveKit
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallMediaFactory
import de.connect2x.trixnity.messenger.compose.view.room.call.livekit.LiveKitCallMedia
import org.koin.core.module.Module
import org.koin.dsl.module

@TrixnityMessengerCallApi
internal actual fun callMediaModule(): Module = module {
    single<LiveKit> { WebLiveKit() }
    single<CallMediaFactory> {
        CallMediaFactory { scope, signalling, permission -> LiveKitCallMedia(scope, get(), signalling, permission) }
    }
}
