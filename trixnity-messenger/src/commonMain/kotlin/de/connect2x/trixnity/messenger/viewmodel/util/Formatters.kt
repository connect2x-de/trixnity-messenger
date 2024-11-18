package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.clientserverapi.model.media.FileTransferProgress
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration

fun Float.format(numOfDec: Int): String {
    val integerDigits = this.toInt()
    val floatDigits = ((this - integerDigits) * 10f.pow(numOfDec)).roundToInt()
    return "${integerDigits},${floatDigits}"
}

fun formatProgress(fileTransferProgress: FileTransferProgress?): String {
    return fileTransferProgress?.let {
        val total = it.total
        if (total != null) "${formatSize(it.transferred, total)} / ${formatSize(total)}"
        else ""
    } ?: ""
}

fun formatSize(sizeInByte: Long, maxSizeInByte: Long = sizeInByte): String {
    return when {
        maxSizeInByte / 1_000_000_000 >= 1 -> {
            "${(sizeInByte / 1_000_000_000f).format(1)}GB"
        }

        maxSizeInByte / 1_000_000 >= 1 -> { // MB
            "${(sizeInByte / 1_000_000f).format(1)}MB"
        }

        else -> {
            "${(sizeInByte / 1_000f).format(1)}kB"
        }
    }
}

// TODO everything down below: i18n

fun formatTimestamp(timestamp: Instant, clock: Clock, timeZone: TimeZone): String {
    val now = clock.now().toLocalDateTime(timeZone)
    val localDateTime = timestamp.toLocalDateTime(timeZone)
    return if (localDateTime.dayOfYear == now.dayOfYear && localDateTime.year == now.year) {
        formatTime(localDateTime)
    } else {
        formatDateShortYear(localDateTime)
    }
}

fun formatDate(localDateTime: LocalDateTime) =
    "${
        localDateTime.dayOfMonth.toString().padStart(2, '0')
    }.${
        localDateTime.monthNumber.toString().padStart(2, '0')
    }.${
        localDateTime.year
    }"

fun formatDateShortYear(localDateTime: LocalDateTime) =
    "${
        localDateTime.dayOfMonth.toString().padStart(2, '0')
    }.${
        localDateTime.monthNumber.toString().padStart(2, '0')
    }.${
        localDateTime.year.toString().substring(2, 4)
    }"

fun formatTime(localDateTime: LocalDateTime): String =
    "${
        localDateTime.hour.toString().padStart(2, '0')
    }:${
        localDateTime.minute.toString().padStart(2, '0')
    }"

fun formatDuration(duration: Duration): String =
    duration.toComponents { hours, minutes, seconds, _ ->
        "${
            if (hours > 0) {
                "${hours}:${minutes.toString().padStart(2, '0')}:"
            } else minutes
        }:" +
                seconds.toString().padStart(2, '0')
    }

