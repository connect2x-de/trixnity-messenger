package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.events.RoomEventContent
import kotlin.reflect.KClass

interface TimelineElementRules {
    val areVisible: Set<KClass<out RoomEventContent>>
    val canHaveUnreadMarker: Set<KClass<out RoomEventContent>>

    operator fun plus(other: TimelineElementRules?): TimelineElementRules {
        if (other == null) return this
        val areVisible = this.areVisible + other.areVisible
        val canHaveUnreadMarker = this.canHaveUnreadMarker + other.canHaveUnreadMarker
        return object : TimelineElementRules {
            override val areVisible: Set<KClass<out RoomEventContent>> = areVisible
            override val canHaveUnreadMarker: Set<KClass<out RoomEventContent>> = canHaveUnreadMarker
        }
    }

    operator fun minus(other: TimelineElementRules?): TimelineElementRules {
        if (other == null) return this
        val areVisible = this.areVisible - other.areVisible
        val canHaveUnreadMarker = this.canHaveUnreadMarker - other.canHaveUnreadMarker
        return object : TimelineElementRules {
            override val areVisible: Set<KClass<out RoomEventContent>> = areVisible
            override val canHaveUnreadMarker: Set<KClass<out RoomEventContent>> = canHaveUnreadMarker
        }
    }
}