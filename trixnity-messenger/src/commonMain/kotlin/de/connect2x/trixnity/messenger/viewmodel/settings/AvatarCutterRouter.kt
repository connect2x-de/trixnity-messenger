package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.FileDescriptor
import org.koin.core.component.get

class AvatarCutterRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = Config.None,
        key = "avatarCutter",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): AvatarCutterWrapper =
        when (config) {
            is Config.None -> AvatarCutterWrapper.None
            is Config.AvatarCutter -> AvatarCutterWrapper.AvatarCutter(
                viewModelContext.get<AvatarCutterViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.accountName),
                    file = config.file,
                    onClose = ::onClose,
                )
            )
        }

    suspend fun show(accountName: String, file: FileDescriptor) {
        navigation.pushSuspending(Config.AvatarCutter(accountName, file))
    }

    suspend fun close() {
        navigation.popSuspending()
    }

    private fun onClose() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }


    sealed class Config : Parcelable {
        @Parcelize
        data class AvatarCutter(val accountName: String, val file: FileDescriptor) : Config()

        @Parcelize
        object None : Config()
    }

    sealed class AvatarCutterWrapper {
        class AvatarCutter(val avatarCutterViewModel: AvatarCutterViewModel) : AvatarCutterWrapper()
        object None : AvatarCutterWrapper()
    }
}
