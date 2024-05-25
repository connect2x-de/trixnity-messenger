package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.CreateMediaStore
import de.connect2x.trixnity.messenger.CreateRepositoriesModule
import de.connect2x.trixnity.messenger.MatrixClients
import de.connect2x.trixnity.messenger.MatrixMessengerBaseConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.createDefaultTrixnityMessengerModules
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.util.SecretByteArray
import io.ktor.http.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.UserId
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

fun createTestDefaultTrixnityMessengerModules(
    matrixClientMock: MatrixClient,
    userId: UserId,
    settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder(),
) = createTestDefaultTrixnityMessengerModules(mapOf(userId to matrixClientMock), settings)

fun createTestDefaultTrixnityMessengerModules(
    matrixClients: Map<UserId, MatrixClient>,
    settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder(),
) = createTestDefaultTrixnityMessengerModules(MutableStateFlow(matrixClients), settings)

fun createTestDefaultTrixnityMessengerModules(
    matrixClients: StateFlow<Map<UserId, MatrixClient>>? = null,
    settings: MatrixMessengerSettingsHolder = createTestMatrixMessengerSettingsHolder(),
) = createDefaultTrixnityMessengerModules() + module {
    single<CoroutineScope> {
        CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        })
    }
    single<MatrixMessengerConfiguration> { MatrixMessengerConfiguration() }.bind<MatrixMessengerBaseConfiguration>()
    single<MatrixMessengerSettingsHolder> { settings }
    if (matrixClients != null)
        single<MatrixClients> {
            object : MatrixClients, StateFlow<Map<UserId, MatrixClient>> by matrixClients {
                override suspend fun login(
                    baseUrl: Url,
                    identifier: IdentifierType,
                    password: String,
                    initialDeviceDisplayName: String?
                ): Result<Unit> {
                    TODO("Not yet implemented")
                }

                override suspend fun login(
                    baseUrl: Url,
                    token: String,
                    initialDeviceDisplayName: String?
                ): Result<Unit> {
                    TODO("Not yet implemented")
                }

                override suspend fun loginWith(baseUrl: Url, loginInfo: MatrixClient.LoginInfo): Result<Unit> {
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

            override suspend fun load(userId: UserId, databasePassword: SecretByteArray?): Module = module
        }
    }
    single<CreateMediaStore> {
        object : CreateMediaStore {
            val store by lazy { InMemoryMediaStore() }
            override suspend fun invoke(userId: UserId): MediaStore = store
        }
    }
    single<FileSystem> { FakeFileSystem() }
}

fun createTestMatrixMessengerSettingsHolder(): MatrixMessengerSettingsHolder {
    val settingsHolder: MutableStateFlow<MatrixMessengerSettings?> =
        MutableStateFlow(MatrixMessengerSettings(mapOf("preferredLang" to JsonPrimitive("en"))))
    val dummyStorage = object : SettingsStorage {
        override suspend fun read(): String? = null
        override suspend fun write(settings: String) {}
    }
    return MatrixMessengerSettingsHolderImpl(dummyStorage, settingsHolder)
}
