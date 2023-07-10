package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.events.RedactedMessageEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationStep
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent.MegolmEncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent.OlmEncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import kotlin.reflect.KClass

object DefaultTimelineElementRules : TimelineElementRules {
    override val areVisible: Set<KClass<out RoomEventContent>> = setOf(
        // encrypted
        MegolmEncryptedEventContent::class,
        OlmEncryptedEventContent::class,
        // messages
        RoomMessageEventContent::class,
        RedactedMessageEventContent::class,
        // verification
        VerificationStep::class,
        // status events
        CreateEventContent::class,
        MemberEventContent::class,
        NameEventContent::class,
    )
    override val canHaveUnreadMarker: Set<KClass<out RoomEventContent>> = setOf(
        // encrypted
        MegolmEncryptedEventContent::class,
        OlmEncryptedEventContent::class,
        // messages
        RoomMessageEventContent::class,
        RedactedMessageEventContent::class,
    )
}