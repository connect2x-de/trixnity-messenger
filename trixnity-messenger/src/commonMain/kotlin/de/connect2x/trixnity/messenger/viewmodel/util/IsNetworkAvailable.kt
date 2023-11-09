package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.isNetworkAvailable

interface IsNetworkAvailable {
    operator fun invoke(): Boolean {
        return isNetworkAvailable()
    }

    companion object : IsNetworkAvailable
}