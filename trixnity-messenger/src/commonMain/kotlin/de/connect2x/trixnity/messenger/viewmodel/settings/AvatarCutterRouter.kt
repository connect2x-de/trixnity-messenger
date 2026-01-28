package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

class AvatarCutterRouter(
    private val viewModelContext: ViewModelContext,
) {
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = null,
        initialConfiguration = Config.None,
        key = "avatarCutter",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper =
        when (config) {
            is Config.None -> Wrapper.None
            is Config.AvatarCutterUserProfile -> Wrapper.AvatarCutter(
                viewModelContext.get<AvatarCutterViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext("AvatarCutterUserProfile", componentContext, config.userId),
                    file = config.file,
                    onClose = ::onClose,
                )
            )

            is Config.AvatarCutterRoom -> Wrapper.AvatarCutter(
                viewModelContext.get<AvatarCutterViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext("AvatarCutterRoom", componentContext, config.userId),
                    roomId = config.roomId,
                    file = config.file,
                    onClose = ::onClose,
                )
            )
        }

    suspend fun show(userId: UserId, file: FileDescriptor) {
        navigation.pushSuspending(Config.AvatarCutterUserProfile(userId, file))
    }

    suspend fun show(userId: UserId, roomId: RoomId, file: FileDescriptor) {
        navigation.pushSuspending(Config.AvatarCutterRoom(userId, roomId, file))
    }

    suspend fun close() {
        navigation.popSuspending()
    }

    private fun onClose() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    sealed class Config {
        data class AvatarCutterUserProfile(
            val userId: UserId,
            val file: FileDescriptor,
        ) : Config()

        data class AvatarCutterRoom(
            val userId: UserId,
            val roomId: RoomId,
            val file: FileDescriptor,
        ) : Config()

        data object None : Config()
    }

    sealed class Wrapper {
        class AvatarCutter(val viewModel: AvatarCutterViewModel) : Wrapper()
        data object None : Wrapper()
    }
}
