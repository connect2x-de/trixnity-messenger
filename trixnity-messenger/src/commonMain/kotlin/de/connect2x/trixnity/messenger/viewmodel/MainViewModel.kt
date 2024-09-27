package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import de.connect2x.trixnity.messenger.util.SendLogToDevs
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.files.ImageRouter
import de.connect2x.trixnity.messenger.viewmodel.files.VideoRouter
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.room.PreviewRoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.MessageMention
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewRoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTrigger
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get
import org.koin.core.component.inject


private val log = KotlinLogging.logger {}

interface MainViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCreateNewAccount: () -> Unit,
        onRemoveAccount: (userId: UserId) -> Unit,
    ): MainViewModel = MainViewModelImpl(
        viewModelContext,
        onCreateNewAccount,
        onRemoveAccount,
    )

    companion object : MainViewModelFactory
}

interface MainViewModel {
    val selectedRoomId: MutableStateFlow<RoomId?>
    val isBackButtonVisible: MutableStateFlow<Boolean>
    val isSinglePane: MutableStateFlow<Boolean>
    val initialSyncStack: Value<ChildStack<InitialSyncRouter.Config, InitialSyncRouter.Wrapper>>
    val selfVerificationStack: Value<ChildStack<SelfVerificationRouter.Config, SelfVerificationRouter.Wrapper>>
    val roomListRouterStack: Value<ChildStack<RoomListRouter.Config, RoomListRouter.Wrapper>>
    val roomRouterStack: Value<ChildStack<RoomRouter.Config, RoomRouter.Wrapper>>
    val imageRouterStack: Value<ChildStack<ImageRouter.Config, ImageRouter.Wrapper>>
    val videoRouterStack: Value<ChildStack<VideoRouter.Config, VideoRouter.Wrapper>>
    val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>>
    val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.Wrapper>>
    val showRoom: StateFlow<Boolean>
    val settingsWizardRouterStack: Value<ChildStack<SettingsWizardRouter.Config, SettingsWizardRouter.Wrapper>>

    // ATTENTION: the viewmodel has to be explicitly started as the routers cannot be not initialized in the init block
    fun start()
    fun closeDetailsAndShowList()
    fun onRoomSelected(userId: UserId, id: RoomId)
    fun onOpenAvatarCutter(userId: UserId, file: FileDescriptor)
    fun onOpenAvatarCutter(userId: UserId, selectedRoomId: RoomId, file: FileDescriptor)

    fun setSinglePane(isSinglePane: Boolean)
    fun openModal(
        type: OpenModalType,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        userId: UserId
    )

    fun openMention(userId: UserId, messageMention: MessageMention)

    fun closeAccountsOverview()
}

open class MainViewModelImpl(
    viewModelContext: ViewModelContext,
    onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (UserId) -> Unit,
) : ViewModelContext by viewModelContext, MainViewModel {

    private val activeVerifications =
        MutableStateFlow(setOf<UserId>()) // in case of multiple active verifications, we need to do them one after another
    private val messengerSettings by inject<MatrixMessengerSettingsHolder>()

    override val selectedRoomId = MutableStateFlow<RoomId?>(null)
    override val isBackButtonVisible = MutableStateFlow(true)
    override val isSinglePane = MutableStateFlow(false)
    override val showRoom = MutableStateFlow(false)

    internal val selfVerificationRouter = SelfVerificationRouter(viewModelContext)
    override val selfVerificationStack: Value<ChildStack<SelfVerificationRouter.Config, SelfVerificationRouter.Wrapper>> =
        selfVerificationRouter.stack


    private val backCallback = BackCallback {
        backPressHandler()
    }

    init { // init before routers, so those can register other handlers that are executed before
        backHandler.register(backCallback)
    }

    private val initialSyncRouter = InitialSyncRouter(viewModelContext = viewModelContext)
    override val initialSyncStack = initialSyncRouter.stack

    private val roomListRouter: RoomListRouter =
        RoomListRouter(
            viewModelContext = viewModelContext,
            selectedRoomId = selectedRoomId,
            onRoomSelected = ::onRoomSelected,
            onOpenAvatarCutter = ::onOpenAvatarCutter,
            onSendLogs = ::onSendLogs,
            onCreateNewAccount = onCreateNewAccount,
            onRemoveAccount = ::onRemoveAccountInternal,
            onAccountSelected = ::closeRoom,
        )
    override val roomListRouterStack: Value<ChildStack<RoomListRouter.Config, RoomListRouter.Wrapper>> =
        roomListRouter.stack

    private val roomRouter: RoomRouter =
        RoomRouterImpl(
            viewModelContext = viewModelContext,
            isBackButtonVisible = isBackButtonVisible,
            onCloseRoom = ::closeDetailsAndShowList,
            onOpenModal = ::openModal,
            onOpenMention = ::openMention,
            onOpenAvatarCutter = ::onOpenAvatarCutter,
        )
    override val roomRouterStack: Value<ChildStack<RoomRouter.Config, RoomRouter.Wrapper>> = roomRouter.stack

    private val selfVerificationTrigger = get<SelfVerificationTrigger>()

    init {
        coroutineScope.launch {
            roomRouterStack.subscribe {
                showRoom.value = it.active.instance !is RoomRouter.Wrapper.None
            }
            selfVerificationTrigger.onInvoke.collect {
                log.debug { "triggered self verification for user: $it" }
                onOpenSelfVerification(it)
            }
        }
    }

    private val imageRouter: ImageRouter = ImageRouter(viewModelContext = viewModelContext)
    override val imageRouterStack: Value<ChildStack<ImageRouter.Config, ImageRouter.Wrapper>> = imageRouter.stack

    private val videoRouter: VideoRouter = VideoRouter(viewModelContext = viewModelContext)
    override val videoRouterStack: Value<ChildStack<VideoRouter.Config, VideoRouter.Wrapper>> =
        videoRouter.stack

    private val verificationRouter: VerificationRouter =
        VerificationRouter(
            viewModelContext = viewModelContext,
            routerKey = "deviceVerification",
            onRedoSelfVerification = selfVerificationRouter::redoSelfVerification
        )
    override val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>> =
        verificationRouter.stack

    private val avatarCutterRouter: AvatarCutterRouter = AvatarCutterRouter(viewModelContext = viewModelContext)
    override val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.Wrapper>> =
        avatarCutterRouter.stack

    private val settingsWizardRouter : SettingsWizardRouter =
        SettingsWizardRouter(viewModelContext)

    override val settingsWizardRouterStack: Value<ChildStack<SettingsWizardRouter.Config, SettingsWizardRouter.Wrapper>> =
        settingsWizardRouter.stack

    private fun backPressHandler() {
        if (imageRouter.isImageOpen()) {
            imageRouter.closeImage()
        } else if (roomRouter.isShown() && isSinglePane.value) {
            closeDetailsAndShowList()
        } else {
            // TODO was "minimize", but we should use native routing without all the back press handlers
            //  native routing could also allow to use web history
            //  see also: https://github.com/arkivanov/Decompose/tree/master/sample/shared/shared/src/commonMain/kotlin/com/arkivanov/sample/shared/multipane
        }
    }

    private fun onRemoveAccountInternal(userId: UserId) {
        roomListRouter.closeAccountsOverview()
        this.onRemoveAccount(userId)
        coroutineScope.launch {
            if (messengerSettings.value.base.accounts.isEmpty()) {
                log.debug { "since all account have been removed, close all navigation" }
                roomRouter.closeRoom()
                roomListRouter.close()
                imageRouter.closeImage()
                videoRouter.closeVideo()
                avatarCutterRouter.close()
                initialSyncRouter.close()
                verificationRouter.closeVerification()
                log.debug { "finished closing all navigation" }
            }
        }
    }

    // ATTENTION: the viewmodel has to be explicitly started as the routers cannot be not initialized in the init block
    override fun start() {
        roomRouter.stack.subscribe { routerStack: ChildStack<RoomRouter.Config, RoomRouter.Wrapper> ->
            log.debug { "roomRouter has changed: ${routerStack.active.configuration::class.simpleName} (roomId: ${routerStack.active.configuration.getRoomId()})" }
            selectedRoomId.value = routerStack.active.configuration.getRoomId()
        }

        startSync()
        possiblyStartSelfVerification()
        startActiveVerificationsQueue()
        reactToActiveVerifications()
        reactToPresenceIsPublicChanges()
        startLoginWizard()
    }

    private fun startSync() {
        coroutineScope.launch {
            initialSyncRouter.stack.toFlow().collect { childStack ->
                if (childStack.active.configuration == InitialSyncRouter.Config.None) {
                    log.info { "initial sync / small sync is done -> now sync regularly" }
                    this@MainViewModelImpl.matrixClients.value.forEach { (userId, matrixClient) ->
                        val presenceIsPublic = messengerSettings[userId].first()?.base?.presenceIsPublic
                        log.debug { "start sync for $userId, presence is public: $presenceIsPublic" }
                        launch {
                            matrixClient.startSync(
                                presence = if (presenceIsPublic == true) Presence.ONLINE else Presence.OFFLINE
                            )
                        }
                    }
                }
            }
        }

        coroutineScope.launch {
            this@MainViewModelImpl.matrixClients
                .scan(
                    Pair<Set<UserId>, Set<UserId>>(emptySet(), emptySet())
                ) { old, new -> old.second to new.keys }
                .collect { (oldMatrixClients, newMatrixClients) ->
                    if (newMatrixClients.isNotEmpty() && oldMatrixClients != newMatrixClients) {
                        log.debug { "MatrixClient has been added, show sync" }
                        initialSyncRouter.showSync()
                    }
                }
        }

        lifecycle.doOnStop {
            coroutineScope.launch {
                withContext(NonCancellable) { // even when the scope is destroyed, we want the sync to stop
                    log.debug { "app is stopped: cancel sync" }
                    this@MainViewModelImpl.matrixClients.value.forEach { (userId, matrixClient) ->
                        log.debug { "stop sync for $userId" }
                        matrixClient.cancelSync(wait = false)
                    }
                }
            }

            // only when the app was stopped, we want to (re-)start the sync
            lifecycle.doOnStart(isOneTime = true) {
                coroutineScope.launch {
                    log.debug { "resume app: restart sync" }
                    this@MainViewModelImpl.matrixClients.value.forEach { (userId, matrixClient) ->
                        val presenceIsPublic = messengerSettings[userId].first()?.base?.presenceIsPublic
                        log.debug { "start sync for $userId, presence is public: $presenceIsPublic" }
                        matrixClient.startSync(
                            presence = if (presenceIsPublic == true) Presence.ONLINE else Presence.OFFLINE
                        )
                    }
                }
            }
        }
    }

    private fun possiblyStartSelfVerification() {
        coroutineScope.launch {
            matrixClients.scopedCollectLatest { namedMatrixClients ->
                namedMatrixClients.forEach { (userId, matrixClient) ->
                    log.debug { "launch listen for self verification methods (account $userId)" }
                    launch {
                        matrixClient.verification.getSelfVerificationMethods()
                            .distinctUntilChanged()
                            .collect { selfVerificationMethods ->
                                log.debug { "self verification methods (account $userId): $selfVerificationMethods" }
                                when (selfVerificationMethods) {
                                    is VerificationService.SelfVerificationMethods.PreconditionsNotMet -> {
                                        log.debug { "cannot determine yet if cross-signing is needed for $userId" }
                                    }

                                    is VerificationService.SelfVerificationMethods.NoCrossSigningEnabled -> {
                                        log.debug { "start bootstrapping $userId" }
                                        selfVerificationRouter.showBootstrap(userId)
                                    }

                                    is VerificationService.SelfVerificationMethods.AlreadyCrossSigned -> {
                                        log.debug { "client for $userId is already cross-signed" }
                                        selfVerificationRouter.closeSelfVerification(userId)
                                    }

                                    is VerificationService.SelfVerificationMethods.CrossSigningEnabled -> {
                                        if (selfVerificationMethods.methods.isNotEmpty()) {
                                            log.debug { "start self verification for $userId" }
                                            selfVerificationRouter.showSelfVerification(userId)
                                        } else {
                                            log.debug { "no self verification methods available for $userId" }
                                            selfVerificationRouter.closeSelfVerification(userId)
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    private fun onOpenSelfVerification(userId: UserId) {
        selfVerificationRouter.showSelfVerification(userId)
    }

    private fun startActiveVerificationsQueue() {
        coroutineScope.launch {
            activeVerifications.collect { currentActiveVerifications ->
                log.trace { "current active verifications: $currentActiveVerifications" }
                currentActiveVerifications.firstOrNull()?.let { userId ->
                    log.debug { "active verification in account $userId" }
                    verificationRouter.startDeviceVerification(userId)
                    deviceVerificationRouterStack.toFlow().first { childStack ->
                        childStack.active.configuration == VerificationRouter.Config.None
                    }.let {
                        activeVerifications.value -= userId
                    }
                }
            }
        }
    }


    private fun reactToActiveVerifications() {
        coroutineScope.launch {
            this@MainViewModelImpl.matrixClients.scopedCollectLatest { namedMatrixClients ->
                namedMatrixClients.forEach { (userId, matrixClient) ->
                    launch {
                        matrixClient.verification.activeDeviceVerification
                            .filterNotNull()
                            .collect {
                                log.debug { "new verification: $it" }
                                launch {
                                    it.state.collect { verificationState ->
                                        if (verificationState is ActiveVerificationState.TheirRequest ||
                                            verificationState is ActiveVerificationState.OwnRequest
                                        ) {
                                            activeVerifications.value += userId
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    private fun reactToPresenceIsPublicChanges() {
        coroutineScope.launch {
            this@MainViewModelImpl.matrixClients.scopedCollectLatest { namedMatrixClients ->
                namedMatrixClients.forEach { (userId, matrixClient) ->
                    launch {
                        messengerSettings[userId].filterNotNull().map { it.base.presenceIsPublic }
                            .collect { presenceIsPublic ->
                                if (presenceIsPublic && lifecycle.state >= Lifecycle.State.STARTED) {
                                    log.info { "the settings for `presenceIsPublic` have changed -> restart sync with ONLINE" }
                                    matrixClient.stopSync()
                                    matrixClient.startSync(presence = Presence.ONLINE)
                                } else if (presenceIsPublic.not() && lifecycle.state >= Lifecycle.State.STARTED) {
                                    log.info { "the settings for `presenceIsPublic` have changed -> restart sync with OFFLINE" }
                                    matrixClient.stopSync()
                                    matrixClient.startSync(presence = Presence.OFFLINE)
                                }
                            }
                    }
                }
            }
        }
    }

    private fun startLoginWizard() {
        log.debug{"Starting login wizard for user ${messengerSettings.value.base.selectedAccount}"}
        coroutineScope.launch {
            settingsWizardRouter.showCurrentWizardStep()
        }
    }

    override fun closeDetailsAndShowList() {
        coroutineScope.launch {
            roomListRouter.show()
            roomRouter.closeRoom()
        }
    }

    private fun closeRoom() {
        log.debug { "Closing the room as account has been switched.." }
        coroutineScope.launch {
            roomRouter.closeRoom()
        }
    }

    override fun onRoomSelected(userId: UserId, id: RoomId) {
        coroutineScope.launch {
            log.debug { "onRoomSelected: $id" }
            roomRouter.showRoom(userId, id)
            // hack for iOS, since the observe mechanism of line 236ff does not work
            selectedRoomId.value = id

            if (isSinglePane.value) {
                roomListRouter.moveToBackStack()
            } else {
                roomListRouter.show()
            }
        }
    }

    override fun onOpenAvatarCutter(userId: UserId, selectedRoomId: RoomId, file: FileDescriptor) {
        coroutineScope.launch {
            log.debug { "open avatar cutter" }
            avatarCutterRouter.show(userId, selectedRoomId, file)
        }
    }

    override fun onOpenAvatarCutter(userId: UserId, file: FileDescriptor) {
        coroutineScope.launch {
            log.debug { "open avatar cutter" }
            avatarCutterRouter.show(userId, file)
        }
    }

    override fun setSinglePane(isSinglePane: Boolean) {
        log.debug { "set single pane: $isSinglePane" }
        isBackButtonVisible.value = isSinglePane

        if (isSinglePane != this.isSinglePane.value) {
            this.isSinglePane.value = isSinglePane
            coroutineScope.launch {
                if (isSinglePane) {
                    switchToSinglePane()
                } else {
                    switchToMultiPane()
                }
            }
        }
    }

    override fun closeAccountsOverview() {
        roomListRouter.closeAccountsOverview()
    }

    private suspend fun switchToMultiPane() {
        roomListRouter.show()
    }

    private suspend fun switchToSinglePane() {
        if (roomRouter.isShown()) {
            roomListRouter.moveToBackStack()
        } else {
            roomListRouter.show()
        }
    }

    override fun openModal(
        type: OpenModalType,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        userId: UserId
    ) {
        when (type) {
            OpenModalType.IMAGE -> coroutineScope.launch {
                imageRouter.openImage(
                    mxcUrl,
                    encryptedFile,
                    fileName,
                    userId
                )
            }

            OpenModalType.VIDEO -> coroutineScope.launch {
                videoRouter.openVideo(
                    mxcUrl,
                    encryptedFile,
                    fileName,
                    userId
                )
            }
        }

    }

    override fun openMention(userId: UserId, messageMention: MessageMention) {
        when (messageMention) {
            is MessageMention.User -> {
                val user = messageMention.user.userId
                // TODO: implement and open user view (profile)
                log.warn { "UserView to display $user not implemented yet" }
            }

            is MessageMention.Room -> {
                log.debug { "Opening Room ${messageMention.room.roomId}" }
                val roomId = messageMention.room.roomId
                onRoomSelected(userId, roomId)
            }

            is MessageMention.Event -> {
                val eventId = messageMention.event.eventId
                // TODO: implement and open event view
                log.warn { "EventView to display $eventId not implemented yet" }
            }
        }
    }

    private fun RoomRouter.Config.getRoomId(): RoomId? =
        when (this) {
            is RoomRouter.Config.None -> null
            is RoomRouter.Config.View -> RoomId(roomId)
        }

    private fun onSendLogs() {
        coroutineScope.launch {
            val sendLogToDevs = getOrNull<SendLogToDevs>()
            if (sendLogToDevs != null)
                try {
                    val config = get<MatrixMessengerBaseConfiguration>()
                    val defaultDeviceDisplayName = get<GetDefaultDeviceDisplayName>()()
                    log.debug { "send logs to devs (email: ${config.sendLogsEmailAddress})" }
                    config.sendLogsEmailAddress?.let { email ->
                        sendLogToDevs(
                            email,
                            // TODO include version of trixnity-messenger or maybe move sendLogs to client
                            "error report for ${config.appName} (${defaultDeviceDisplayName})",
                        )
                    }
                } catch (exc: Exception) {
                    log.error(exc) { "Cannot send error report." }
                }
            else log.warn { "send log to devs is not supported on this platform" }
        }
    }
}

class PreviewMainViewModel : MainViewModel {
    override val selectedRoomId: MutableStateFlow<RoomId?> = MutableStateFlow(null)
    override val isBackButtonVisible: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isSinglePane: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val initialSyncStack: Value<ChildStack<InitialSyncRouter.Config, InitialSyncRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = InitialSyncRouter.Config.None,
                    instance = InitialSyncRouter.Wrapper.None,
                )
            )
        )
    override val selfVerificationStack: Value<ChildStack<SelfVerificationRouter.Config, SelfVerificationRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = SelfVerificationRouter.Config.None,
                    instance = SelfVerificationRouter.Wrapper.None
                )
            )
        )
    override val roomListRouterStack: Value<ChildStack<RoomListRouter.Config, RoomListRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = RoomListRouter.Config.RoomList,
                    instance = RoomListRouter.Wrapper.List(PreviewRoomListViewModel()),
                )
            )
        )
    override val roomRouterStack: Value<ChildStack<RoomRouter.Config, RoomRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = RoomRouter.Config.None,
                    instance = RoomRouter.Wrapper.View(PreviewRoomViewModel()),
                )
            )
        )
    override val imageRouterStack: Value<ChildStack<ImageRouter.Config, ImageRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = ImageRouter.Config.None,
                    instance = ImageRouter.Wrapper.None,
                )
            )
        )
    override val videoRouterStack: Value<ChildStack<VideoRouter.Config, VideoRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = VideoRouter.Config.None,
                    instance = VideoRouter.Wrapper.None,
                )
            )
        )
    override val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = VerificationRouter.Config.None,
                    instance = VerificationRouter.Wrapper.None,
                )
            )
        )
    override val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = AvatarCutterRouter.Config.None,
                    instance = AvatarCutterRouter.Wrapper.None,
                )
            )
        )
    override val settingsWizardRouterStack : Value<ChildStack<SettingsWizardRouter.Config, SettingsWizardRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = SettingsWizardRouter.Config.None,
                    instance = SettingsWizardRouter.Wrapper.None
                )
            )
        )

    override val showRoom: StateFlow<Boolean> = MutableStateFlow(false)

    override fun start() {
    }

    override fun closeDetailsAndShowList() {
    }

    override fun onRoomSelected(userId: UserId, id: RoomId) {
        selectedRoomId.value = id
    }

    override fun onOpenAvatarCutter(userId: UserId, file: FileDescriptor) {
    }

    override fun onOpenAvatarCutter(userId: UserId, selectedRoomId: RoomId, file: FileDescriptor) {
    }

    override fun setSinglePane(isSinglePane: Boolean) {
        this.isSinglePane.value = isSinglePane
    }

    override fun openModal(
        type: OpenModalType,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        userId: UserId
    ) {
    }

    override fun openMention(userId: UserId, messageMention: MessageMention) {
    }

    override fun closeAccountsOverview() {
    }
}
