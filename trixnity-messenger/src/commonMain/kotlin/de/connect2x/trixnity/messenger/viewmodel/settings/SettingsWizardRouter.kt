package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper.WizardExplanation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
) : ViewModelContext by viewModelContext {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val activeAccount = messengerSettings.map { it.base.selectedAccount }.stateIn(
        coroutineScope, SharingStarted.WhileSubscribed(), null
    )

    private val wizardSteps = listOf<Config>(
        Config.WizardExplanation(activeAccount.value ?: UserId("Error")),
        Config.NotificationSettings(activeAccount.value ?: UserId("Error")),
        Config.WizardConfirm
    )

    fun getWizardSteps(): List<Wrapper> {
        val list = mutableListOf<Wrapper>()
        for (step in wizardSteps) {
            when (step) {
                is Config.WizardExplanation -> list.add(
                    WizardExplanation(activeAccount.value?: UserId("Unknown"))
                )
                is Config.NotificationSettings -> list.add(
                    Wrapper.NotificationSettings()
                )
                is Config.WizardConfirm -> list.add(
                    Wrapper.WizardConfirm())
                    else -> {}
            }
        }
        return list
    }

    sealed class Config {
        data class NotificationSettings(val userId: UserId) : Config()
        data class WizardExplanation(val userId: UserId) : Config()
        data object WizardConfirm : Config()
        data object None : Config()
    }

    sealed class Wrapper {
        class NotificationSettings() : Wrapper()
        class WizardExplanation(userId: UserId) : Wrapper()
        class WizardConfirm() : Wrapper()
        data object None : Wrapper()
    }
}
