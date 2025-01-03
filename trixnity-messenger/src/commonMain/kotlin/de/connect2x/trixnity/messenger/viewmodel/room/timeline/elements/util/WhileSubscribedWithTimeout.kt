package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlin.time.Duration.Companion.seconds

internal val whileSubscribedWithTimeout = SharingStarted.WhileSubscribed(5.seconds)
