package de.connect2x.livekitnity.api.events

import kotlinx.coroutines.flow.SharedFlow

public interface EventListenable<out T> {
  public val events: SharedFlow<T>
}

public suspend inline fun <T> EventListenable<T>.collect(
    crossinline action: suspend (value: T) -> Unit,
): Nothing {
  events.collect { value -> action(value) }
}
