package de.connect2x.livekitnity.core.network

import de.connect2x.livekitnity.api.network.ReconnectPolicy
import de.connect2x.livekitnity.core.platform.Converter
import io.livekit.android.room.network.ReconnectContext
import kotlin.time.Duration

internal class CommonToAndroidReconnectPolicy(val delegate: ReconnectPolicy) :
    io.livekit.android.room.network.ReconnectPolicy {
  override fun getNextRetryDelay(context: ReconnectContext): Duration? {
    return delegate.getNextRetryDelay(Converter.toCommon(context))
  }
}
