package de.connect2x.trixnity.messenger.internal.util

import de.connect2x.trixnity.messenger.util.CloseApp

internal fun interface CloseAppIfPossible {
    operator fun invoke()
}

internal fun CloseAppIfPossible(closeApp: CloseApp?): CloseAppIfPossible {
    return CloseAppIfPossibleImpl(closeApp = closeApp)
}

private class CloseAppIfPossibleImpl(private val closeApp: CloseApp?) : CloseAppIfPossible {
    override fun invoke() {
        closeApp?.invoke()
    }
}
