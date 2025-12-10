package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettings
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.util.GraphemeIterableProvider
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.testGraphemeIterableProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MediaStoreModule
import net.folivo.trixnity.client.RepositoriesModule
import net.folivo.trixnity.client.media.inMemory
import net.folivo.trixnity.client.store.repository.inMemory
import net.folivo.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import net.folivo.trixnity.core.model.UserId
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.Instant

fun TestScope.createTestDefaultTrixnityMessengerModules(
    matrixClientMock: MatrixClient,
    userId: UserId,
    settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder(),
) = createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock), settings)

fun TestScope.createTestDefaultTrixnityMessengerModules(
    matrixClients: Map<UserId, MatrixClient>,
    settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder(),
) = createTestDefaultTrixnityMessengerModules(MutableStateFlow(matrixClients), settings)

fun TestScope.createTestDefaultTrixnityMessengerModules(
    matrixClients: StateFlow<Map<UserId, MatrixClient>>? = null,
    settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder(),
) = createTrixnityMessengerDefaultModuleFactories().map { it.invoke() } + module {
    single<TimeZone> { TimeZone.of("CET") }
    single<CoroutineScope> {
        CoroutineScope(
            backgroundScope.coroutineContext
                    + SupervisorJob(backgroundScope.coroutineContext[Job])
        )
    }
    single<MatrixMessengerConfiguration> { MatrixMessengerConfiguration() }.bind<MatrixMessengerBaseConfiguration>()
    single<MatrixMessengerSettingsHolder> { settings }
    if (matrixClients != null) single<MatrixClients> {
        @OptIn(ExperimentalForInheritanceCoroutinesApi::class) object : MatrixClients,
            StateFlow<Map<UserId, MatrixClient>> by matrixClients {

            override val initFromStoreResult: StateFlow<MatrixClients.InitFromStoreResult?>
                get() = TODO("Not yet implemented")
            override val isInitialized: StateFlow<Boolean>
                get() = TODO("Not yet implemented")

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
        }
    }
    single<CreateRepositoriesModule> {
        object : CreateRepositoriesModule {
            val module by lazy { RepositoriesModule.inMemory() }

            override suspend fun generateDatabaseKey(): ByteArray? = null
            override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule = module
            override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule = module
        }
    }
    single<CreateMediaStoreModule> {
        object : CreateMediaStoreModule {
            val store by lazy { MediaStoreModule.inMemory() }
            override suspend fun invoke(userId: UserId): MediaStoreModule = store
        }
    }
    single<FileSystem> { FakeFileSystem() }
    single<RootPath> { RootPath("/".toPath()) }
    single<GraphemeIterableProvider> { testGraphemeIterableProvider() }
    single<Clock> {
        object : Clock {
            @OptIn(ExperimentalCoroutinesApi::class)
            override fun now(): Instant = Instant.fromEpochMilliseconds(testScheduler.currentTime)
        }
    }
}

fun createTestMatrixMessengerSettingsHolder(): MatrixMessengerSettingsHolder {
    val settingsHolder: MutableStateFlow<MatrixMessengerSettings?> =
        MutableStateFlow(MatrixMessengerSettings(mapOf("preferredLang" to JsonPrimitive("en"))))
    val dummyStorage = object : SettingsStorage {
        override suspend fun read(): String? = null
        override suspend fun write(settings: String) {}
    }
    val delegate = MatrixMessengerSettingsHolderImpl(dummyStorage, settingsHolder)
    return object : MatrixMessengerSettingsHolder by delegate {
        override fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> = flow {
            val hasNoEntry = delegate[userId].first() == null
            if (hasNoEntry) delegate.update<MatrixMessengerAccountSettingsBase>(userId) { it }
            emitAll(delegate[userId])
        }
    }
}

fun createTestMatrixMultiMessengerSettingsHolder(): MatrixMultiMessengerSettingsHolder {
    val settingsHolder: MutableStateFlow<MatrixMultiMessengerSettings?> =
        MutableStateFlow(MatrixMultiMessengerSettings(mapOf()))
    val dummyStorage = object : SettingsStorage {
        override suspend fun read(): String? = null
        override suspend fun write(settings: String) {}
    }
    return MatrixMultiMessengerSettingsHolderImpl(dummyStorage, settingsHolder)
}

