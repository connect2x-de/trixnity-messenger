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
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }


@OptIn(ExperimentalCoroutinesApi::class)
class AccountSetupRouter(
    private val viewModelContext: ViewModelContext,
    private val onCloseCrossDeviceVerification : () -> Unit,
    private val onStartCrossSigningBootstrap: (userId: UserId) -> Unit,
    private val onStartVerification: (UserId, Boolean) -> Unit
) : ViewModelContext by viewModelContext {


    val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "AccountSetupRouter",
        childFactory = ::createChild
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper = when (config) {
        is Config.None -> {
            Wrapper.None
        }

        is Config.ShowAccountSetup -> {
            log.debug { "Building Account setup for ${config.userId}" }
            Wrapper.ShowAccountSetup(
                get<AccountSetupViewModelFactory>().create(
                    viewModelContext.childContext(
                        componentContext, userId = config.userId
                    ), ::onSetupClose, onStartCrossSigningBootstrap, onCloseCrossDeviceVerification, onStartVerification
                )
            )
        }
    }


    private val settings = get<MatrixMessengerSettingsHolder>()

    fun onSetupClose(userId: UserId) {
        log.debug { "Closing Account setup for $userId" }
        coroutineScope.launch {
            settings.update<MatrixMessengerAccountSettingsBase>(userId) {
                it.copy(accountSetupFinished = true)
            }
            navigation.popWhileSuspending { it is Config.ShowAccountSetup && it.userId == userId }
        }
    }


    fun startSetup(userId: UserId) {
        log.debug { "Starting Account setup for $userId" }
        navigation.launchPush(coroutineScope, Config.ShowAccountSetup(userId))
    }


    sealed class Wrapper {
        data class ShowAccountSetup(val viewModel: AccountSetupViewModel) : Wrapper()
        data object None : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data class ShowAccountSetup(val userId: UserId) : Config()
        @Serializable
        data object None : Config()
    }
}

