package de.connect2x.trixnity.messenger.viewmodel.room.archive

import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

private val log = KotlinLogging.logger { }
internal fun LocalDateTime.formatLocalDateTime(): String =
    "${formatDate(this)},${formatTime(this)}"

internal fun localDateTimeOf(event: ClientEvent.RoomEvent<*>): LocalDateTime {
    val timestamp = event.originTimestamp
    requireNotNull(timestamp) // should not happen as only RoomEvents and StateEvents are possible
    return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.of(timezone()))
}

fun transformMessage(timelineEvent: TimelineEvent): String? {
    val sender = timelineEvent.sender.full
    val event = timelineEvent.event
    val receivedDateTime = localDateTimeOf(event).formatLocalDateTime()
    val formattedResult = timelineEvent.content?.fold(onSuccess = {
        if (it is RoomMessageEventContent.TextBased) {
            "$receivedDateTime, $sender, ${it.body}"
        } else
            null
    }, onFailure = {
        log.error(it) { "failed to archive room" }
        null
    })

    return formattedResult
}
