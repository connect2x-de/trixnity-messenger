package de.connect2x.trixnity.messenger.internal.util

import de.connect2x.trixnity.messenger.util.MinimizeApp

internal fun interface MinimizeAppIfPossible {
    operator fun invoke()
}

internal fun MinimizeAppIfPossible(minimizeApp: MinimizeApp?): MinimizeAppIfPossible {
    return MinimizeAppIfPossibleImpl(minimizeApp = minimizeApp)
}

private class MinimizeAppIfPossibleImpl(private val minimizeApp: MinimizeApp?) : MinimizeAppIfPossible {
    override fun invoke() {
        minimizeApp?.invoke()
    }
}
