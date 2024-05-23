package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

class AvatarCutterRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "avatarCutter",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper =
        when (config) {
            is Config.None -> Wrapper.None
            is Config.AvatarCutter -> Wrapper.AvatarCutter(
                viewModelContext.get<AvatarCutterViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.userId),
                    file = config.file,
                    onClose = ::onClose,
                )
            )
        }

    suspend fun show(userId: UserId, file: FileDescriptor) {
        navigation.pushSuspending(Config.AvatarCutter(userId, file))
    }

    suspend fun close() {
        navigation.popSuspending()
    }

    private fun onClose() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }


    @Serializable
    sealed class Config {
        @Serializable
        data class AvatarCutter(
            val userId: UserId,
            val file: FileDescriptor
        ) : Config()

        @Serializable
        data object None : Config()
    }

    sealed class Wrapper {
        class AvatarCutter(val viewModel: AvatarCutterViewModel) : Wrapper()
        data object None : Wrapper()
    }
}
