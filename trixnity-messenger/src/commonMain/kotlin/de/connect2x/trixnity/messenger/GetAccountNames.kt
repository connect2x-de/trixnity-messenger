package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings

/**
 * Wrapper for [Messenger0Settings.accountNames] to be able to replace or mock.
 */
interface GetAccountNames {
    suspend operator fun invoke(): List<String>
}

class GetAccountNamesImpl() : GetAccountNames {
    override suspend operator fun invoke(): List<String> {
        return getAccountNames()
    }
}