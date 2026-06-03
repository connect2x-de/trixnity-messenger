package de.connect2x.trixnity.messenger.util

import kotlin.collections.component1
import kotlin.collections.component2
import web.events.Event
import web.events.EventHandler
import web.events.EventTarget
import web.events.EventType
import web.events.addEventHandler

fun handleFirst(eventTarget: EventTarget, handlers: Map<EventType<Event>, () -> Unit>) {
    val handlerRemovers = mutableListOf<() -> Unit>()
    handlers.forEach { (eventType, handler) ->
        val handlerRemover =
            eventTarget.addEventHandler(
                type = eventType,
                handler =
                    EventHandler {
                        handlerRemovers.forEach { it() }
                        handler()
                    },
            )
        handlerRemovers.add(handlerRemover)
    }
}
