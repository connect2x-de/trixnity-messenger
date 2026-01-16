package de.connect2x.trixnity.messenger.notification

import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.createTestMatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.update
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.GetDefaultDeviceDisplayName
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentiallyReturns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifyNoMoreCalls
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.client.PushApiClient
import net.folivo.trixnity.clientserverapi.model.push.PusherData
import net.folivo.trixnity.clientserverapi.model.push.SetPushers
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PushNotificationProviderTest {
    val userId1 = UserId("user1", "server")
    val userId2 = UserId("user2", "server")

    lateinit var multiSettings: MatrixMultiMessengerSettingsHolder
    lateinit var settings: MatrixMessengerSettingsHolder
    val getDefaultDeviceDisplayName = GetDefaultDeviceDisplayName { "Test device" }
    lateinit var matrixClients: MatrixClients
    val matrixClient1 = mock<MatrixClient>()
    val matrixClient2 = mock<MatrixClient>()
    val matrixClientApi = mock<MatrixClientServerApiClient>()
    val matrixClientApiPush = mock<PushApiClient>()

    @OptIn(ExperimentalForInheritanceCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        resetMocks(matrixClient1, matrixClient2, matrixClientApi)
        multiSettings = createTestMatrixMultiMessengerSettingsHolder()
        settings = createTestMatrixMessengerSettingsHolder()
        matrixClients = object : MatrixClients,
            StateFlow<Map<UserId, MatrixClient>> by MutableStateFlow(
                mapOf(
                    userId1 to matrixClient1,
                    userId2 to matrixClient2,
                )
            ) {

            override val initFromStoreResult: StateFlow<MatrixClients.InitFromStoreResult?>
                get() = TODO("Not yet implemented")
            override val isInitialized: StateFlow<Boolean> = MutableStateFlow(true)
            override suspend fun create(authProviderData: MatrixClientAuthProviderData): MatrixClients.CreateResult {
                TODO("Not yet implemented")
            }

            override suspend fun logout(userId: UserId): Result<Unit> {
                TODO("Not yet implemented")
            }

            override suspend fun remove(userId: UserId): Result<Unit> {
                TODO("Not yet implemented")
            }

            override fun close() {
                TODO("Not yet implemented")
            }

            override suspend fun doWork() {
                TODO("Not yet implemented")
            }
        }
        every { matrixClient1.api } returns matrixClientApi
        every { matrixClientApi.push } returns matrixClientApiPush
    }

    fun TestScope.cut() = TestPushNotificationProvider(
        config = MatrixMessengerConfiguration().apply {
            pushUrl = "https://push.connect2x.de"
        },
        multiSettings = multiSettings,
        settings = settings,
        getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
        matrixClients = matrixClients,
        coroutineScope = backgroundScope,
    )

    @Test
    fun `deliverPushKeys - enabled - different pushKey in settings - set`() = runTest {
        everySuspend { matrixClientApiPush.setPushers(any(), any()) } returns Result.success(Unit)
        multiSettings.update<MatrixMultiMessengerNotificationProviderPushSettings> {
            it.copy(pushKey = "new_push_key")
        }
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = true, deliveredPushKey = "old_push_key")
        }
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId2) {
            it.copy(enabled = true, deliveredPushKey = "new_push_key")
        }

        backgroundScope.launch { cut().doWork() }
        delay(100.milliseconds)

        verifySuspend(VerifyMode.exhaustive) {
            matrixClientApiPush.setPushers(
                SetPushers.Request.Set(
                    appId = "de.connect2x.trixnity.messenger",
                    appDisplayName = "Trixnity Messenger",
                    data = PusherData(
                        url = "https://push.connect2x.de",
                        format = "event_id_only",
                        customFields = JsonObject(mapOf("dino" to JsonPrimitive(true))),
                    ),
                    deviceDisplayName = getDefaultDeviceDisplayName(),
                    kind = "http",
                    lang = "en",
                    pushkey = "new_push_key",
                    append = true,
                )
            )
        }
    }

    @Test
    fun `deliverPushKeys - disabled - different pushKey in settings - remove`() = runTest {
        everySuspend { matrixClientApiPush.setPushers(any(), any()) } returns Result.success(Unit)
        multiSettings.update<MatrixMultiMessengerNotificationProviderPushSettings> {
            it.copy(pushKey = "new_push_key")
        }
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = false, deliveredPushKey = "old_push_key")
        }
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId2) {
            it.copy(enabled = true, deliveredPushKey = "new_push_key")
        }

        backgroundScope.launch { cut().doWork() }
        delay(100.milliseconds)

        verifySuspend(VerifyMode.exhaustive) {
            matrixClientApiPush.setPushers(
                SetPushers.Request.Remove(
                    appId = "de.connect2x.trixnity.messenger",
                    pushkey = "old_push_key",
                )
            )
        }
    }

    @Test
    fun `deliverPushKeys - enabled - same pushKey in settings - skip`() = runTest {
        everySuspend { matrixClientApiPush.setPushers(any(), any()) } returns Result.success(Unit)
        multiSettings.update<MatrixMultiMessengerNotificationProviderPushSettings> {
            it.copy(pushKey = "new_push_key")
        }
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = true, deliveredPushKey = "new_push_key")
        }

        backgroundScope.launch { cut().doWork() }
        delay(100.milliseconds)

        verifyNoMoreCalls(matrixClientApiPush)
    }

    @Test
    fun `deliverPushKeys - disabled - null pushKey in settings - skip`() = runTest {
        everySuspend { matrixClientApiPush.setPushers(any(), any()) } returns Result.success(Unit)
        multiSettings.update<MatrixMultiMessengerNotificationProviderPushSettings> {
            it.copy(pushKey = "new_push_key")
        }
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = false, deliveredPushKey = null)
        }

        backgroundScope.launch { cut().doWork() }
        delay(100.milliseconds)

        verifyNoMoreCalls(matrixClientApiPush)
    }

    @Test
    fun `deliverPushKeys - MatrixServerException - skip retry`() = runTest {
        everySuspend { matrixClientApiPush.setPushers(any(), any()) } returns Result.failure(
            MatrixServerException(HttpStatusCode.Unauthorized, ErrorResponse.Unauthorized(""))
        )
        multiSettings.update<MatrixMultiMessengerNotificationProviderPushSettings> {
            it.copy(pushKey = "new_push_key")
        }
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = false, deliveredPushKey = "old_push_key")
        }

        backgroundScope.launch { cut().doWork() }
        delay(5.seconds)

        verifySuspend(VerifyMode.exactly(1)) {
            matrixClientApiPush.setPushers(
                SetPushers.Request.Remove(
                    appId = "de.connect2x.trixnity.messenger",
                    pushkey = "old_push_key",
                )
            )
        }
    }

    @Test
    fun `deliverPushKeys - other exceptions retry`() = runTest {
        everySuspend { matrixClientApiPush.setPushers(any(), any()) } sequentiallyReturns listOf(
            Result.failure(
                RuntimeException("dino")
            ),
            Result.success(Unit)
        )
        multiSettings.update<MatrixMultiMessengerNotificationProviderPushSettings> {
            it.copy(pushKey = "new_push_key")
        }
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = false, deliveredPushKey = "old_push_key")
        }

        backgroundScope.launch { cut().doWork() }
        delay(5.seconds)

        verifySuspend(VerifyMode.exactly(2)) {
            matrixClientApiPush.setPushers(
                SetPushers.Request.Remove(
                    appId = "de.connect2x.trixnity.messenger",
                    pushkey = "old_push_key",
                )
            )
        }
    }

    @Test
    fun `isEnabled - one account enabled - true`() = runTest {
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = false)
        }
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId2) {
            it.copy(enabled = false)
        }
        val cut = cut()

        cut.isEnabled.value shouldBe false

        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId2) {
            it.copy(enabled = true)
        }
        delay(100.milliseconds)
        cut.isEnabled.value shouldBe true
    }

    @Test
    fun `isEnabled - all account disabled - false`() = runTest {
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = false)
        }
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId2) {
            it.copy(enabled = true)
        }
        val cut = cut()

        cut.isEnabled.value shouldBe true

        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId2) {
            it.copy(enabled = false)
        }
        delay(100.milliseconds)
        cut.isEnabled.value shouldBe false
    }

    @Test
    fun `isEnabled - for user`() = runTest {
        settings.create(userId1, MatrixMessengerAccountSettingsBase())
        settings.create(userId2, MatrixMessengerAccountSettingsBase())
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId1) {
            it.copy(enabled = false)
        }
        settings.update<MatrixMessengerAccountNotificationProviderPushSettings>(userId2) {
            it.copy(enabled = true)
        }
        val cut = cut()

        cut.isEnabled(userId1).first() shouldBe false
        cut.isEnabled(userId2).first() shouldBe true
    }

    class TestPushNotificationProvider(
        config: MatrixMessengerConfiguration,
        multiSettings: MatrixMultiMessengerSettingsHolder?,
        settings: MatrixMessengerSettingsHolder,
        getDefaultDeviceDisplayName: GetDefaultDeviceDisplayName,
        matrixClients: MatrixClients,
        coroutineScope: CoroutineScope,
    ) : PushNotificationProvider(
        config = config,
        multiSettings = multiSettings,
        settings = settings,
        getDefaultDeviceDisplayName = getDefaultDeviceDisplayName,
        matrixClients = matrixClients,
        coroutineScope = coroutineScope,
    ) {
        var enableServiceCalled: Boolean = false
        override suspend fun enableService() {
            enableServiceCalled = true
        }

        var disableServiceCalled: Boolean = false
        override suspend fun disableService() {
            disableServiceCalled = true
        }

        override suspend fun getPusherCustomFields(
            profile: String?,
            account: UserId
        ): JsonObject = JsonObject(mapOf("dino" to JsonPrimitive(true)))

        override val id: String = "test"
        override val displayName: String = "test"
    }
}
