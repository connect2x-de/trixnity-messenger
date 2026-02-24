package de.connect2x.trixnity.messenger.util

import android.content.Context
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import kotlin.concurrent.atomics.AtomicReference

class ContextGetter(initialValue: (() -> Context)?) {
    private val value: AtomicReference<(() -> Context)?> = AtomicReference(initialValue)

    operator fun invoke(): Context =
        checkNotNull(value.load()) { "ContextGetter has not been set. Use MatrixMessenger.defaultContextGetter or MatrixMultiMessenger.defaultContextGetter to set it." }()

    operator fun invoke(value: () -> Context) {
        this.value.store(value)
    }
}

val MatrixMessenger.defaultContextGetter: ContextGetter
    get() = di.get<ContextGetter>()

val MatrixMultiMessenger.defaultContextGetter: ContextGetter
    get() = di.get<ContextGetter>()
