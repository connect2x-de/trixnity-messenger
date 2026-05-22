package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed

internal val whileSubscribedWithTimeout = SharingStarted.WhileSubscribed(5.seconds)
