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
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.*
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
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncState
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
import de.connect2x.trixnity.messenger.viewmodel.util.runBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import de.connect2x.trixnity.messenger.viewmodel.util.toFlow
import de.connect2x.trixnity.messenger.viewmodel.verification.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.*
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptedFile
import org.koin.core.component.get

private val log = KotlinLogging.logger {}

interface MainViewModelFactory {
    fun newMainViewModel(
        viewModelContext: ViewModelContext,
        initialSyncOnceIsFinished: (Boolean) -> Unit,
        minimizeMessenger: () -> Unit,
        onCreateNewAccount: () -> Unit,
        onRemoveAccount: (String) -> Unit,
    ): MainViewModel {
        return MainViewModelImpl(
            viewModelContext,
            initialSyncOnceIsFinished,
            minimizeMessenger,
            onCreateNewAccount,
            onRemoveAccount,
        )
    }
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
    fun onRoomSelected(accountName: String, id: RoomId)
    fun onOpenAvatarCutter(file: String, accountName: String)
    fun setSinglePane(isSinglePane: Boolean)
    fun openModal(
        type: OpenModalType,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        accountName: String
    )

    fun closeAccountsOverview()

    fun selectFile(file: String)
    fun dragFile(file: String)
    fun dragFileExit()

    sealed class SelfVerificationWrapper {
        object None : SelfVerificationWrapper()
        class View(val selfVerificationViewModel: SelfVerificationViewModel) :
            SelfVerificationWrapper()

        class RedoSelfVerification(val redoSelfVerificationViewModel: RedoSelfVerificationViewModel) :
            SelfVerificationWrapper()

        class Bootstrap(val bootstrapViewModel: BootstrapViewModel) : SelfVerificationWrapper()
    }

    sealed class SelfVerificationConfig : Parcelable {
        @Parcelize
        object None : SelfVerificationConfig()

        @Parcelize
        data class SelfVerification(val accountName: String) : SelfVerificationConfig()

        @Parcelize
        data class RedoSelfVerification(val accountName: String) : SelfVerificationConfig()

        @Parcelize
        data class Bootstrap(val accountName: String) : SelfVerificationConfig()
    }
}

open class MainViewModelImpl(
    viewModelContext: ViewModelContext,
    private val initialSyncOnceIsFinished: (Boolean) -> Unit,
    private val minimizeMessenger: () -> Unit,
    private val onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (String) -> Unit,
) : ViewModelContext by viewModelContext, MainViewModel {

    private val bootstrapStarted = MutableStateFlow(false)
    private val selfVerifications =
        MutableStateFlow(listOf<String>()) // in case of multiple self verifications, we need to do one after another
    private val activeVerifications =
        MutableStateFlow(listOf<String>()) // in case of multiple active verifications, we need to do them one after another

    override val selectedRoomId = MutableStateFlow<RoomId?>(null)
    override val isBackButtonVisible = MutableStateFlow(true)
    override val isSinglePane = MutableStateFlow(false)
    override val showRoom = MutableStateFlow(false)

    private val selfVerificationNavigation = StackNavigation<SelfVerificationConfig>()
    override val selfVerificationStack = childStack(
        source = selfVerificationNavigation,
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
                        .newSelfVerificationViewModel(
                            viewModelContext = childContext(componentContext, selfVerificationConfig.accountName),
                            onClose = ::closeSelfVerification,
                        )
                )
            }

            is SelfVerificationConfig.RedoSelfVerification -> SelfVerificationWrapper.RedoSelfVerification(
                get<RedoSelfVerificationViewModelFactory>()
                    .newRedoSelfVerificationViewModel(
                        viewModelContext = childContext(componentContext, selfVerificationConfig.accountName),
                        onStartSelfVerification = { showSelfVerification(selfVerificationConfig.accountName) },
                        onClose = ::closeSelfVerification,
                    )
            )

            is SelfVerificationConfig.Bootstrap -> {
                SelfVerificationWrapper.Bootstrap(
                    get<BootstrapViewModelFactory>().newBootstrapViewModel(
                        viewModelContext = childContext(componentContext, selfVerificationConfig.accountName),
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
            minimizeMessenger()
        }
    }

    private fun onRemoveAccountInternal(accountName: String) {
        roomListRouter.closeAccountsOverview()
        this.onRemoveAccount(accountName)
        coroutineScope.launch {
            if (getKoin().get<GetAccountNames>()().isEmpty()) {
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
    }

    private fun startSync() {
        coroutineScope.launch {
            val syncForAccounts = namedMatrixClients.map { namedMatrixClients ->
                namedMatrixClients.mapNotNull { namedMatrixClient ->
                    namedMatrixClient.matrixClient.value?.let { matrixClient ->
                        log.debug { "start sync [${namedMatrixClient.accountName}] -> $matrixClient, initial sync done: ${matrixClient.initialSyncDone.value}" }
                        val initialSyncDone = matrixClient.initialSyncDone.first()
                        if (initialSyncDone) {
                            namedMatrixClient.accountName to InitialSyncState.DONE
                        } else {
                            namedMatrixClient.accountName to InitialSyncState.NOT_DONE
                        }
                    }
                }.toMap()
            }
            launch {
                syncForAccounts.collect {
                    initialSyncRouter.showSync(it)
                }
            }

            initialSyncRouter.stack.subscribe { childStack ->
                if (childStack.active.configuration == InitialSyncConfig.None) {
                    log.info { "initial sync / small sync is done -> now sync regularly" }
                    initialSyncOnceIsFinished(true)
                    launch {
                        namedMatrixClients.collectLatest { namedMatrixClients ->
                            namedMatrixClients.forEach { (accountName, matrixClientFlow) ->
                                log.debug { "start sync for $accountName" }
                                matrixClientFlow.value?.startSync()
                            }
                        }
                    }
                }
            }
        }
        lifecycle.doOnStop {
            runBlocking { // even when the scope is destroyed, we want the sync to stop
                log.debug { "app is stopped: cancel sync" }
                namedMatrixClients.value.forEach { (accountName, matrixClientFlow) ->
                    log.debug { "stop sync for $accountName" }
                    matrixClientFlow.value?.cancelSync(wait = false)
                }
            }

            // only when the app was stopped, we want to (re-)start the sync
            lifecycle.doOnStart(isOneTime = true) {
                coroutineScope.launch {
                    log.debug { "resume app: restart sync" }
                    namedMatrixClients.value.forEach { (accountName, matrixClientFlow) ->
                        log.debug { "start sync for $accountName" }
                        matrixClientFlow.value?.startSync()
                    }
                }
            }
        }
    }

    /** Continually checks for new self verifications in a queue and executes them sequentially. */
    private fun startSelfVerificationsQueue() {
        coroutineScope.launch {
            selfVerifications.collect { currentSelfVerifications ->
                log.trace { "current self verifications: $currentSelfVerifications" }
                currentSelfVerifications.firstOrNull()?.let { accountName ->
                    log.debug { "selfVerification for account $accountName" }
                    selfVerificationNavigation.replaceCurrentSuspending(
                        SelfVerificationConfig.SelfVerification(accountName)
                    )
                    selfVerificationStack.toFlow().first { childStack ->
                        childStack.active.configuration == SelfVerificationConfig.None
                    }.let {
                        selfVerifications.value -= accountName
                    }
                }
            }
        }
    }

    private fun startActiveVerificationsQueue() {
        coroutineScope.launch {
            activeVerifications.collect { currentActiveVerifications ->
                log.trace { "current active verifications: $currentActiveVerifications" }
                currentActiveVerifications.firstOrNull()?.let { accountName ->
                    log.debug { "active verification in account $accountName" }
                    verificationRouter.startDeviceVerification(accountName)
                    deviceVerificationRouterStack.toFlow().first { childStack ->
                        childStack.active.configuration == VerificationRouter.Config.None
                    }.let {
                        activeVerifications.value -= accountName
                    }
                }
            }
        }
    }

    private fun possiblyStartSelfVerification() {
        coroutineScope.launch {
            namedMatrixClients.scopedCollectLatest { namedMatrixClients ->
                namedMatrixClients.forEach { (accountName, matrixClientFlow) ->
                    val matrixClient = matrixClientFlow.value ?: throw IllegalStateException("")
                    log.debug { "launch listen for self verification methods (account $accountName)" }
                    launch {
                        matrixClient.verification.getSelfVerificationMethods()
                            .distinctUntilChanged()
                            .collect { selfVerificationMethods ->
                                log.debug { "self verification methods (account $accountName): $selfVerificationMethods" }
                                when (selfVerificationMethods) {
                                    is PreconditionsNotMet -> {
                                        log.debug { "cannot determine yet if cross-signing is needed" }
                                    }

                                    is NoCrossSigningEnabled -> {
                                        log.debug { "start bootstrapping" }
                                        showBootstrap(accountName)
                                    }

                                    is AlreadyCrossSigned -> {
                                        log.debug { "client is already cross-signed" }
                                    }

                                    is CrossSigningEnabled -> {
                                        if (selfVerificationMethods.methods.isNotEmpty()) {
                                            log.debug { "start self verification" }
                                            showSelfVerification(accountName)
                                        } else {
                                            closeSelfVerification()
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
            namedMatrixClients.scopedCollectLatest { namedMatrixClients ->
                namedMatrixClients.forEach { (accountName, matrixClient) ->
                    launch {
                        matrixClient.value?.verification?.activeDeviceVerification
                            ?.filterNotNull()
                            ?.collect {
                                log.debug { "new verification: $it" }
                                launch {
                                    it.state.collect { verificationState ->
                                        if (verificationState is ActiveVerificationState.TheirRequest ||
                                            verificationState is ActiveVerificationState.OwnRequest
                                        ) {
                                            activeVerifications.value += accountName
                                        }
                                    }
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

    override fun onRoomSelected(accountName: String, id: RoomId) {
        coroutineScope.launch {
            log.debug { "onRoomSelected: $id" }
            roomRouter.showRoom(accountName, id)
            // hack for iOS, since the observe mechanism of line 236ff does not work
            selectedRoomId.value = id

            if (isSinglePane.value) {
                roomListRouter.moveToBackStack()
            } else {
                roomListRouter.show()
            }
        }
    }

    override fun onOpenAvatarCutter(accountName: String, file: String) {
        coroutineScope.launch {
            log.debug { "open avatar cutter" }
            avatarCutterRouter.show(accountName, file)
        }
    }

    override fun setSinglePane(singlePane: Boolean) {
        log.debug { "set single pane: $singlePane" }
        isBackButtonVisible.value = singlePane

        if (singlePane != isSinglePane.value) {
            isSinglePane.value = singlePane
            coroutineScope.launch {
                if (singlePane) {
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

    override fun selectFile(file: String) {
        log.debug { "select file $file, selected roomId: ${selectedRoomId.value}" }
        if (selectedRoomId.value != null) {
            val instance = roomRouterStack.value.active.instance
            if (instance is RoomWrapper.View) {
                instance.roomViewModel.selectFile(file)
            }
        }
    }

    override fun dragFile(file: String) {
        log.debug { "drag file $file, selected roomId: ${selectedRoomId.value}" }
        if (selectedRoomId.value != null) {
            val instance = roomRouterStack.value.active.instance
            if (instance is RoomWrapper.View) {
                instance.roomViewModel.dragFile(file)
            }
        }
    }

    override fun dragFileExit() {
        log.debug { "drag file exit, selected roomId: ${selectedRoomId.value}" }
        if (selectedRoomId.value != null) {
            val instance = roomRouterStack.value.active.instance
            if (instance is RoomWrapper.View) {
                instance.roomViewModel.dragFileExit()
            }
        }
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
        accountName: String
    ) {
        when (type) {
            OpenModalType.IMAGE -> coroutineScope.launch {
                imageRouter.openImage(
                    mxcUrl,
                    encryptedFile,
                    fileName,
                    accountName
                )
            }

            OpenModalType.VIDEO -> coroutineScope.launch {
                videoRouter.openVideo(
                    mxcUrl,
                    encryptedFile,
                    fileName,
                    accountName
                )
            }
        }

    }

    private fun redoSelfVerification(accountName: String) {
        selfVerificationNavigation.launchPush(coroutineScope, SelfVerificationConfig.RedoSelfVerification(accountName))
    }

    /** @see startSelfVerificationsQueue() **/
    private fun showSelfVerification(accountName: String) {
        log.debug { "add account to self verification queue: $accountName" }
        // do sequentially (for different accounts), so here just fill the list
        selfVerifications.value += accountName
    }

    internal fun closeSelfVerification() {
        if (selfVerificationStack.backStack.any { it.configuration is SelfVerificationConfig.None }) {
            selfVerificationNavigation.launchPopWhile(coroutineScope) { it !is SelfVerificationConfig.None }
        } else {
            selfVerificationNavigation.launchReplaceCurrent(coroutineScope, SelfVerificationConfig.None)
        }
    }

    private suspend fun showBootstrap(accountName: String) {
        // it can happen that the bootstrap is triggered twice (initial sync, then regular sync; to avoid any
        // complications, only allow one bootstrap to be shown at the time
        if (bootstrapStarted.value.not()) {
            log.debug { "show bootstrap view" }
            bootstrapStarted.value = true
            selfVerificationNavigation.pushSuspending(SelfVerificationConfig.Bootstrap(accountName))
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
            try {
                log.debug { "send logs to devs (email: ${MessengerConfig.instance.sendLogsEmailAddress})" }
                MessengerConfig.instance.sendLogsEmailAddress?.let { email ->
                    sendLogToDevs(
                        email,
                        "error report for ${MessengerConfig.instance.appName} (version ${getVersion()}, ${deviceDisplayName()})",
                        getLogContent()
                    )
                }
            } catch (exc: Exception) {
                log.error(exc) { "Cannot send error report." }
            }
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

    override fun onRoomSelected(accountName: String, id: RoomId) {
        selectedRoomId.value = id
    }

    override fun onOpenAvatarCutter(file: String, accountName: String) {
    }

    override fun setSinglePane(isSinglePane: Boolean) {
        this.isSinglePane.value = isSinglePane
    }

    override fun openModal(
        type: OpenModalType,
        mxcUrl: String,
        encryptedFile: EncryptedFile?,
        fileName: String,
        accountName: String
    ) {
    }

    override fun closeAccountsOverview() {
    }

    override fun selectFile(file: String) {
    }

    override fun dragFile(file: String) {
    }

    override fun dragFileExit() {
    }

}