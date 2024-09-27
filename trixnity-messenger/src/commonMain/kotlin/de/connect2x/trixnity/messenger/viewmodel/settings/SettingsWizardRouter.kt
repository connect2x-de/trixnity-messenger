package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushToFront
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.launchPopWhile
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper.WizardExplanation
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.datastructure.linkedHashMapListOf
import korlibs.io.async.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
) : ViewModelContext by viewModelContext {
    private val navigation = StackNavigation<Config>()

    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = null,
        initialConfiguration = Config.None,
        key = "settingsWizard",
        childFactory = ::createChild
    )

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val activeAccount = messengerSettings.map { it.base.selectedAccount }.stateIn(
        coroutineScope, SharingStarted.WhileSubscribed(), null
    )

    private val wizardSteps = listOf<Config>(
        Config.WizardExplanation(activeAccount.value ?: UserId("Error")),
        Config.NotificationSettings(activeAccount.value ?: UserId("Error"))
    )

    private val currentWizardStep = MutableStateFlow(0)


    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper =
        when (config) {
            is Config.None -> Wrapper.None
            is Config.WizardExplanation -> WizardExplanation(::switchToNextWizardStep)
            is Config.NotificationSettings -> Wrapper.NotificationSettings(::switchToNextWizardStep)
        }

    fun showCurrentWizardStep() {
            if (currentWizardStep.value < wizardSteps.size) {
                log.debug { "Showing Wizard step ${currentWizardStep.value}" }
                navigation.launchPush(coroutineScope, wizardSteps[currentWizardStep.value])
            } else {
                log.debug { "Closing Wizard" }
                closeWizard()
        }
    }

    fun switchToNextWizardStep() {
        currentWizardStep.value++
        showCurrentWizardStep()
    }

    fun closeWizard() {
            navigation.launchPopWhile(coroutineScope){ it != Config.None }
    }

    sealed class Config {
        data class NotificationSettings(val userId: UserId) : Config()
        data class WizardExplanation(val userId: UserId) : Config()
        data object None : Config()
    }

    sealed class Wrapper {
        class NotificationSettings(val onSwitchToNext: () -> Unit) : Wrapper()
        class WizardExplanation(val onSwitchToNext: () -> Unit) : Wrapper()
        data object None : Wrapper()
    }
}
