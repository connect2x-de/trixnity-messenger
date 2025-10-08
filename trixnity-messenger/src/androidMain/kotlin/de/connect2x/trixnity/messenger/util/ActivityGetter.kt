package de.connect2x.trixnity.messenger.util

import androidx.activity.ComponentActivity
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger

class ActivityGetter : () -> ComponentActivity? {
    private var backingField: (() -> ComponentActivity)? = null

    override operator fun invoke(): ComponentActivity =
        checkNotNull(backingField) { "ActivityGetter has not been set. Use MatrixMessenger.defaultActivityGetter or MatrixMultiMessenger.defaultActivityGetter to set it." }()

    operator fun invoke(value: () -> ComponentActivity) {
        backingField = value
    }
}

val MatrixMessenger.defaultActivityGetter: ActivityGetter
    get() = di.get<ActivityGetter>()

val MatrixMultiMessenger.defaultActivityGetter: ActivityGetter
    get() = di.get<ActivityGetter>()
