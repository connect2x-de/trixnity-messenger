package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.util.SecretString
import io.ktor.http.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
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
    single<MatrixMessengerConfiguration> { MatrixMessengerConfiguration() }
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

            override suspend fun load(userId: UserId, databasePassword: SecretString?): Module = module
        }
    }
    single<CreateMediaStore> {
        object : CreateMediaStore {
            val store by lazy { InMemoryMediaStore() }
            override suspend fun invoke(userId: UserId): MediaStore = store
        }
    }
}

fun createTestMatrixMessengerSettingsHolder(): MatrixMessengerSettingsHolder {
    val settingsHolder: MutableStateFlow<MatrixMessengerSettings> =
        MutableStateFlow(MatrixMessengerSettings(preferredLang = "en"))
    return object : MatrixMessengerSettingsHolder, StateFlow<MatrixMessengerSettings> by settingsHolder {
        override fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> {
            settingsHolder.update {
                if (it.accounts.containsKey(userId)) it
                else it.copy(
                    accounts = it.accounts + (userId to MatrixMessengerAccountSettings(
                        userId = userId,
                        databasePassword = null
                    ))
                )
            }
            return settingsHolder.map { it.accounts[userId] }
        }

        override suspend fun update(
            userId: UserId,
            updater: (MatrixMessengerAccountSettings?) -> MatrixMessengerAccountSettings?
        ) = update {
            val oldAccounts = it.accounts
            val newAccount = updater(
                oldAccounts[userId] ?: MatrixMessengerAccountSettings(
                    userId = userId,
                    databasePassword = null
                )
            )
            val newAccounts =
                if (newAccount == null) oldAccounts - userId
                else oldAccounts + (userId to newAccount)
            it.copy(accounts = newAccounts)
        }

        override suspend fun update(updater: (MatrixMessengerSettings) -> MatrixMessengerSettings) =
            settingsHolder.update(updater)

        override suspend fun init() {}
    }
}