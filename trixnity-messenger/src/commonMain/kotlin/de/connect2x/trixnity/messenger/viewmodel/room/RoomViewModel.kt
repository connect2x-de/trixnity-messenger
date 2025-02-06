package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.settings.OpenAvatarCutterCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.None as ExtrasNone
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.None as ExtrasWrapperNone
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Config.None as TimelineNone
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Wrapper.None as TimelineWrapperNone


private val log = KotlinLogging.logger {}

interface RoomViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onOpenRoom: (UserId, RoomId) -> Unit,
        onCloseRoom: () -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenAvatarCutter: OpenAvatarCutterCallback,
    ): RoomViewModel = RoomViewModelImpl(
        viewModelContext = viewModelContext,
        roomId = selectedRoomId,
        onOpenRoom = onOpenRoom,
        onCloseRoom = onCloseRoom,
        onOpenMention = onOpenMention,
        onOpenAvatarCutter = onOpenAvatarCutter,
    )

    companion object : RoomViewModelFactory
}

/**
 * The view model that is active when a room is selected from the room list.
 */
interface RoomViewModel {
    /**
     * Holds the content of the messenger timeline related to the selected room.
     */
    val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>>

    /**
     * Holds the content of the selected details or settings related to the selected room.
     */
    val extrasStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>>

    /**
     * Indicates whether any actual content should be shown.
     * @return `true` if any or more configs besides
     * [ExtrasRouter.Config.None] are currently active.
     */
    val isShown: StateFlow<Boolean>

    /**
     * Indicates whether any of the room settings should be shown.
     * @return `true` if any of the following configs are active:
     * * [RoomSettings.Main]
     * * [RoomSettings.AddMembers]
     * * [RoomSettings.ExportRoom]
     */
    val isRoomSettingsShown: StateFlow<Boolean>

    /**
     * Requests to close the currently selected room.
     */
    fun closeRoom()

    /**
     * Opens [RoomSettings.Main] and replaces any other currently active extras configs.
     */
    fun openRoomSettings()

    /**
     * Opens [ExtrasRouter.Config.Details.UserProfile]
     * for the [userId] and currently selected room.
     */
    fun openUserProfile(userId: UserId)
}

open class RoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    onOpenRoom: (UserId, RoomId) -> Unit,
    private val onCloseRoom: () -> Unit,
    onOpenMention: OpenMentionCallback,
    onOpenAvatarCutter: OpenAvatarCutterCallback,
) : MatrixClientViewModelContext by viewModelContext, RoomViewModel {

    override val isRoomSettingsShown = MutableStateFlow(false)
    override val isShown = MutableStateFlow(false)

    override fun closeRoom() {
        onCloseRoom()
    }

    override fun openRoomSettings() {
        onOpenRoomSettings()
    }

    override fun openUserProfile(userId: UserId) {
        onOpenUserProfile(userId)
    }

    private val extrasRouter: ExtrasRouter = ExtrasRouterImpl(
        viewModelContext = viewModelContext,
        onOpenRoom = onOpenRoom,
        onCloseRoom = ::onCloseRoom,
        onOpenAvatarCutter = onOpenAvatarCutter,
    )
    override val extrasStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>> =
        extrasRouter.stack

    private val timelineRouter: TimelineRouter = TimelineRouterImpl(
        viewModelContext = viewModelContext,
        onOpenRoomSettings = ::onOpenRoomSettings,
        onOpenUserProfile = ::onOpenUserProfile,
        onOpenMention = onOpenMention,
        onCloseRoom = ::onCloseRoom,
    )
    override val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>> =
        timelineRouter.stack

    init {
        log.debug { "create RoomViewModel for: ${roomId.full} " }
        coroutineScope.launch { timelineRouter.openTimeline(roomId) }
        extrasStack.subscribe {
            isRoomSettingsShown.value = it.active.configuration is RoomSettings
            isShown.value = it.active.configuration !is ExtrasNone
        }
    }

    private fun onCloseRoom() {
        this.onCloseRoom.invoke()
    }

    private fun onOpenRoomSettings() =
        coroutineScope.launch {
            extrasRouter.openRoomSettings(roomId)
        }

    private fun onOpenUserProfile(userId: UserId) =
        coroutineScope.launch {
            extrasRouter.openUserProfile(userId, roomId)
        }
}

class PreviewRoomViewModel : RoomViewModel {
    override val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = TimelineNone,
                    instance = TimelineWrapperNone,
                )
            )
        )
    override val extrasStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = ExtrasNone,
                    instance = ExtrasWrapperNone,
                )
            )
        )
    override val isRoomSettingsShown = MutableStateFlow(false)
    override val isShown = MutableStateFlow(false)
    override fun closeRoom() {}
    override fun openRoomSettings() {}
    override fun openUserProfile(userId: UserId) {}
}
