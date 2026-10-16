package de.connect2x.livekitnity.core

import de.connect2x.livekitnity.api.events.EventListenable
import kotlinx.coroutines.flow.SharedFlow

internal class SimpleEventListenable<T>(override val events: SharedFlow<T>) : EventListenable<T>
