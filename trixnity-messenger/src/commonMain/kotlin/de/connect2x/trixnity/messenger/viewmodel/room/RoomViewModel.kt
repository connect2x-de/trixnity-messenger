package de.connect2x.trixnity.messenger.viewmodel.room

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouter
import de.connect2x.trixnity.messenger.viewmodel.room.settings.SettingsRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalUserCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile


private val log = KotlinLogging.logger {}

interface RoomViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        isBackButtonVisible: MutableStateFlow<Boolean>,
        showedUserId: MutableStateFlow<UserId?>,
        onRoomBack: () -> Unit,
        onOpenModal: OpenModalUserCallback,
        onOpenMention: OpenMentionCallback,
        onOpenAvatarCutter: (UserId, RoomId, FileDescriptor) -> Unit,
    ): RoomViewModel {
        return RoomViewModelImpl(
            viewModelContext = viewModelContext,
            roomId = selectedRoomId,
            onRoomBack = onRoomBack,
            isBackButtonVisible = isBackButtonVisible,
            showedUserId = showedUserId,
            onOpenModal = onOpenModal,
            onOpenMention = onOpenMention,
            onOpenAvatarCutter = onOpenAvatarCutter,
        )
    }

    companion object : RoomViewModelFactory
}

interface RoomViewModel {
    val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>>
    val settingsStack: Value<ChildStack<SettingsRouter.Config, SettingsRouter.Wrapper>>
    val isShowSettings: StateFlow<Boolean>
    val isShowUserProfile: StateFlow<Boolean>
    val isTwoPane: StateFlow<Boolean>
    fun onRoomBack()
    fun setSinglePane(twoPane: Boolean)
    fun showSettings()
}

open class RoomViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onRoomBack: () -> Unit,
    private val showedUserId: MutableStateFlow<UserId?>,
    onOpenModal: OpenModalUserCallback,
    onOpenMention: OpenMentionCallback,
    isBackButtonVisible: MutableStateFlow<Boolean>,
    onOpenAvatarCutter: (UserId, RoomId, FileDescriptor) -> Unit,
) : MatrixClientViewModelContext by viewModelContext, RoomViewModel {

    override val isTwoPane = MutableStateFlow(false)

    override val isShowSettings = MutableStateFlow(false)

    override val isShowUserProfile: StateFlow<Boolean> = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val settingsRouter: SettingsRouter = SettingsRouterImpl(
        viewModelContext = viewModelContext,
        roomId = roomId,
        onRoomBack = onRoomBack,
        showedUserId = showedUserId.flatMapLatest {
            it?.let {
                matrixClient.user.getById(roomId, it)
            } ?: flowOf(null)
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null),
        onSettingsBack = ::onSettingsBack,
        onCloseUserProfile = ::onCloseUserProfile,
        onOpenAvatarCutter = onOpenAvatarCutter,
    )

    private val timelineRouter: TimelineRouter = TimelineRouterImpl(
        viewModelContext = viewModelContext,
        isBackButtonVisible = isBackButtonVisible,
        onShowSettings = ::onShowSettings,
        onRoomBack = onRoomBack,
        onOpenModal = { type: OpenModalType, mxcUrl: String, encryptedFile: EncryptedFile?, fileName: String ->
            onOpenModal(type, mxcUrl, encryptedFile, fileName, userId)
        },
        onOpenMention = onOpenMention
    )

    override val timelineStack: Value<ChildStack<TimelineRouter.Config, TimelineRouter.Wrapper>> =
        timelineRouter.stack
    override val settingsStack: Value<ChildStack<SettingsRouter.Config, SettingsRouter.Wrapper>> =
        settingsRouter.stack

    init {
        log.debug { "create RoomViewModel " + roomId.full }
        coroutineScope.launch { timelineRouter.showTimeline(roomId) }
        settingsStack.subscribe {
            isShowSettings.value = it.active.instance !is SettingsRouter.Wrapper.None
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

    internal fun onCloseUserProfile() {
        showedUserId.value = null
    }

    internal fun onSettingsBack() = coroutineScope.launch {
        if (showedUserId.value != null) {
            showedUserId.value = null
        } else {
            settingsRouter.closeSettings()
            timelineRouter.showTimeline(roomId)
        }
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
    override val settingsStack: Value<ChildStack<SettingsRouter.Config, SettingsRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = SettingsRouter.Config.None,
                    instance = SettingsRouter.Wrapper.None,
                )
            )
        )

    override val isShowSettings: StateFlow<Boolean> = MutableStateFlow(false)
    override val isShowUserProfile: StateFlow<Boolean> = MutableStateFlow(false)
    override val isTwoPane: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override fun onRoomBack() {}
    override fun setSinglePane(twoPane: Boolean) {
        isTwoPane.value = twoPane
    }

    override fun showSettings() {
    }
}
