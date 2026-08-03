package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface AccountSetupFinishedLogic {
    fun accountSetupFinished(): Flow<Map<UserId, Boolean>>
}

internal fun AccountSetupFinishedLogic(
    matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder
): AccountSetupFinishedLogic {
    return AccountSetupFinishedLogicImpl(matrixMessengerSettingsHolder = matrixMessengerSettingsHolder)
}

private class AccountSetupFinishedLogicImpl(private val matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder) :
    AccountSetupFinishedLogic {

    override fun accountSetupFinished(): Flow<Map<UserId, Boolean>> {
        return accountSetupFinished(matrixMessengerSettingsHolder)
    }
}

private fun accountSetupFinished(
    matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder
): Flow<Map<UserId, Boolean>> {
    return matrixMessengerSettingsHolder.map { settings ->
        settings.base.accounts.mapValues { (_, accountSettings) -> accountSettings.base.accountSetupFinished }
    }
}
