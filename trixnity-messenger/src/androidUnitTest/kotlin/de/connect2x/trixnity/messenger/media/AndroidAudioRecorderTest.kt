package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.configureTestLogging
import de.connect2x.trixnity.messenger.continually
import de.connect2x.trixnity.messenger.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.media.AndroidAudioRecorder
import de.connect2x.trixnity.messenger.media.AudioRecorder
import de.connect2x.trixnity.messenger.resetMocks
import de.connect2x.trixnity.messenger.runTestWithCoroutineScope
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.junit.After
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock

class AndroidAudioRecorderTest {
    private val clock = mock<Clock>()
    private val fileSystem = FakeFileSystem()
    private val getContext = mock<ContextGetter>()
    private val getActivity = mock<ActivityGetter>()

    val i18n = object : I18n(
        DefaultLanguages,
        createTestMatrixMessengerSettingsHolder(),
        GetSystemLang { "en" },
        TimeZone.of("CET"),
    ) {}


    init {
        resetMocks(
            clock,
            fileSystem,
            getContext,
            getActivity,
            i18n,
        )
    }

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @AfterTest
    fun tearDown() {
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun `init - initial state is ready`() = runTestWithCoroutineScope { coroutineScope ->
        val cut = androidAudioRecorder(coroutineScope)
        cut.state shouldBe AudioRecorder.State.Ready
        cut.registeredRequestPermission shouldBe null
    }

    private fun androidAudioRecorder(coroutineScope: CoroutineScope) =
        AndroidAudioRecorder(clock, fileSystem, getContext, getActivity, i18n, coroutineScope)
}
