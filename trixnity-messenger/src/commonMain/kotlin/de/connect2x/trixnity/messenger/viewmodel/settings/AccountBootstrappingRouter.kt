package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.launchPopWhile
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }


@OptIn(ExperimentalCoroutinesApi::class)
class AccountBootstrappingRouter(
    private val viewModelContext: ViewModelContext,
    private val onStartCrossDeviceVerification : (userId: UserId) -> Unit,
    private val onCloseCrossDeviceVerification : () -> Unit,
    private val onStartVerificationBootstrap: (userId: UserId) -> Unit
) : ViewModelContext by viewModelContext {


    val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "AccountBootstrappingRouter",
        childFactory = ::createChild
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper = when (config) {
        is Config.None -> {
            Wrapper.None
        }

        is Config.ShowBootstrap -> {
            log.debug { "Building Account Bootstrapping for ${config.userId}" }
            Wrapper.ShowBootstrap(
                get<AccountBootstrappingViewModelFactory>().create(
                    viewModelContext.childContext(
                        componentContext, userId = config.userId
                    ), ::onBootstrapClose, onStartVerificationBootstrap, onStartCrossDeviceVerification, onCloseCrossDeviceVerification
                )
            )
        }
    }


    private val settings = get<MatrixMessengerSettingsHolder>()

    fun onBootstrapClose(userId: UserId) {
        log.debug { "Closing AccountBootstrapping for $userId" }
        coroutineScope.launch {
            settings.update<MatrixMessengerAccountSettingsBase>(userId) {
                it.copy(deviceBootstrappingFinished = true)
            }
            navigation.popWhileSuspending { it is Config.ShowBootstrap && it.userId == userId }
        }
    }


    fun startBootstrap(userId: UserId) {
        log.debug { "Starting Account Bootstrapping for $userId" }
        navigation.launchPush(coroutineScope, Config.ShowBootstrap(userId))
    }


    sealed class Wrapper {
        data class ShowBootstrap(val viewModel: AccountBootstrappingViewModel) : Wrapper()
        data object None : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data class ShowBootstrap(val userId: UserId) : Config()
        @Serializable
        data object None : Config()
    }
}

