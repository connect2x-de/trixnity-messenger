package de.connect2x.messenger.android

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit

var Context.backgroundSyncShouldBeRunning: Boolean
    get() {
        return this
            .getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_BACKGROUND_SYNC_SHOULD_BE_RUNNING, false)
    }
    set(value) {
        this.getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit(commit = true) {
                putBoolean(PREF_BACKGROUND_SYNC_SHOULD_BE_RUNNING, value)
            }
    }
