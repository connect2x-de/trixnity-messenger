package de.connect2x.livekitnity.api.network

import kotlin.time.Duration

public interface ReconnectPolicy {
  public fun getNextRetryDelay(context: ReconnectContext): Duration?
}
