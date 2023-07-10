package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.messenger.MessengerConfig
import de.connect2x.trixnity.messenger.viewmodel.util.timezone
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FileNameComputations(private val clock: Clock) {

    fun getOrCreateFileName(body: String, mimeType: String?, defaultMimeType: ContentType): String {
        return body.ifBlank {
            createFileName(mimeType?.let { ContentType.parse(it) } ?: defaultMimeType)
        }
    }

    private fun createFileName(mimeType: ContentType): String {
        val appName = MessengerConfig.instance.appName
        val localDateTime = clock.now().toLocalDateTime(TimeZone.of(timezone()))
        val month = localDateTime.monthNumber.toString().padStart(2, '0')
        val dayOfMonth = localDateTime.dayOfMonth.toString().padStart(2, '0')
        val date = "${localDateTime.year}-${month}-${dayOfMonth}"
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        val time = "${hour}-${minute}"
        val extension = mimeType.contentSubtype
        return "$appName $date $time.$extension"
    }
}