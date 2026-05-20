package de.connect2x.trixnity.messenger.util

import androidx.activity.ComponentActivity
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import kotlin.concurrent.atomics.AtomicReference

class ActivityGetter(initialValue: (() -> ComponentActivity)?) {
    private val value: AtomicReference<(() -> ComponentActivity)?> = AtomicReference(initialValue)

    operator fun invoke(): ComponentActivity =
        checkNotNull(value.load()) {
            "ActivityGetter has not been set. Use MatrixMessenger.defaultActivityGetter or MatrixMultiMessenger.defaultActivityGetter to set it."
        }()

    operator fun invoke(value: () -> ComponentActivity) {
        this.value.store(value)
    }
}

val MatrixMessenger.defaultActivityGetter: ActivityGetter
    get() = di.get<ActivityGetter>()

val MatrixMultiMessenger.defaultActivityGetter: ActivityGetter
    get() = di.get<ActivityGetter>()
