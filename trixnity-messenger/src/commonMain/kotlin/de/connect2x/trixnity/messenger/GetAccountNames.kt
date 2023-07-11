package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings

/**
 * Wrapper for [Messenger0Settings.accountNames] to be able to replace or mock.
 */
interface GetAccountNames {
    operator fun invoke(): List<String>
}

class GetAccountNamesImpl(private val messengerSettings: MessengerSettings) : GetAccountNames {
    override operator fun invoke(): List<String> {
        return messengerSettings.accountNames
    }
}