package de.connect2x.messenger.android

import android.app.Activity

interface MatrixMessengerStartup {
    suspend operator fun invoke(activity: Activity)
}
