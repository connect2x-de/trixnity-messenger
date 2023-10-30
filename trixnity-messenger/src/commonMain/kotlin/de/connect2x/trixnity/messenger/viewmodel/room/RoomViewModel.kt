package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouterImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile

private val log = KotlinLogging.logger {}

interface RoomViewModelFactory {
    fun newRoomViewModel(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        onRoomBack: () -> Unit,
        onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, accountName: String) -> Unit,
    ): RoomViewModel {
        return RoomViewModelImpl(
            viewModelContext = viewModelContext,
            roomId = selectedRoomId,
            onRoomBack = onRoomBack,
            isBackButtonVisible = isBackButtonVisible,
            onOpenModal = onOpenModal
        )
    }
}

interface RoomViewModel {
    val timelineStack: Value<ChildStack<TimelineRouter.TimelineConfig, TimelineRouter.TimelineWrapper>>
    val settingsStack: Value<ChildStack<SettingsRouter.SettingsConfig, SettingsRouter.SettingsWrapper>>
    val isShowSettings: StateFlow<Boolean>
    val isTwoPane: StateFlow<Boolean>
    fun onRoomBack()
    fun setSinglePane(twoPane: Boolean)
    fun selectFile(file: FileDescriptor)
    fun dragFile(file: FileDescriptor)
    fun dragFileExit()
    fun showSettings()
}

open class RoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onRoomBack: () -> Unit,
    onOpenModal: (type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String, accountName: String) -> Unit,
    isBackButtonVisible: MutableStateFlow<Boolean>,
) : MatrixClientViewModelContext by viewModelContext, RoomViewModel {

    override val isTwoPane = MutableStateFlow(false)

    override val isShowSettings = MutableStateFlow(false)


    private val settingsRouter: SettingsRouter = SettingsRouterImpl(
        viewModelContext = viewModelContext,
        roomId = roomId,
        onRoomBack = onRoomBack,
        onSettingsBack = ::onSettingsBack
    )

    private val timelineRouter: TimelineRouter = TimelineRouterImpl(
        viewModelContext = viewModelContext,
        isBackButtonVisible = isBackButtonVisible,
        onShowSettings = ::onShowSettings,
        onRoomBack = onRoomBack,
        onOpenModal = { type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String ->
            onOpenModal(type, mxcUrl, encryptedFile, fileName, accountName)
        },
    )

    override val timelineStack: Value<ChildStack<TimelineRouter.TimelineConfig, TimelineRouter.TimelineWrapper>> =
        timelineRouter.timelineStack
    override val settingsStack: Value<ChildStack<SettingsRouter.SettingsConfig, SettingsRouter.SettingsWrapper>> =
        settingsRouter.settingsStack

    init {
        log.debug { "create RoomViewModel " + roomId.full }
        coroutineScope.launch { timelineRouter.showTimeline(roomId) }
        settingsStack.subscribe {
            isShowSettings.value = it.active.instance !is SettingsRouter.SettingsWrapper.None
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

    override fun selectFile(file: FileDescriptor) {
        val instance = timelineStack.value.active.instance
        if (instance is TimelineRouter.TimelineWrapper.View) {
            instance.timelineViewModel.selectFile(file)
        }
    }

    override fun dragFile(file: FileDescriptor) {
        val instance = timelineStack.value.active.instance
        if (instance is TimelineRouter.TimelineWrapper.View) {
            instance.timelineViewModel.dragFile(file)
        }
    }

    override fun dragFileExit() {
        val instance = timelineStack.value.active.instance
        if (instance is TimelineRouter.TimelineWrapper.View) {
            instance.timelineViewModel.dragFileExit()
        }
    }

    override fun showSettings() {
        onShowSettings()
    }

    private fun switchToMultiPane() = coroutineScope.launch {
        if (settingsRouter.isShown()) {
            timelineRouter.showTimeline(roomId)
            settingsRouter.showSettings()
        } else {
            timelineRouter.showTimeline(roomId)
        }
    }

    private fun switchToSinglePane() = coroutineScope.launch {
        if (settingsRouter.isShown()) {
            timelineRouter.closeTimeline()
        } else {
            timelineRouter.showTimeline(roomId)
        }
    }

    internal fun onSettingsBack() = coroutineScope.launch {
        settingsRouter.closeSettings()
        timelineRouter.showTimeline(roomId)
    }

    internal fun onShowSettings() = coroutineScope.launch {
        settingsRouter.showSettings()
        if (isTwoPane.value) {
            timelineRouter.closeTimeline()
        } else {
            timelineRouter.showTimeline(roomId)
        }
    }

}

class PreviewRoomViewModel() : RoomViewModel {
    override val timelineStack: Value<ChildStack<TimelineRouter.TimelineConfig, TimelineRouter.TimelineWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = TimelineRouter.TimelineConfig.None,
                    instance = TimelineRouter.TimelineWrapper.None,
                )
            )
        )
    override val settingsStack: Value<ChildStack<SettingsRouter.SettingsConfig, SettingsRouter.SettingsWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = SettingsRouter.SettingsConfig.None,
                    instance = SettingsRouter.SettingsWrapper.None,
                )
            )
        )
    override val isShowSettings: StateFlow<Boolean> = MutableStateFlow(false)
    override val isTwoPane: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun onRoomBack() {}
    override fun setSinglePane(twoPane: Boolean) {
        isTwoPane.value = twoPane
    }

    override fun selectFile(file: FileDescriptor) {
    }

    override fun dragFile(file: FileDescriptor) {
    }

    override fun dragFileExit() {
    }

    override fun showSettings() {
    }
}