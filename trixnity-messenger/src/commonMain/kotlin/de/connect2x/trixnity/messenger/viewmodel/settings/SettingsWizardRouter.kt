package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }


@OptIn(ExperimentalCoroutinesApi::class)
class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
) : ViewModelContext by viewModelContext {


    val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "SettingsWizardRouter,",
        childFactory = ::createChild
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper = when (config) {
        is Config.None -> {
            Wrapper.None
        }

        is Config.ShowWizard -> {
            log.debug { "Building Settings Wizard" }
            Wrapper.ShowWizard(
                get<AccountBootstrappingViewModelFactory>().create(
                    viewModelContext.childContext(
                        componentContext, userId = config.userId
                    ), ::onWizardClose
                )
            )
        }
    }


    private val settings = get<MatrixMessengerSettingsHolder>()

    fun onWizardClose(userId: UserId) {
        log.debug { "Closing Settings Wizard for $userId" }
        coroutineScope.launch {
            settings.update<MatrixMessengerAccountSettingsBase>(userId) {
                it.copy(accountBootstrappingFinished = true)
            }
            navigation.popWhileSuspending { it is Config.ShowWizard && it.userId == userId }
        }
    }

    fun startWizard(userId: UserId) {
        log.debug { "Starting Wizard" }
        navigation.launchPush(coroutineScope, Config.ShowWizard(userId))
    }


    sealed class Wrapper {
        data class ShowWizard(val viewModel: AccountBootstrappingViewModel) : Wrapper()
        data object None : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data class ShowWizard(val userId: UserId) : Config()

        @Serializable
        data object None : Config()
    }
}

