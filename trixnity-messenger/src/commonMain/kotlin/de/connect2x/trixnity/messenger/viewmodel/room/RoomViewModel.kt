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
        onRoomBack: () -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenAvatarCutter: OpenAvatarCutterCallback,
    ): RoomViewModel = RoomViewModelImpl(
        viewModelContext = viewModelContext,
        roomId = selectedRoomId,
        onRoomBack = onRoomBack,
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

    fun onRoomBack()
    fun showSettings()
    fun showMessageMetadata(eventId: EventId)
}

open class RoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onRoomBack: () -> Unit,
    onOpenMention: OpenMentionCallback,
    onOpenAvatarCutter: OpenAvatarCutterCallback,
) : MatrixClientViewModelContext by viewModelContext, RoomViewModel {

    override val isRoomSettingsShown = MutableStateFlow(false)
    override val isExtrasShown = MutableStateFlow(false)

    private val extrasRouter: ExtrasRouter = ExtrasRouterImpl(
        viewModelContext = viewModelContext,
        onRoomBack = onRoomBack,
        onSettingsBack = ::onSettingsBack,
        onOpenAvatarCutter = onOpenAvatarCutter,
    )

    private val timelineRouter: TimelineRouter = TimelineRouterImpl(
        viewModelContext = viewModelContext,
        onShowSettings = ::onShowRoomSettings,
        onRoomBack = onRoomBack,
        onOpenMention = onOpenMention,
        onOpenMetadata = ::onShowMessageMetadata,
    )

    override val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>> =
        timelineRouter.stack
    override val extrasStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>> =
        extrasRouter.stack

    init {
        log.debug { "create RoomViewModel for: ${roomId.full} " }
        coroutineScope.launch { timelineRouter.showTimeline(roomId) }
        extrasStack.subscribe {
            isRoomSettingsShown.value = it.active.configuration is RoomSettings
            isExtrasShown.value = it.active.configuration !is ExtrasNone
        }
    }

    override fun showSettings() {
        onShowRoomSettings()
    }

    override fun showMessageMetadata(eventId: EventId) {
        onShowMessageMetadata(eventId)
    }

    override fun onRoomBack() {
        this.onRoomBack.invoke()
    }

    internal fun onShowRoomSettings() = coroutineScope.launch {
        extrasRouter.showRoomSettings(roomId)
    }

    internal fun onShowMessageMetadata(eventId: EventId) = coroutineScope.launch {
        extrasRouter.showMessageMetadata(eventId, roomId)
    }

    internal fun onSettingsBack() = coroutineScope.launch {
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
    override fun onRoomBack() {}
    override fun showSettings() {}
    override fun showMessageMetadata(eventId: EventId) {}
}
