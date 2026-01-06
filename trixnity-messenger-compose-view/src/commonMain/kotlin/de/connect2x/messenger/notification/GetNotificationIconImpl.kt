package de.connect2x.trixnity.messenger.notification

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.Module

private val log = KotlinLogging.logger { }

// TODO this should be part of the SDK (e.g. using imagemagick) instead of view level
expect fun getPlatformNotificationIconModule(): Module
