package de.connect2x.trixnity.messenger.util

import android.content.Context
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger

class ContextGetter(initialValue: Context?) : () -> Context? {
    private var backingField: (() -> Context)? = initialValue?.let { { it } }

    override operator fun invoke(): Context =
        checkNotNull(backingField) { "ContextGetter has not been set. Use MatrixMessenger.defaultContextGetter or MatrixMultiMessenger.defaultContextGetter to set it." }()

    operator fun invoke(value: () -> Context) {
        backingField = value
    }
}

val MatrixMessenger.defaultContextGetter: ContextGetter
    get() = di.get<ContextGetter>()

val MatrixMultiMessenger.defaultContextGetter: ContextGetter
    get() = di.get<ContextGetter>()
