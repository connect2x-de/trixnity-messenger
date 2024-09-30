package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper.WizardExplanation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
) : ViewModelContext by viewModelContext {

    private val activeAccount = MutableStateFlow<UserId?>(null)

    init {
        val messengerSettings = get<MatrixMessengerSettingsHolder>()
        activeAccount.value = messengerSettings.value.base.selectedAccount
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val privacySettings =
        viewModelContext.get<PrivacySettingsAllAccountsViewModelFactory>()
            .create(
                viewModelContext.childContext(key = "SettingsWizard-Privacy"),
                {},
                {}).privacySettings.transformLatest { value ->
                if (value.find { it.account == activeAccount.value } != null) emit(
                    value
                )
            }.mapLatest { it.first { it.account == activeAccount.value } }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val wizardSteps = listOf<Wrapper>(
        WizardExplanation(activeAccount.value ?: UserId("Error")),
        Wrapper.PrivacySettings(privacySettings),
        Wrapper.NotificationSettings(),
        Wrapper.WizardConfirm
    )

    fun getWizardSteps(): List<Wrapper> {
        return wizardSteps
    }


    sealed class Wrapper {
        class NotificationSettings() : Wrapper()
        class PrivacySettings(val viewModel: StateFlow<PrivacySettingsSingleAccountViewModel?>) : Wrapper()
        class WizardExplanation(val userId: UserId) : Wrapper()
        data object WizardConfirm : Wrapper()
        data object None : Wrapper()
    }
}
