package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationCapabilities
import de.connect2x.sysnotify.NotificationHandle
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.NotificationIcon
import de.connect2x.sysnotify.NotificationPlatform
import de.connect2x.sysnotify.NotificationPriority
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.media.MediaService
import de.connect2x.trixnity.client.notification.NotificationService
import de.connect2x.trixnity.client.notification.NotificationUpdate
import de.connect2x.trixnity.client.store.RoomUser
import de.connect2x.trixnity.client.store.TimelineEvent
import de.connect2x.trixnity.client.user.UserService
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountNotificationSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.createTestDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.settle
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.InMemoryPlatformMedia
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import de.connect2x.trixnity.utils.toByteArrayFlow
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class NotificationSyncServiceTest {
    val matrixClientMock = mock<MatrixClient>()
    val notificationServiceMock = mock<NotificationService>()
    val mediaServiceMock = mock<MediaService>()
    val userServiceMock = mock<UserService>()
    val notificationHandler = FakeNotificationHandler()
    val user = UserId("test", "server")
    val roomUser = createRoomUser(user)

    val matrixClientMock2 = mock<MatrixClient>()
    val notificationServiceMock2 = mock<NotificationService>()
    val mediaServiceMock2 = mock<MediaService>()
    val userServiceMock2 = mock<UserService>()
    val notificationHandler2 = FakeNotificationHandler()
    val user2 = UserId("test2", "server2")
    val roomUser2 = createRoomUser(user2)

    val roomId = RoomId("!room456")
    val roomName = "room name"

    val settingsHolder = createTestMatrixMessengerSettingsHolder()
    val notificationHandlersMock = mock<NotificationHandlers>()
    val getNotificationIconMock = mock<GetNotificationIcon>()
    val config = MatrixMessengerConfiguration(appIcon = "app icon")

    val roomNameMock = mock<RoomName>()

    val i18n =
        object :
            I18n(
                DefaultLanguages,
                createTestMatrixMessengerSettingsHolder(),
                GetSystemLang { "en" },
                TimeZone.of("CET"),
            ) {}

    val message = "Hello"

    val messageEvent =
        ClientEvent.RoomEvent.MessageEvent(
            content = RoomMessageEventContent.TextBased.Text(message),
            id = EventId("event-id"),
            sender = user,
            roomId = roomId,
            originTimestamp = 0L,
        )
    val joinEvent =
        StateEvent(
            content = MemberEventContent(membership = Membership.JOIN),
            id = EventId("event-id"),
            sender = user,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = "",
        )
    val inviteEvent =
        StateEvent(
            content = MemberEventContent(membership = Membership.INVITE),
            id = EventId("event-id"),
            sender = user,
            roomId = roomId,
            originTimestamp = 0L,
            stateKey = user.full,
        )

    @BeforeTest
    fun setup() {
        configureTestLogging()
        resetMocks(
            matrixClientMock,
            notificationServiceMock,
            mediaServiceMock,
            userServiceMock,
            matrixClientMock2,
            notificationServiceMock2,
            mediaServiceMock2,
            userServiceMock2,
        )
        setupMatrixClientMock(
            matrixClientMock,
            user,
            roomUser,
            notificationServiceMock,
            userServiceMock,
            mediaServiceMock,
        )
        setupMatrixClientMock(
            matrixClientMock2,
            user2,
            roomUser2,
            notificationServiceMock2,
            userServiceMock2,
            mediaServiceMock2,
        )
        every { roomNameMock.getRoomName(any<RoomId>(), any(), any()) } returns flowOf(roomName)
        every { notificationHandlersMock[user] } returns notificationHandler
        every { notificationHandlersMock[user2] } returns notificationHandler2
        every { getNotificationIconMock.fromResource(any()) } returns NotificationIcon(ByteArray(0), 0, 0)
        every { getNotificationIconMock.fromBytes(any(), any(), any()) } returns NotificationIcon(ByteArray(0), 0, 0)
    }

    @Test
    fun `new notification » should push new notification`() = runTest {
        val tag = "tag123"
        val content = createNotificationUpdateContent(messageEvent)
        val notificationNew = createNotificationNew(tag, content)
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationNew)

        val cut = notificationSyncService()
        backgroundScope.launch { cut.doWork() }

        settle()
        val notification = notificationHandler.notifications.getValue(tag)
        with(notification) {
            title shouldContain roomName
            description.shouldContain(user.full).shouldContain(message)
            icon shouldNotBe null
            statusIcon shouldNotBe null
            callbackData shouldContain roomId.full.substring(1)
            playSound shouldBe true
        }
    }

    @Test
    fun `update notification » should update existing notification`() = runTest {
        val tag = "tag123"
        val content = createNotificationUpdateContent(messageEvent)
        val notificationUpdate = createNotificationUpdate(tag, content)
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationUpdate)
        notificationHandler.initialize(
            mapOf(
                tag to
                    Notification(
                        "old title",
                        description = "old description",
                        icon = null,
                        statusIcon = null,
                        callbackData = "old callback data",
                    )
            )
        )
        notificationHandler.notifications shouldHaveSize 1

        val cut = notificationSyncService()
        backgroundScope.launch { cut.doWork() }

        settle()
        notificationHandler.notifications shouldHaveSize 1
        val notification = notificationHandler.notifications.getValue(tag)
        with(notification) {
            title shouldContain roomName
            description.shouldContain(user.full).shouldContain(message)
            icon shouldNotBe null
            statusIcon shouldNotBe null
            callbackData shouldContain roomId.full.substring(1)
            playSound shouldBe false
        }
    }

    @Test
    fun `remove notification » should remove existing notification`() = runTest {
        val tag = "tag123"
        val notificationRemove = createNotificationRemove(tag)
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationRemove)
        notificationHandler.initialize(mapOf(tag to Notification()))
        notificationHandler.notifications shouldHaveSize 1

        val cut = notificationSyncService()
        backgroundScope.launch { cut.doWork() }

        settle()
        notificationHandler.notifications shouldHaveSize 0
    }

    @Test
    fun `state event » should send correct notification data for join`() = runTest {
        val tag = "tag123"
        val content = createNotificationUpdateContent(joinEvent)
        val notificationNew = createNotificationNew(tag, content)
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationNew)

        val cut = notificationSyncService()
        backgroundScope.launch { cut.doWork() }

        settle()
        val notification = notificationHandler.notifications.getValue(tag)
        with(notification) {
            title shouldContain roomName
            description shouldContain "activity"
            icon shouldNotBe null
            statusIcon shouldNotBe null
            callbackData shouldContain roomId.full.substring(1)
            playSound shouldBe true
        }
    }

    @Test
    fun `state event » should send correct notification data for invite`() = runTest {
        val tag = "tag123"
        val content = createNotificationUpdateContent(inviteEvent)
        val notificationNew = createNotificationNew(tag, content)
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationNew)

        val cut = notificationSyncService()
        backgroundScope.launch { cut.doWork() }

        settle()
        val notification = notificationHandler.notifications.getValue(tag)
        with(notification) {
            title shouldContain roomName
            description shouldContain "invite"
            icon shouldNotBe null
            statusIcon shouldNotBe null
            callbackData shouldContain roomId.full.substring(1)
            playSound shouldBe true
        }
    }

    @Test
    fun `details disabled » push new single-no-details-notification`() = runTest {
        val content = createNotificationUpdateContent(messageEvent)
        val notificationNew = createNotificationNew("some tag", content)
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationNew)

        val cut = notificationSyncService(showDetails = mapOf(user to false))
        backgroundScope.launch { cut.doWork() }

        settle()
        val notification = notificationHandler.notifications.getValue(NotificationSyncService.noDetailsTag)
        with(notification) {
            title shouldContain "new messages"
            description shouldContain "new messages"
            icon shouldBe null
            statusIcon shouldBe null
            callbackData shouldContain config.appUri
            callbackData shouldNotContain roomId.full.substring(1)
            playSound shouldBe true
        }
    }

    @Test
    fun `details disabled » replace existing single-no-details-notification`() = runTest {
        val content = createNotificationUpdateContent(messageEvent)
        val notificationUpdate = createNotificationUpdate("some other tag", content)
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationUpdate)
        notificationHandler.initialize(
            mapOf(
                NotificationSyncService.noDetailsTag to
                    Notification("old title", description = "old description", callbackData = "old callback data")
            )
        )
        notificationHandler.notifications shouldHaveSize 1

        val cut = notificationSyncService(showDetails = mapOf(user to false))
        backgroundScope.launch { cut.doWork() }

        settle()
        notificationHandler.notifications shouldHaveSize 1
        val notification = notificationHandler.notifications.getValue(NotificationSyncService.noDetailsTag)
        with(notification) {
            title shouldContain "new messages"
            description shouldContain "new messages"
            icon shouldBe null
            statusIcon shouldBe null
            callbackData shouldContain config.appUri
            callbackData shouldNotContain roomId.full.substring(1)
            playSound shouldBe true
        }
    }

    @Test
    fun `details disabled » remove single-no-details-notification if last notification`() = runTest {
        val notificationRemove = createNotificationRemove("placeholder")
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationRemove)
        notificationHandler.initialize(mapOf(NotificationSyncService.noDetailsTag to Notification()))
        notificationHandler.notifications shouldHaveSize 1
        every { notificationServiceMock.getCount() } returns flowOf(0)

        val cut = notificationSyncService(showDetails = mapOf(user to false))
        backgroundScope.launch { cut.doWork() }

        settle()
        notificationHandler.notifications shouldHaveSize 0
    }

    @Test
    fun `details disabled » don't remove single-no-details-notification if there are more`() = runTest {
        val notificationRemove = createNotificationRemove("placeholder")
        every { notificationServiceMock.getAllUpdates() } returns flowOf(notificationRemove)
        notificationHandler.initialize(mapOf(NotificationSyncService.noDetailsTag to Notification()))
        notificationHandler.notifications shouldHaveSize 1
        every { notificationServiceMock.getCount() } returns flowOf(2)

        val cut = notificationSyncService(showDetails = mapOf(user to false))
        backgroundScope.launch { cut.doWork() }

        settle()
        notificationHandler.notifications shouldHaveSize 1
    }

    /* FIXME uncomment if clearAll() is fixed for all platforms

    @Test
    fun `notifications disabled » clear all notifications for user`() = runTest {
        val content = createNotificationUpdateContent(messageEvent)
        val notificationNew = createNotificationNew("some tag", content)
        var flowStarted = false
        var flowFinished = false
        val updateFlow =
            flowOf(notificationNew)
                .onStart { flowStarted = true }
                .onCompletion { cause ->
                    if (cause == null) {
                        flowFinished = true
                    }
                }
        every { notificationServiceMock.getAllUpdates() } returns updateFlow
        val initialNotifications = (0..4).associate { it.toString() to Notification("notification $it", "wake up $it") }
        notificationHandler.initialize(initialNotifications)

        val cut = notificationSyncService(notificationsEnabled = mapOf(user to false))
        backgroundScope.launch { cut.doWork() }

        settle()
        notificationHandler.notifications shouldHaveSize 0
        flowStarted shouldBe true
        flowFinished shouldBe true
    }

    @Test
    fun `notifications disabled » clear notifications for user with notifications disabled only`() = runTest {
        val matrixClients = mapOf(user to matrixClientMock, user2 to matrixClientMock2)
        val notificationsEnabled = mapOf(user to true, user2 to false)
        val content = createNotificationUpdateContent(messageEvent)
        val notificationNew = createNotificationNew("some tag", content)
        var flowStarted = false
        var flowFinished = false
        var flowStarted2 = false
        var flowFinished2 = false
        val updateFlow =
            flowOf(notificationNew)
                .onStart { flowStarted = true }
                .onCompletion { cause ->
                    if (cause == null) {
                        flowFinished = true
                    }
                }
        val updateFlow2 =
            flowOf(notificationNew)
                .onStart { flowStarted2 = true }
                .onCompletion { cause ->
                    if (cause == null) {
                        flowFinished2 = true
                    }
                }
        every { notificationServiceMock.getAllUpdates() } returns updateFlow
        every { notificationServiceMock2.getAllUpdates() } returns updateFlow2
        val initialNotifications = (0..4).associate { it.toString() to Notification("notification $it", "wake up $it") }
        notificationHandler.initialize(initialNotifications.toMap())
        notificationHandler2.initialize(initialNotifications.toMap())

        val cut = notificationSyncService(matrixClients = matrixClients, notificationsEnabled = notificationsEnabled)
        backgroundScope.launch { cut.doWork() }

        settle()
        notificationHandler.notifications shouldHaveSize 6
        notificationHandler2.notifications shouldHaveSize 0
        flowStarted shouldBe true
        flowFinished shouldBe true
        flowStarted2 shouldBe true
        flowFinished2 shouldBe true
    }
    */

    private suspend fun TestScope.notificationSyncService(
        matrixClients: Map<UserId, MatrixClient> = mapOf(user to matrixClientMock),
        showDetails: Map<UserId, Boolean> = mapOf(user to true),
        notificationsEnabled: Map<UserId, Boolean> = mapOf(user to true),
    ): NotificationSyncService {
        val di =
            koinApplication { modules(createTestDefaultTrixnityMessengerModules(matrixClients, settingsHolder)) }.koin

        val notificationProvider = NoOpNotificationProvider(settings = settingsHolder, coroutineScope = backgroundScope)

        matrixClients.forEach {
            val user = it.key
            val showDetails = showDetails[user] ?: true

            settingsHolder.create(user, MatrixMessengerAccountSettingsBase())
            settingsHolder.update<MatrixMessengerAccountNotificationSettings>(user) {
                MatrixMessengerAccountNotificationSettings(playSound = true, showDetails = showDetails)
            }

            when (notificationsEnabled[user]) {
                true -> notificationProvider.enable(it.key)
                false -> notificationProvider.disable(it.key)
                null -> {}
            }
        }
        return NotificationSyncService(
            matrixClients = di.get<MatrixClients>(),
            notificationHandlersMock,
            notificationProviders =
                NotificationProviders(providers = emptyList(), getNoOpNotificationProvider = { notificationProvider }),
            config,
            settingsHolder,
            roomNameMock,
            getNotificationIconMock,
            i18n,
        )
    }

    private fun setupMatrixClientMock(
        matrixClient: MatrixClient = mock(),
        user: UserId = UserId("user"),
        roomUser: RoomUser = createRoomUser(user),
        notificationService: NotificationService = mock(),
        userService: UserService = mock(),
        mediaService: MediaService = mock(),
    ): MatrixClient {
        val di =
            koinApplication {
                    modules(
                        module {
                            single<NotificationService> { notificationService }
                            single<UserService> { userService }
                            single<MediaService> { mediaService }
                        }
                    )
                }
                .koin
        every { matrixClient.userId } returns user
        every { matrixClient.di } returns di
        every { userService.getById(any(), any()) } returns flowOf(roomUser)
        val avatar = InMemoryPlatformMedia("avatar".encodeToByteArray().toByteArrayFlow())
        everySuspend {
            mediaService.getThumbnail(any(), avatarSize().toLong(), avatarSize().toLong(), any(), any(), any())
        } returns Result.success(avatar)
        every { notificationService.getAllUpdates() } returns flowOf()
        every { notificationService.getCount() } returns flowOf(1)
        return matrixClientMock
    }

    class FakeNotificationHandler(
        override val platform: NotificationPlatform = NotificationPlatform.UNKNOWN,
        override val capabilities: NotificationCapabilities =
            NotificationCapabilities(
                isTitleSupported = true,
                isDescriptionSupported = true,
                isIconSupported = true,
                isActionSupported = true,
                isPopSupported = true,
                isSoundSupported = true,
                isCustomSoundSupported = true,
                isGroupingSupported = true,
                isStatusIconSupported = true,
                isCounterSupported = true,
            ),
        override val hasPermissions: Boolean = true,
    ) : NotificationHandler {
        override val name: String = "john notification"
        override val id: String = "id"
        override val appId: String = "tammy"
        override val priority: NotificationPriority = NotificationPriority.DEFAULT
        override val contributesToCounter: Boolean = true

        private val _notifications = mutableMapOf<String, Notification>()
        val notifications: Map<String, Notification>
            get() = _notifications.toMap()

        var isUnregistered = false
        var isClosed = false

        override suspend fun push(notification: Notification, tag: String): NotificationHandle {
            if (!hasPermissions) return NotificationHandle.INVALID
            _notifications[tag] = notification
            return FakeNotificationHandle(tag)
        }

        override suspend fun pop(handle: NotificationHandle) {
            pop(handle.tag)
        }

        override suspend fun pop(tag: String) {
            if (!hasPermissions) return
            _notifications.remove(tag)
        }

        override suspend fun update(tag: String, notification: Notification): NotificationHandle {
            if (!hasPermissions) return NotificationHandle.INVALID
            _notifications[tag] = notification
            return FakeNotificationHandle(tag)
        }

        override suspend fun clearAll() {
            if (!hasPermissions) return
            _notifications.clear()
        }

        override suspend fun unregister() {
            if (!hasPermissions) return
            isUnregistered = true
            close()
        }

        override fun close() {
            isClosed = true
        }

        fun initialize(notifications: Map<String, Notification>) {
            _notifications.clear()
            _notifications.putAll(notifications)
        }
    }

    class FakeNotificationHandle(override val tag: String) : NotificationHandle

    private fun createNotificationUpdateContent(eventContent: ClientEvent.RoomEvent<*>): NotificationUpdate.Content {
        return when (eventContent) {
            is ClientEvent.RoomEvent.MessageEvent ->
                NotificationUpdate.Content.Message(TimelineEvent(event = eventContent))
            is StateEvent -> NotificationUpdate.Content.State(eventContent)
        }
    }

    private fun createNotificationNew(tag: String, content: NotificationUpdate.Content): NotificationUpdate {
        return NotificationUpdate.New(tag, "", emptySet(), content)
    }

    private fun createNotificationUpdate(tag: String, content: NotificationUpdate.Content): NotificationUpdate {
        return NotificationUpdate.Update(tag, "", emptySet(), content)
    }

    private fun createNotificationRemove(tag: String): NotificationUpdate {
        return NotificationUpdate.Remove(tag, "")
    }

    private fun createRoomUser(user: UserId, roomId: RoomId = RoomId("dummy")): RoomUser {
        return RoomUser(
            roomId,
            user,
            user.full,
            StateEvent(
                MemberEventContent(avatarUrl = "avatar", membership = Membership.JOIN),
                EventId(""),
                user,
                roomId,
                0,
                stateKey = "",
            ),
        )
    }
}
