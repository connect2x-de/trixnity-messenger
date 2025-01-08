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
import de.connect2x.trixnity.messenger.util.MinimizeApp
import de.connect2x.trixnity.messenger.util.SendLogToDevs
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSyncRouter
import de.connect2x.trixnity.messenger.viewmodel.room.PreviewRoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouterImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewRoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupRouter
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterRouter
import de.connect2x.trixnity.messenger.viewmodel.sharing.SharingRouter
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
    val isRoomShown: StateFlow<Boolean>
    val initialSyncStack: Value<ChildStack<InitialSyncRouter.Config, InitialSyncRouter.Wrapper>>
    val selfVerificationStack: Value<ChildStack<SelfVerificationRouter.Config, SelfVerificationRouter.Wrapper>>
    val roomListRouterStack: Value<ChildStack<RoomListRouter.Config, RoomListRouter.Wrapper>>
    val roomRouterStack: Value<ChildStack<RoomRouter.Config, RoomRouter.Wrapper>>
    val deviceVerificationRouterStack: Value<ChildStack<VerificationRouter.Config, VerificationRouter.Wrapper>>
    val avatarCutterRouterStack: Value<ChildStack<AvatarCutterRouter.Config, AvatarCutterRouter.Wrapper>>
    val accountSetupRouterStack: Value<ChildStack<AccountSetupRouter.Config, AccountSetupRouter.Wrapper>>
    val sharingStack: Value<ChildStack<SharingRouter.Config, SharingRouter.Wrapper>>

    /**
     * ATTENTION: The viewmodel has to be explicitly started as the routers cannot be initialized in the init block!
     */
    fun start()
    fun closeDetailsAndShowList()
    fun onRoomSelected(userId: UserId, id: RoomId)
    fun onOpenAvatarCutter(userId: UserId, file: FileDescriptor)
    fun onOpenAvatarCutter(userId: UserId, selectedRoomId: RoomId, file: FileDescriptor)
    fun openMention(userId: UserId, timelineElementMention: TimelineElementMention)
    fun closeAccountsOverview()
}

open class MainViewModelImpl(
    viewModelContext: ViewModelContext,
    onCreateNewAccount: () -> Unit,
    private val onRemoveAccount: (UserId) -> Unit,
) : ViewModelContext by viewModelContext, MainViewModel {

    // In case of multiple active verifications, these need to be processed in consecutive order one at a time!
    private val activeVerifications = MutableStateFlow(setOf<UserId>())
    private val messengerSettings by inject<MatrixMessengerSettingsHolder>()

    override val selectedRoomId = MutableStateFlow<RoomId?>(null)
    override val isRoomShown = MutableStateFlow(false)

    internal val selfVerificationRouter = SelfVerificationRouter(viewModelContext)
    override val selfVerificationStack: Value<ChildStack<SelfVerificationRouter.Config, SelfVerificationRouter.Wrapper>> =
        selfVerificationRouter.stack

    internal val sharingRouter = SharingRouter(viewModelContext)
    override val sharingStack: Value<ChildStack<SharingRouter.Config, SharingRouter.Wrapper>> =
        sharingRouter.stack


    private val backCallback = BackCallback {
        backPressHandler()
    }

    init { // Init before routers, so those can register other handlers that are executed beforehand.
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
            onAccountSelected = ::onAccountSelected,
            onStartAccountSetup = ::startAccountSetup
        )
    override val roomListRouterStack: Value<ChildStack<RoomListRouter.Config, RoomListRouter.Wrapper>> =
        roomListRouter.stack

    private val roomRouter: RoomRouter =
        RoomRouterImpl(
            viewModelContext = viewModelContext,
            onCloseRoom = ::closeDetailsAndShowList,
            onOpenMention = ::openMention,
            onOpenAvatarCutter = ::onOpenAvatarCutter,
        )
    override val roomRouterStack: Value<ChildStack<RoomRouter.Config, RoomRouter.Wrapper>> = roomRouter.stack

    private val selfVerificationTrigger = get<SelfVerificationTrigger>()

    init {
        coroutineScope.launch {
            roomRouterStack.subscribe {
                isRoomShown.value = it.active.instance !is RoomRouter.Wrapper.None
            }
            selfVerificationTrigger.onInvoke.collect {
                log.debug { "triggered self verification for user: $it" }
                onOpenSelfVerification(it)
            }
        }
    }

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

    private val accountSetupRouter: AccountSetupRouter =
        AccountSetupRouter(
            viewModelContext,
            onStartCrossSigningBootstrap = ::showCrossSigningBootstrap,
            onCloseCrossDeviceVerification = verificationRouter::closeVerification
        )

    override val accountSetupRouterStack: Value<ChildStack<AccountSetupRouter.Config, AccountSetupRouter.Wrapper>> =
        accountSetupRouter.stack

    private fun showCrossSigningBootstrap(userId: UserId) {
        coroutineScope.launch {
            selfVerificationRouter.showCrossSigningBootstrap(userId)
        }
    }

    private fun backPressHandler() {
        if (roomRouter.isShown()) {
            closeDetailsAndShowList()
        } else {
            getOrNull<MinimizeApp>()?.invoke()
            // TODO: was "minimize", but we should use native routing without all the back press handlers
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
                avatarCutterRouter.close()
                initialSyncRouter.close()
                verificationRouter.closeVerification()
                log.debug { "finished closing all navigation" }
            }
        }
    }

    // ATTENTION: The viewmodel has to be explicitly started as the routers cannot be initialized in the init block!
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
        possiblyStartAccountSetup()
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
                withContext(NonCancellable) { // Even when the scope is destroyed, we want the sync to stop.
                    log.debug { "app is stopped: cancel sync" }
                    this@MainViewModelImpl.matrixClients.value.forEach { (userId, matrixClient) ->
                        log.debug { "stop sync for $userId" }
                        matrixClient.cancelSync(wait = false)
                    }
                }
            }
            lifecycle.doOnStart(isOneTime = true) { // Only when the app was stopped, we want to (re-)start the sync.
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
                                        selfVerificationRouter.showCrossSigningBootstrap(userId)
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

    private fun possiblyStartAccountSetup() {
        coroutineScope.launch {
            matrixClients.scopedCollectLatest { clients ->
                clients.forEach {
                    if (messengerSettings.value.base.accounts[it.key]?.base?.accountSetupFinished == false) {
                        startAccountSetup(it.key)
                    }
                }
            }
        }
    }

    private fun startAccountSetup(userId: UserId) {
        accountSetupRouter.startSetup(userId)
    }

    override fun closeDetailsAndShowList() {
        coroutineScope.launch {
            roomListRouter.show()
            roomRouter.closeRoom()
        }
    }

    private fun onAccountSelected() {
        closeRoom()
        possiblyStartAccountSetup()
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
            // TODO: What hack exactly? Comment might be outdated!
            // Hack for iOS: Since the observe mechanism of line 236ff does not work.
            selectedRoomId.value = id
            roomListRouter.show()
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

    override fun closeAccountsOverview() {
        roomListRouter.closeAccountsOverview()
    }

    override fun openMention(userId: UserId, timelineElementMention: TimelineElementMention) {
        when (timelineElementMention) {
            is TimelineElementMention.User -> {
                val user = timelineElementMention.user.userId
                // TODO: implement and open user view (profile)
                log.warn { "UserView to display $user not implemented yet" }
            }

            is TimelineElementMention.Room -> {
                log.debug { "Opening Room ${timelineElementMention.room.roomId}" }
                val roomId = timelineElementMention.room.roomId
                onRoomSelected(userId, roomId)
            }

            is TimelineElementMention.Event -> {
                val eventId = timelineElementMention.event.eventId
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
                    instance = SelfVerificationRouter.Wrapper.None,
                )
            )
        )
    override val sharingStack: Value<ChildStack<SharingRouter.Config, SharingRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = SharingRouter.Config.None,
                    instance = SharingRouter.Wrapper.None,
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
    override val accountSetupRouterStack: Value<ChildStack<AccountSetupRouter.Config, AccountSetupRouter.Wrapper>> =
        MutableValue(
            ChildStack(
                active = Child.Created(
                    configuration = AccountSetupRouter.Config.None,
                    instance = AccountSetupRouter.Wrapper.None,
                )
            )
        )
    override val isRoomShown: StateFlow<Boolean> = MutableStateFlow(false)
    override fun onRoomSelected(userId: UserId, id: RoomId) {
        selectedRoomId.value = id
    }

    override fun start() {}
    override fun closeDetailsAndShowList() {}
    override fun onOpenAvatarCutter(userId: UserId, file: FileDescriptor) {}
    override fun onOpenAvatarCutter(userId: UserId, selectedRoomId: RoomId, file: FileDescriptor) {}
    override fun openMention(userId: UserId, timelineElementMention: TimelineElementMention) {}
    override fun closeAccountsOverview() {}
}
