package de.connect2x.trixnity.messenger.util

import android.app.Activity
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger

class ActivityGetter : () -> Activity? {
    private var backingField: () -> Activity? = { null }

    override operator fun invoke(): Activity? = backingField()
    operator fun invoke(value: () -> Activity?) {
        backingField = value
    }
}

val MatrixMessenger.defaultActivityGetter: ActivityGetter
    get() = di.get<ActivityGetter>()

val MatrixMultiMessenger.defaultActivityGetter: ActivityGetter
    get() = di.get<ActivityGetter>()
