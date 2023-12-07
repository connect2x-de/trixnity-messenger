package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.events.RedactedEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStep
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent.MegolmEncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import kotlin.reflect.KClass

object DefaultTimelineElementRules : TimelineElementRules {
    override val areVisible: Set<KClass<out RoomEventContent>> = setOf(
        // encrypted
        MegolmEncryptedMessageEventContent::class,
        // messages
        RoomMessageEventContent::class,
        RedactedEventContent::class,
        // verification
        VerificationStep::class,
        // status events
        CreateEventContent::class,
        MemberEventContent::class,
        NameEventContent::class,
    )
    override val canHaveUnreadMarker: Set<KClass<out RoomEventContent>> = setOf(
        // encrypted
        MegolmEncryptedMessageEventContent::class,
        // messages
        RoomMessageEventContent::class,
        RedactedEventContent::class,
    )
}