package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.datetime.LocalDateTime

fun LocalDateTime.isDifferentDay(other: LocalDateTime): Boolean {
    val differentDayInTheYear = this.year == other.year && this.dayOfYear != other.dayOfYear
    val differentYear = this.year != other.year
    return differentDayInTheYear || differentYear
}
