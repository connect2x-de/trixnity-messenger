package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import de.connect2x.trixnity.messenger.util.FileDescriptor
import de.connect2x.trixnity.messenger.util.mb
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.createTestDefaultTrixnityMessengerModules
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.ServerData
import net.folivo.trixnity.clientserverapi.model.media.GetMediaConfig
import net.folivo.trixnity.clientserverapi.model.server.GetVersions
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.utils.ByteArrayFlow
import net.folivo.trixnity.utils.toByteArrayFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class SendAttachmentViewModelTest {
    private val roomId = RoomId("room1", "localhost")
    private val me = UserId("bob", "localhost")

    private val matrixClientMock: MatrixClient = mock()
    private val serverData: MutableStateFlow<ServerData> = MutableStateFlow(
        ServerData(
            versions = GetVersions.Response(listOf()),
            mediaConfig = GetMediaConfig.Response(50.mb())
        )
    )

    @BeforeTest
    fun beforeTest() {
        resetMocks(matrixClientMock)
        every { matrixClientMock.serverData } returns serverData
    }

    @Test
    fun `should have no error when uploading file more than max file size`() = runTestWithCoroutineScope {
        val cut = sendAttachmentViewModel(coroutineContext, 40.mb())
        delay(500.milliseconds)
        assertTrue { cut.error.value == null }
    }

    @Test
    fun `should have error when uploading file more than max file size`() = runTestWithCoroutineScope {
        val cut = sendAttachmentViewModel(coroutineContext, 60.mb())
        delay(500.milliseconds)
        assertTrue { cut.error.value != null }
    }

    private fun sendAttachmentViewModel(
        coroutineContext: CoroutineContext,
        fileSize: Long
    ): SendAttachmentViewModelImpl = SendAttachmentViewModelImpl(
        viewModelContext = MatrixClientViewModelContextImpl(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            coroutineContext = coroutineContext,
            userId = me,
            di = koinApplication {
                modules(createTestDefaultTrixnityMessengerModules(mapOf(me to matrixClientMock)) + module {
                    single<MatrixMessengerConfiguration> { MatrixMessengerConfiguration() }
                    single<MatrixClient> { matrixClientMock }
                })
            }.koin
        ),
        file = object : FileDescriptor {
            override val content: ByteArrayFlow = ByteArray(fileSize.toInt()) { it.toByte() }.toByteArrayFlow()
            override val fileName: String = "test.txt"
            override val fileSize: Long = fileSize
            override val mimeType: ContentType = ContentType.Any
        },
        selectedRoomId = roomId,
        onCloseAttachmentSendView = {},
    )

}
