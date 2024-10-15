package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.datetime.TimeZone

fun timezone() = "CET" // TODO configurable!

fun timezoneOf(timezoneId: String): TimeZone = try {
    TimeZone.of(timezoneId)
} catch (exc: Exception) {
    TimeZone.currentSystemDefault()
}

fun avatarSize() = 36
