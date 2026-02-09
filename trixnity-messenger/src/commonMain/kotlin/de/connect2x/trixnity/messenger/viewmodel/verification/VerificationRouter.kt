package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.serialization.Serializable
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

class VerificationRouter(
    private val viewModelContext: ViewModelContext,
    private val routerKey: String,
    private val onRedoSelfVerification: (userId: UserId) -> Unit,
) {
    private val navigation = StackNavigation<Config>()

    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "VerificationRouter-${routerKey}",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper =
        when (config) {
            is Config.DeviceVerification -> Wrapper.Verification(
                viewModelContext.get<VerificationViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext("DeviceVerification", componentContext, config.userId),
                    onCloseVerification = ::closeVerification,
                    onRedoSelfVerification = { onRedoSelfVerification(config.userId) },
                    roomId = null,
                    timelineEventId = null,
                )
            )

            is Config.UserVerification -> Wrapper.Verification(
                viewModelContext.get<VerificationViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext("UserVerification", componentContext, config.userId),
                    onCloseVerification = ::closeVerification,
                    onRedoSelfVerification = { onRedoSelfVerification(config.userId) },
                    roomId = config.roomId,
                    timelineEventId = config.timelineEventId,
                )
            )

            is Config.None -> Wrapper.None
        }

    suspend fun startDeviceVerification(userId: UserId) {
        if (stack.value.active.configuration !is Config.DeviceVerification) {
            navigation.pushSuspending(Config.DeviceVerification(userId))
        }
    }

    suspend fun startUserVerification(roomId: RoomId, timelineEventId: EventId, userId: UserId) {
        if (stack.value.active.configuration !is Config.UserVerification) {
            navigation.pushSuspending(Config.UserVerification(roomId, timelineEventId, userId))
        }
    }

    fun closeVerification() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    @Serializable
    sealed class Config {
        @Serializable
        data class DeviceVerification(val userId: UserId) : Config()

        @Serializable
        data class UserVerification(val roomId: RoomId, val timelineEventId: EventId, val userId: UserId) : Config()

        @Serializable
        data object None : Config()
    }

    sealed class Wrapper {
        class Verification(val viewModel: VerificationViewModel) : Wrapper()
        data object None : Wrapper()
    }
}
