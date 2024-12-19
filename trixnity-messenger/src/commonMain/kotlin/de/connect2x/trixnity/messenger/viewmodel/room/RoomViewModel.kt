package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.None
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter.Config.RoomSettings
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId


private val log = KotlinLogging.logger {}

interface RoomViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        onRoomBack: () -> Unit,
        onOpenMention: OpenMentionCallback,
        onOpenAvatarCutter: (UserId, RoomId, FileDescriptor) -> Unit,
    ): RoomViewModel {
        return RoomViewModelImpl(
            viewModelContext = viewModelContext,
            roomId = selectedRoomId,
            onRoomBack = onRoomBack,
            isBackButtonVisible = isBackButtonVisible,
            onOpenMention = onOpenMention,
            onOpenAvatarCutter = onOpenAvatarCutter,
        )
    }

    companion object : RoomViewModelFactory
}

interface RoomViewModel {
    val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>>
    val settingsStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>>
    val isSettingsShown: StateFlow<Boolean>
    val isExtrasShown: StateFlow<Boolean>
    val isTwoPane: StateFlow<Boolean>
    fun onRoomBack()
    fun setSinglePane(twoPane: Boolean)
    fun showSettings()
    fun showMessageMetadata(messageHolder: TimelineElementHolderViewModel)
}

open class RoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onRoomBack: () -> Unit,
    onOpenMention: OpenMentionCallback,
    isBackButtonVisible: MutableStateFlow<Boolean>,
    onOpenAvatarCutter: (UserId, RoomId, FileDescriptor) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomViewModel {

    override val isSettingsShown = MutableStateFlow(false)
    override val isExtrasShown = MutableStateFlow(false)
    override val isTwoPane = MutableStateFlow(false)

    private val extrasRouter: ExtrasRouter = ExtrasRouterImpl(
        viewModelContext = viewModelContext,
        onRoomBack = onRoomBack,
        onSettingsBack = ::onSettingsBack,
        onOpenAvatarCutter = onOpenAvatarCutter,
    )

    private val timelineRouter: TimelineRouter = TimelineRouterImpl(
        viewModelContext = viewModelContext,
        isBackButtonVisible = isBackButtonVisible,
        onShowSettings = ::onShowSettings,
        onRoomBack = onRoomBack,
        onOpenMention = onOpenMention,
        onOpenMetadata = ::showMessageMetadata,
    )

    override val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>> =
        timelineRouter.stack
    override val settingsStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>> =
        extrasRouter.stack

    init {
        log.debug { "create RoomViewModel " + roomId.full }
        coroutineScope.launch { timelineRouter.showTimeline(roomId) }
        settingsStack.subscribe {
            isSettingsShown.value = it.active.configuration is RoomSettings
            isExtrasShown.value = it.active.configuration !is None
        }
    }

    override fun onRoomBack() {
        this.onRoomBack.invoke()
    }

    override fun setSinglePane(singlePane: Boolean) {
        if (singlePane != isTwoPane.value) {
            isTwoPane.value = singlePane
            if (singlePane) {
                switchToSinglePane()
            } else {
                switchToMultiPane()
            }
        }
    }

    override fun showSettings() {
        onShowSettings()
    }

    override fun showMessageMetadata(messageHolder: TimelineElementHolderViewModel) {
        coroutineScope.launch {
            extrasRouter.showMessageMetadata(messageHolder.eventId, roomId)
        }
    }

    private fun switchToMultiPane() = coroutineScope.launch {
        if (extrasRouter.isExtrasRouterShown()) {
            timelineRouter.showTimeline(roomId)
            extrasRouter.showSettings(roomId)
        } else {
            timelineRouter.showTimeline(roomId)
        }
    }

    private fun switchToSinglePane() = coroutineScope.launch {
        if (extrasRouter.isExtrasRouterShown()) {
            timelineRouter.closeTimeline()
        } else {
            timelineRouter.showTimeline(roomId)
        }
    }

    internal fun onSettingsBack() = coroutineScope.launch {
        extrasRouter.closeSettings()
        timelineRouter.showTimeline(roomId)
    }

    internal fun onShowSettings() = coroutineScope.launch {
        extrasRouter.showSettings(roomId)
        if (isTwoPane.value) {
            timelineRouter.closeTimeline()
        } else {
            timelineRouter.showTimeline(roomId)
        }
    }

}

class PreviewRoomViewModel : RoomViewModel {
    override val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = TimelineRouter.Config.None,
                    instance = TimelineRouter.Wrapper.None,
                )
            )
        )
    override val settingsStack: Value<ChildStack<ExtrasRouter.Config, ExtrasRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = None,
                    instance = ExtrasRouter.Wrapper.None,
                )
            )
        )
    override val isSettingsShown: StateFlow<Boolean> = MutableStateFlow(false)
    override val isExtrasShown: StateFlow<Boolean> = MutableStateFlow(false)
    override val isTwoPane: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun onRoomBack() {}
    override fun showSettings() {}
    override fun showMessageMetadata(messageHolder: TimelineElementHolderViewModel) {}
    override fun setSinglePane(twoPane: Boolean) {
        isTwoPane.value = twoPane
    }
}
