package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.CreateMediaStore
import de.connect2x.trixnity.messenger.CreateRepositoriesModule
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.createTrixnityMessengerDefaultModuleFactories
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.testDispatcher
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.GraphemeIterableProvider
import de.connect2x.trixnity.messenger.util.RootPath
import de.connect2x.trixnity.messenger.util.SecretByteArray
import de.connect2x.trixnity.messenger.util.ImmediateDispatcherElement
import de.connect2x.trixnity.messenger.util.testGraphemeIterableProvider
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.UserId
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

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
        backgroundScope + ImmediateDispatcherElement(testDispatcher)
    }
    single<MatrixMessengerConfiguration> { MatrixMessengerConfiguration() }.bind<MatrixMessengerBaseConfiguration>()
    single<MatrixMessengerSettingsHolder> { settings }
    if (matrixClients != null) single<MatrixClients> {
        @OptIn(ExperimentalForInheritanceCoroutinesApi::class) object : MatrixClients,
            StateFlow<Map<UserId, MatrixClient>> by matrixClients {
            override suspend fun login(
                baseUrl: Url, identifier: IdentifierType, password: String, initialDeviceDisplayName: String?
            ): Result<MatrixClient> {
                TODO("Not yet implemented")
            }

            override suspend fun login(
                baseUrl: Url, token: String, initialDeviceDisplayName: String?
            ): Result<MatrixClient> {
                TODO("Not yet implemented")
            }

            override suspend fun loginWith(
                baseUrl: Url, loginInfo: MatrixClient.LoginInfo
            ): Result<MatrixClient> {
                TODO("Not yet implemented")
            }

            override suspend fun initFromStore(): MatrixClients.InitFromStoreResult {
                TODO("Not yet implemented")
            }

            override suspend fun logout(userId: UserId): Result<Unit> {
                TODO("Not yet implemented")
            }

            override suspend fun remove(userId: UserId): Result<Unit> {
                TODO("Not yet implemented")
            }
        }
    }
    single<CreateRepositoriesModule> {
        object : CreateRepositoriesModule {
            val module by lazy { createInMemoryRepositoriesModule() }

            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult =
                CreateRepositoriesModule.CreateResult(module, null)

            override suspend fun load(userId: UserId, databaseKey: SecretByteArray?): Module = module
        }
    }
    single<CreateMediaStore> {
        object : CreateMediaStore {
            val store by lazy { InMemoryMediaStore() }
            override suspend fun invoke(userId: UserId): MediaStore = store
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
