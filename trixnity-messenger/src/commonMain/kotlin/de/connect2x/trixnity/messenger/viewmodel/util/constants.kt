package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.datetime.TimeZone

fun currentTimezone(): TimeZone = TimeZone.currentSystemDefault()

fun avatarSize() = 36
