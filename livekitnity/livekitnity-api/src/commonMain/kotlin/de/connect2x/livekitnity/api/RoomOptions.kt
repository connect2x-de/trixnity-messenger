package de.connect2x.livekitnity.api

import de.connect2x.livekitnity.api.e2ee.E2EEOptions
import de.connect2x.livekitnity.api.network.ReconnectPolicy

public data class RoomOptions(
    val adaptiveStream: Boolean = false,
    val dynacast: Boolean = false,
    val e2eeOptions: E2EEOptions? = null,
    val reconnectPolicy: ReconnectPolicy? = null,
)
