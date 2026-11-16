package de.connect2x.trixnity.messenger.compose.view.room.call

import de.connect2x.livekitnity.api.LiveKit
import de.connect2x.livekitnity.core.AndroidLiveKit
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallMediaFactory
import de.connect2x.trixnity.messenger.compose.view.room.call.livekit.LiveKitCallMedia
import de.connect2x.trixnity.messenger.util.ContextGetter
import org.koin.core.module.Module
import org.koin.dsl.module

@TrixnityMessengerCallApi
internal actual fun callMediaModule(): Module = module {
    single<LiveKit> { AndroidLiveKit(get<ContextGetter>().invoke().applicationContext) }
    single<CallMediaFactory> {
        CallMediaFactory { scope, signalling, permission -> LiveKitCallMedia(scope, get(), signalling, permission) }
    }
}
