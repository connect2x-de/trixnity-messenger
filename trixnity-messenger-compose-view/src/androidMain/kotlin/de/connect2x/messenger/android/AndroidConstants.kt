package de.connect2x.messenger.android

fun getDefaultChannelId(appName: String): String = "de.connect2x.messenger.${appName.lowercase().replace(' ', '_')}"
const val INITIAL_SYNC_CHANNEL_ID: String = "de.connect2x.messenger.initialsync"

const val PREFS = "de.connect2x.messenger.prefs"
const val PREF_BACKGROUND_SYNC_SHOULD_BE_RUNNING = "backgroundSyncShouldBeRunning"
