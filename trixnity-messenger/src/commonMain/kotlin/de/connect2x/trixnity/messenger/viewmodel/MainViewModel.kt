package de.connect2x.trixnity.messenger.viewmodel

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.observe
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel.SelfVerificationConfig
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel.SelfVerificationWrapper
import de.connect2x.trixnity.messenger.viewmodel.files.ImageRouter
import de.connect2x.trixnity.messenger.viewmodel.files.ImageRouter.ImageConfig
import de.connect2x.trixnity.messenger.viewmodel.files.ImageRouter.ImageWrapper
import de.connect2x.trixnity.messenger.viewmodel.files.VideoRouter
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter.InitialSyncConfig
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter.InitialSyncWrapper
import de.connect2x.trixnity.messenger.viewmodel.room.PreviewRoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.RoomConfig
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter.RoomWrapper
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.OpenModalType
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewRoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter.RoomListConfig
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter.RoomListWrapper
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterRouter
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import de.connect2x.trixnity.messenger.viewmodel.verification.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.*
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
    val initialSyncStack: Value<ChildStack<InitialSyncConfig, InitialSyncWrapper>>
    val selfVerificationStack: Value<ChildStack<SelfVerificationConfig, SelfVerificationWrapper>>
    val roomListRouterStack: Value<ChildStack<RoomListConfig, RoomListWrapper>>
    val roomRouterStack: Value<ChildStack<RoomConfig, RoomWrapper>>
    val imageRouterStack: Value<ChildStack<ImageConfig, ImageWrapper>>
    val videoRouterStack: Value<ChildStack<VideoRouter.VideoConfig, VideoRouter.VideoWrapper>>
    val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.VerificationWrapper>>
    val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.AvatarCutterWrapper>>
    val showRoom: StateFlow<Boolean>

    // ATTENTION: the viewmodel has to be explicitly started as the routers cannot be not initialized in the init block
    fun start()
    fun closeDetailsAndShowList()
    fun onRoomSelected(userId: UserId, id: RoomId)
    fun onOpenAvatarCutter(userId: UserId, file: FileDescriptor)
    fun setSinglePane(isSinglePane: Boolean)
    fun openModal(
        type: OpenModalType,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        userId: UserId
    )

    fun closeAccountsOverview()

    sealed class SelfVerificationWrapper {
        data object None : SelfVerificationWrapper()
        class View(val selfVerificationViewModel: SelfVerificationViewModel) :
            SelfVerificationWrapper()

        class RedoSelfVerification(val redoSelfVerificationViewModel: RedoSelfVerificationViewModel) :
            SelfVerificationWrapper()

        class Bootstrap(val bootstrapViewModel: BootstrapViewModel) : SelfVerificationWrapper()
    }

    @Serializable
    sealed class SelfVerificationConfig {
        @Serializable
        data object None : SelfVerificationConfig()

        @Serializable
        data class SelfVerification(val userId: UserId) : SelfVerificationConfig()

        @Serializable
        data class RedoSelfVerification(val userId: UserId) : SelfVerificationConfig()

        @Serializable
        data class Bootstrap(val userId: UserId) : SelfVerificationConfig()
    }
}

open class MainViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (UserId) -> Unit,
) : ViewModelContext by viewModelContext, MainViewModel {

    private val messengerSettings by inject<MatrixMessengerSettingsHolder>()
    private val bootstrapStarted = MutableStateFlow(false)
    private val selfVerifications =
        MutableStateFlow(setOf<UserId>()) // in case of multiple self verifications, we need to do one after another
    private val activeVerifications =
        MutableStateFlow(setOf<UserId>()) // in case of multiple active verifications, we need to do them one after another

    override val selectedRoomId = MutableStateFlow<RoomId?>(null)
    override val isBackButtonVisible = MutableStateFlow(true)
    override val isSinglePane = MutableStateFlow(false)
    override val showRoom = MutableStateFlow(false)

    private val selfVerificationNavigation = StackNavigation<SelfVerificationConfig>()
    override val selfVerificationStack = childStack(
        source = selfVerificationNavigation,
        serializer = SelfVerificationConfig.serializer(),
        initialConfiguration = SelfVerificationConfig.None,
        handleBackButton = false,
        childFactory = ::createSelfVerificationChild
    )

    private fun createSelfVerificationChild(
        selfVerificationConfig: SelfVerificationConfig,
        componentContext: ComponentContext
    ): SelfVerificationWrapper =
        when (selfVerificationConfig) {
            is SelfVerificationConfig.None -> SelfVerificationWrapper.None
            is SelfVerificationConfig.SelfVerification -> {
                SelfVerificationWrapper.View(
                    get<SelfVerificationViewModelFactory>()
                        .create(
                            viewModelContext = childContext(componentContext, selfVerificationConfig.userId),
                            onClose = { closeSelfVerification(selfVerificationConfig.userId) },
                        )
                )
            }

            is SelfVerificationConfig.RedoSelfVerification -> SelfVerificationWrapper.RedoSelfVerification(
                get<RedoSelfVerificationViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext, selfVerificationConfig.userId),
                        onStartSelfVerification = { showSelfVerification(selfVerificationConfig.userId) },
                        onClose = { closeSelfVerification(selfVerificationConfig.userId) },
                    )
            )

            is SelfVerificationConfig.Bootstrap -> {
                SelfVerificationWrapper.Bootstrap(
                    get<BootstrapViewModelFactory>().create(
                        viewModelContext = childContext(componentContext, selfVerificationConfig.userId),
                        onClose = ::closeBootstrap,
                    )
                )
            }
        }

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
        )
    override val roomListRouterStack: Value<ChildStack<RoomListConfig, RoomListWrapper>> = roomListRouter.stack

    private val roomRouter: RoomRouter =
        RoomRouterImpl(
            viewModelContext = viewModelContext,
            isBackButtonVisible = isBackButtonVisible,
            onCloseRoom = ::closeDetailsAndShowList,
            onOpenModal = ::openModal,
        )
    override val roomRouterStack: Value<ChildStack<RoomConfig, RoomWrapper>> = roomRouter.roomStack

    init {
        coroutineScope.launch {
            roomRouterStack.subscribe {
                showRoom.value = it.active.instance !is RoomWrapper.None
            }
        }
    }

    private val imageRouter: ImageRouter = ImageRouter(viewModelContext = viewModelContext)
    override val imageRouterStack: Value<ChildStack<ImageConfig, ImageWrapper>> = imageRouter.stack

    private val videoRouter: VideoRouter = VideoRouter(viewModelContext = viewModelContext)
    override val videoRouterStack: Value<ChildStack<VideoRouter.VideoConfig, VideoRouter.VideoWrapper>> =
        videoRouter.stack

    private val verificationRouter: VerificationRouter =
        VerificationRouter(
            viewModelContext = viewModelContext,
            onRedoSelfVerification = ::redoSelfVerification
        )
    override val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.VerificationWrapper>> =
        verificationRouter.stack

    private val avatarCutterRouter: AvatarCutterRouter = AvatarCutterRouter(viewModelContext = viewModelContext)
    override val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.AvatarCutterWrapper>> =
        avatarCutterRouter.stack

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
            if (messengerSettings.value.accounts.isEmpty()) {
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
        roomRouter.roomStack.observe(lifecycle) { routerStack: ChildStack<RoomConfig, RoomWrapper> ->
            log.debug { "roomRouter has changed: ${routerStack.active.configuration::class.simpleName} (roomId: ${routerStack.active.configuration.getRoomId()})" }
            selectedRoomId.value = routerStack.active.configuration.getRoomId()
        }

        startSync()
        startSelfVerificationsQueue()
        startActiveVerificationsQueue()
        possiblyStartSelfVerification()
        reactToActiveVerifications()
        reactToPresenceIsPublicChanges()
    }

    private fun startSync() {
        coroutineScope.launch {
            initialSyncRouter.stack.toFlow().collect { childStack ->
                if (childStack.active.configuration == InitialSyncConfig.None) {
                    log.info { "initial sync / small sync is done -> now sync regularly" }
                    this@MainViewModelImpl.matrixClients.value.forEach { (userId, matrixClient) ->
                        val presenceIsPublic = messengerSettings[userId].first()?.presenceIsPublic
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
                        val presenceIsPublic = messengerSettings[userId].first()?.presenceIsPublic
                        log.debug { "start sync for $userId, presence is public: $presenceIsPublic" }
                        matrixClient.startSync(
                            presence = if (presenceIsPublic == true) Presence.ONLINE else Presence.OFFLINE
                        )
                    }
                }
            }
        }
    }

    /** Continually checks for new self verifications in a queue and executes them sequentially. */
    // Changed to an unidirectional flow:
    //
    // To start a verification flow, add an accountName to selfVerifications
    // This method will pick that change up and navigate accordingly
    //
    // To close a verification flow, remove the accountName from selfVerifications
    // If there are still pending verifications, this method will navigate to that flow,
    // otherwise it'll close the self verification flow entirely.
    private fun startSelfVerificationsQueue() {
        coroutineScope.launch {
            selfVerifications.collect { currentSelfVerifications ->
                log.trace { "current self verifications: $currentSelfVerifications" }
                val nextAccountToVerify = currentSelfVerifications.firstOrNull()
                if (nextAccountToVerify != null) {
                    selfVerificationNavigation.replaceCurrentSuspending(
                        SelfVerificationConfig.SelfVerification(nextAccountToVerify)
                    )
                } else {
                    // Queue is empty, close all verifications
                    if (selfVerificationStack.backStack.any { it.configuration is SelfVerificationConfig.None }) {
                        selfVerificationNavigation.popWhileSuspending { it !is SelfVerificationConfig.None }
                    } else {
                        selfVerificationNavigation.replaceCurrentSuspending(SelfVerificationConfig.None)
                    }
                }
            }
        }
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

    private fun possiblyStartSelfVerification() {
        coroutineScope.launch {
            this@MainViewModelImpl.matrixClients.scopedCollectLatest { namedMatrixClients ->
                namedMatrixClients.forEach { (userId, matrixClient) ->
                    log.debug { "launch listen for self verification methods (account $userId)" }
                    launch {
                        matrixClient.verification.getSelfVerificationMethods()
                            .distinctUntilChanged()
                            .collect { selfVerificationMethods ->
                                log.debug { "self verification methods (account $userId): $selfVerificationMethods" }
                                when (selfVerificationMethods) {
                                    is PreconditionsNotMet -> {
                                        log.debug { "cannot determine yet if cross-signing is needed for $userId" }
                                    }

                                    is NoCrossSigningEnabled -> {
                                        log.debug { "start bootstrapping $userId" }
                                        showBootstrap(userId)
                                    }

                                    is AlreadyCrossSigned -> {
                                        log.debug { "client for $userId is already cross-signed" }
                                        closeSelfVerification(userId)
                                    }

                                    is CrossSigningEnabled -> {
                                        if (selfVerificationMethods.methods.isNotEmpty()) {
                                            log.debug { "start self verification for $userId" }
                                            showSelfVerification(userId)
                                        } else {
                                            log.debug { "no self verification methods available for $userId" }
                                            closeSelfVerification(userId)
                                        }
                                    }
                                }
                            }
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
                        messengerSettings[userId].filterNotNull().map { it.presenceIsPublic }
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

    override fun closeDetailsAndShowList() {
        coroutineScope.launch {
            roomListRouter.show()
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

    private fun redoSelfVerification(userId: UserId) {
        selfVerificationNavigation.launchPush(coroutineScope, SelfVerificationConfig.RedoSelfVerification(userId))
    }

    /** @see startSelfVerificationsQueue() **/
    private fun showSelfVerification(userId: UserId) {
        log.debug { "add account to self verification queue: $userId" }
        // do sequentially (for different accounts), so here just fill the list
        selfVerifications.value += userId
    }

    internal fun closeSelfVerification(userId: UserId) {
        log.debug { "remove account from self verification queue: $userId" }
        selfVerifications.value -= userId
    }

    private suspend fun showBootstrap(userId: UserId) {
        // it can happen that the bootstrap is triggered twice (initial sync, then regular sync; to avoid any
        // complications, only allow one bootstrap to be shown at the time
        if (bootstrapStarted.value.not()) {
            log.debug { "show bootstrap view" }
            bootstrapStarted.value = true
            selfVerificationNavigation.pushSuspending(SelfVerificationConfig.Bootstrap(userId))
        }
    }

    private fun closeBootstrap() = coroutineScope.launch {
        log.debug { "close bootstrap view" }
        bootstrapStarted.value = false
        selfVerificationNavigation.popSuspending(onComplete = { log.debug { "close bootstrap completed: $it" } })
    }

    private fun RoomConfig.getRoomId(): RoomId? =
        when (this) {
            is RoomConfig.None -> null
            is RoomConfig.View -> RoomId(roomId)
        }

    private fun onSendLogs() {
        coroutineScope.launch {
            val sendLogToDevs = getOrNull<SendLogToDevs>()
            if (sendLogToDevs != null)
                try {
                    val config = get<MatrixMessengerConfiguration>()
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
    override val initialSyncStack: Value<ChildStack<InitialSyncConfig, InitialSyncWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = InitialSyncConfig.None,
                    instance = InitialSyncWrapper.None,
                )
            )
        )
    override val selfVerificationStack: Value<ChildStack<SelfVerificationConfig, SelfVerificationWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = SelfVerificationConfig.None,
                    instance = SelfVerificationWrapper.None
                )
            )
        )
    override val roomListRouterStack: Value<ChildStack<RoomListConfig, RoomListWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = RoomListConfig.RoomList,
                    instance = RoomListWrapper.List(PreviewRoomListViewModel()),
                )
            )
        )
    override val roomRouterStack: Value<ChildStack<RoomConfig, RoomWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = RoomConfig.None,
                    instance = RoomWrapper.View(PreviewRoomViewModel()),
                )
            )
        )
    override val imageRouterStack: Value<ChildStack<ImageConfig, ImageWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = ImageConfig.None,
                    instance = ImageWrapper.None,
                )
            )
        )
    override val videoRouterStack: Value<ChildStack<VideoRouter.VideoConfig, VideoRouter.VideoWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = VideoRouter.VideoConfig.None,
                    instance = VideoRouter.VideoWrapper.None,
                )
            )
        )
    override val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.VerificationWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = VerificationRouter.Config.None,
                    instance = VerificationRouter.VerificationWrapper.None,
                )
            )
        )
    override val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.AvatarCutterWrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = AvatarCutterRouter.Config.None,
                    instance = AvatarCutterRouter.AvatarCutterWrapper.None,
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

    override fun closeAccountsOverview() {
    }
}