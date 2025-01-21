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
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.None as ExtrasNone
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Wrapper.None as ExtrasWrapperNone
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Config.None as TimelineNone
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter.Wrapper.None as TimelineWrapperNone


private val log = KotlinLogging.logger {}

interface RoomViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        onCloseRoom: () -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenAvatarCutter: OpenAvatarCutterCallback,
    ): RoomViewModel = RoomViewModelImpl(
        viewModelContext = viewModelContext,
        roomId = selectedRoomId,
        onCloseRoom = onCloseRoom,
        onOpenMention = onOpenMention,
        onOpenAvatarCutter = onOpenAvatarCutter,
    )

    companion object : RoomViewModelFactory
}

interface RoomViewModel {
    val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>>
    val extrasStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>>
    val isRoomSettingsShown: StateFlow<Boolean>
    val isExtrasShown: StateFlow<Boolean>
    fun closeRoom()
    fun openRoomSettings()
    fun openMessageMetadata(eventId: EventId)
}

open class RoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onCloseRoom: () -> Unit,
    onOpenMention: OpenMentionCallback,
    onOpenAvatarCutter: OpenAvatarCutterCallback,
) : MatrixClientViewModelContext by viewModelContext, RoomViewModel {

    override val isRoomSettingsShown = MutableStateFlow(false)
    override val isExtrasShown = MutableStateFlow(false)

    override fun closeRoom() {
        onCloseRoom()
    }

    override fun openRoomSettings() {
        onOpenRoomSettings()
    }

    override fun openMessageMetadata(eventId: EventId) {
        onOpenMessageMetadata(eventId)
    }

    private val extrasRouter: ExtrasRouter = ExtrasRouterImpl(
        viewModelContext = viewModelContext,
        onCloseRoom = ::onCloseRoom,
        onCloseSettings = ::onCloseSettings,
        onOpenAvatarCutter = onOpenAvatarCutter,
    )
    override val extrasStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>> =
        extrasRouter.stack

    private val timelineRouter: TimelineRouter = TimelineRouterImpl(
        viewModelContext = viewModelContext,
        onCloseRoom = ::onCloseRoom,
        onOpenRoomSettings = ::onOpenRoomSettings,
        onOpenMention = onOpenMention,
        onOpenMetadata = ::onOpenMessageMetadata,
    )
    override val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>> =
        timelineRouter.stack

    init {
        log.debug { "create RoomViewModel for: ${roomId.full} " }
        coroutineScope.launch { timelineRouter.openTimeline(roomId) }
        extrasStack.subscribe {
            isRoomSettingsShown.value = it.active.configuration is RoomSettings
            isExtrasShown.value = it.active.configuration !is ExtrasNone
        }
    }

    private fun onCloseRoom() {
        this.onCloseRoom.invoke()
    }

    private fun onOpenRoomSettings() = coroutineScope.launch {
        extrasRouter.openRoomSettings(roomId)
    }

    private fun onOpenMessageMetadata(eventId: EventId) = coroutineScope.launch {
        extrasRouter.openMessageMetadata(eventId, roomId)
    }

    private fun onCloseSettings() = coroutineScope.launch {
        extrasRouter.closeExtrasRouter()
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
    override val isExtrasShown = MutableStateFlow(false)
    override fun closeRoom() {}
    override fun openRoomSettings() {}
    override fun openMessageMetadata(eventId: EventId) {}
}
