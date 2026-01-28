package de.connect2x.messenger.notification

import org.koin.core.module.Module

// TODO this should be part of the SDK (e.g. using imagemagick) instead of view level
expect fun getPlatformNotificationIconModule(): Module
