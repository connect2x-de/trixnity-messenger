package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.cleanAccountName

internal fun getAppFolder() = getContext().filesDir.resolve(MessengerConfig.instance.appName)
internal fun getAccountPath(accountName: String) = getAppFolder().resolve(accountName.cleanAccountName())