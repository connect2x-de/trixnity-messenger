package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.Parcel
import de.connect2x.trixnity.messenger.Parceler
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get

class VerificationRouter(
    private val viewModelContext: ViewModelContext,
    private val onRedoSelfVerification: (String) -> Unit,
) {
    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        initialConfiguration = Config.None,
        key = "DeviceVerificationRouter",
        childFactory = ::createChild,
    )

    private fun createChild(config: Config, componentContext: ComponentContext): VerificationWrapper =
        when (config) {
            is Config.DeviceVerification -> VerificationWrapper.Verification(
                viewModelContext.get<VerificationViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.accountName),
                    onCloseVerification = ::closeVerification,
                    onRedoSelfVerification = { onRedoSelfVerification(config.accountName) },
                    roomId = null,
                    timelineEventId = null,
                )
            )

            is Config.UserVerification -> VerificationWrapper.Verification(
                viewModelContext.get<VerificationViewModelFactory>().create(
                    viewModelContext = viewModelContext.childContext(componentContext, config.accountName),
                    onCloseVerification = ::closeVerification,
                    onRedoSelfVerification = { onRedoSelfVerification(config.accountName) },
                    roomId = config.roomId,
                    timelineEventId = config.timelineEventId,
                )
            )

            is Config.None -> VerificationWrapper.None
        }

    suspend fun startDeviceVerification(accountName: String) {
        if (stack.value.active.configuration !is Config.DeviceVerification) {
            navigation.pushSuspending(Config.DeviceVerification(accountName))
        }
    }

    suspend fun startUserVerification(roomId: RoomId, timelineEventId: EventId, accountName: String) {
        if (stack.value.active.configuration !is Config.UserVerification) {
            navigation.pushSuspending(Config.UserVerification(roomId, timelineEventId, accountName))
        }
    }

    fun closeVerification() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    sealed class Config : Parcelable {
        @Parcelize
        data class DeviceVerification(val accountName: String) : Config()

        @Parcelize
        data class UserVerification(val roomId: RoomId, val timelineEventId: EventId, val accountName: String) :
            Config() {
            private companion object : Parceler<UserVerification> {
                override fun UserVerification.write(parcel: Parcel, flags: Int) {
                    parcel.writeString(this.roomId.full)
                    parcel.writeString(this.timelineEventId.full)
                    parcel.writeString(this.accountName)
                }

                override fun create(parcel: Parcel): UserVerification {
                    val roomId = RoomId(parcel.readString() ?: "")
                    val timelineEventId = EventId(parcel.readString() ?: "")
                    val accountName = parcel.readString() ?: ""
                    return UserVerification(roomId, timelineEventId, accountName)
                }
            }
        }

        @Parcelize
        object None : Config()
    }

    sealed class VerificationWrapper {
        class Verification(val verificationViewModel: VerificationViewModel) : VerificationWrapper()
        object None : VerificationWrapper()
    }
}
