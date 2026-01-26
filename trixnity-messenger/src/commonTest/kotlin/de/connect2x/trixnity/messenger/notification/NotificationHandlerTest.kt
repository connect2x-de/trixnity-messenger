package de.connect2x.trixnity.messenger.notification

import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.createTestMatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.resetMocks
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.core.model.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class NotificationHandlerTest {

    lateinit var notificationProviders: List<NotificationProviderMock>
    lateinit var multiSettings: MatrixMultiMessengerSettingsHolder
    val matrixClients = mock<MatrixClients>()

    lateinit var cut: NotificationHandlers
    var callbackParam: Boolean? = null
    val notificationHandler = mock<NotificationHandler>()

    @BeforeTest
    fun setup() {
        resetMocks(notificationHandler)
        multiSettings = createTestMatrixMultiMessengerSettingsHolder()
        callbackParam = null
        notificationProviders = listOf(
            NotificationProviderMock(),
            NotificationProviderMock(),
        )
        cut = NotificationHandlersImpl(
            MatrixMessengerConfiguration(),
            NotificationProviders(notificationProviders) { throw IllegalStateException() },
            multiSettings,
            matrixClients,
            { callbackParam = it }
        ) { _, _, _ ->
            notificationHandler
        }
    }

    @Test
    fun `continuouslyRequestPermissions - missing permission and notification enabled - request`() = runTest {
        everySuspend { notificationHandler.requestPermissions(any()) } calls { (callback: (Boolean) -> Unit) ->
            callback(true)
        }
        every { notificationHandler.hasPermissions } returns false

        backgroundScope.launch {
            cut.continuouslyRequestPermissions()
        }

        delay(100.milliseconds)
        callbackParam shouldBe null

        notificationProviders.first().isEnabled.value = true
        delay(100.milliseconds)
        callbackParam shouldBe true

        verifySuspend { notificationHandler.requestPermissions(any()) }
    }

    @Test
    fun `continuouslyRequestPermissions - has permission - nothing`() = runTest {
        everySuspend { notificationHandler.requestPermissions(any()) } calls { (callback: (Boolean) -> Unit) ->
            callback(true)
        }
        every { notificationHandler.hasPermissions } returns true

        backgroundScope.launch {
            cut.continuouslyRequestPermissions()
        }

        delay(100.milliseconds)
        callbackParam shouldBe null

        notificationProviders.first().isEnabled.value = true
        delay(100.milliseconds)
        callbackParam shouldBe null

        verifySuspend(VerifyMode.not) { notificationHandler.requestPermissions(any()) }
    }

    @Test
    fun `continuouslyRequestPermissions - notification disabled - nothing`() = runTest {
        everySuspend { notificationHandler.requestPermissions(any()) } calls { (callback: (Boolean) -> Unit) ->
            callback(true)
        }
        every { notificationHandler.hasPermissions } returns false

        backgroundScope.launch {
            cut.continuouslyRequestPermissions()
        }

        delay(100.milliseconds)
        callbackParam shouldBe null

        verifySuspend(VerifyMode.not) { notificationHandler.requestPermissions(any()) }
    }

    class NotificationProviderMock : NotificationProvider {
        override val id = object : NotificationProvider.Id<NotificationProviderMock> {}
        override val config = object : NotificationProvider.Config<NotificationProviderMock> {}
        override val displayName: String = "mock"
        override val canBeEnabled: Boolean = true
        override val isEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
        override fun isEnabled(userId: UserId): Flow<Boolean> = flowOf(false)
        override suspend fun enable(userId: UserId) {}
        override suspend fun disable(userId: UserId) {}
    }

}
