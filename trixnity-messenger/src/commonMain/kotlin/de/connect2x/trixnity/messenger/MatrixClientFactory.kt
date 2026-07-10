package de.connect2x.trixnity.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.MatrixClientConfiguration
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.create
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.core.MSC3814
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent
import de.connect2x.trixnity.core.serialization.events.EventContentSerializerMappings
import de.connect2x.trixnity.core.serialization.events.EventContentSerializerMappingsBuilder
import de.connect2x.trixnity.core.serialization.events.default
import de.connect2x.trixnity.messenger.secrets.SecretByteArrayIntegrityCheckException
import de.connect2x.trixnity.messenger.secrets.SecretByteArrays
import de.connect2x.trixnity.messenger.secrets.getDatabaseKey
import de.connect2x.trixnity.messenger.secrets.setDatabaseKey
import kotlin.coroutines.CoroutineContext
import org.koin.dsl.module

private val log = Logger("de.connect2x.trixnity.messenger.MatrixClientFactory")

interface MatrixClientFactory {
    suspend fun create(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData,
        configuration: MatrixClientConfiguration.() -> Unit = {},
    ): Result<MatrixClient>

    suspend fun load(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData? = null,
        configuration: MatrixClientConfiguration.() -> Unit = {},
    ): Result<MatrixClient>
}

class MatrixClientFactoryImpl(
    private val secretByteArrays: SecretByteArrays,
    private val createRepositoriesModule: CreateRepositoriesModule,
    private val createMediaStoreModule: CreateMediaStoreModule,
    private val createCryptoDriverModule: CreateCryptoDriverModule,
    private val appCoroutineContext: CoroutineContext,
    private val messengerConfiguration: MatrixMessengerConfiguration,
) : MatrixClientFactory {
    override suspend fun create(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData,
        configuration: MatrixClientConfiguration.() -> Unit,
    ): Result<MatrixClient> =
        MatrixClient.create(
                repositoriesModule = createRepositoriesModuleOrThrow(userId),
                mediaStoreModule = createMediaStoreModule(userId),
                cryptoDriverModule = createCryptoDriverModule(),
                authProviderData = authProviderData,
                coroutineContext = appCoroutineContext,
            ) {
                configureDefault()
                configuration()
                messengerConfiguration.client.invoke(this)
            }
            .recoverCatching { throwable ->
                // some database exceptions only occur when the database is initialized
                if (throwable is Exception) createRepositoriesModule.handleExceptions(throwable)
                throw throwable
            }

    override suspend fun load(
        userId: UserId,
        authProviderData: MatrixClientAuthProviderData?,
        configuration: MatrixClientConfiguration.() -> Unit,
    ): Result<MatrixClient> =
        MatrixClient.create(
                repositoriesModule = loadRepositoriesModuleOrThrow(userId),
                mediaStoreModule = createMediaStoreModule(userId),
                cryptoDriverModule = createCryptoDriverModule(),
                authProviderData = authProviderData,
                coroutineContext = appCoroutineContext,
            ) {
                configureDefault()
                configuration()
                messengerConfiguration.client.invoke(this)
            }
            .recoverCatching { throwable ->
                // some database exceptions only occur when the database is initialized
                if (throwable is Exception) createRepositoriesModule.handleExceptions(throwable)
                throw throwable
            }

    private suspend fun createRepositoriesModuleOrThrow(userId: UserId): RepositoriesModule {
        log.debug { "create repositories module" }
        val repositoriesModule =
            try {
                createRepositoriesModule.create(userId, getDatabaseKey(userId, true))
            } catch (exc: SecretByteArrayIntegrityCheckException) {
                log.error(exc) { "The settings.json file seems to be manipulated." }
                throw MatrixClientInitializationException.DatabaseKeysManipulatedException(exc.message)
            } catch (exc: Exception) {
                createRepositoriesModule.handleExceptions(exc)
                if (isLocked(exc)) throw MatrixClientInitializationException.DatabaseLockedException()
                else throw MatrixClientInitializationException.DatabaseAccessException(exc.message)
            }
        return repositoriesModule
    }

    private suspend fun loadRepositoriesModuleOrThrow(userId: UserId): RepositoriesModule {
        log.debug { "load repositories module" }
        val repositoriesModule =
            try {
                createRepositoriesModule.load(userId, getDatabaseKey(userId, false))
            } catch (exc: SecretByteArrayIntegrityCheckException) {
                log.error(exc) { "The settings.json file seems to be manipulated." }
                throw MatrixClientInitializationException.DatabaseKeysManipulatedException(exc.message)
            } catch (exc: Exception) {
                createRepositoriesModule.handleExceptions(exc)
                if (isLocked(exc)) throw MatrixClientInitializationException.DatabaseLockedException()
                else throw MatrixClientInitializationException.DatabaseAccessException(exc.message)
            }
        return repositoriesModule
    }

    private suspend fun getDatabaseKey(userId: UserId, createNew: Boolean): ByteArray? {
        return if (createNew) {
            val newKey = createRepositoriesModule.generateDatabaseKey() ?: return null
            secretByteArrays.setDatabaseKey(userId, newKey)
            newKey
        } else {
            secretByteArrays.getDatabaseKey(userId)
        }
    }

    // we cannot check for SQLNonTransientConnectionException since this is common code
    private fun isLocked(exc: Throwable): Boolean =
        exc.cause?.message?.contains("locked") == true || exc.cause?.let { isLocked(it) } ?: false

    private fun MatrixClientConfiguration.configureDefault() {
        markOwnMessageAsRead = true
        enableExternalNotifications = true
        httpClientEngine = messengerConfiguration.httpClientEngine
        httpClientConfig = messengerConfiguration.httpClientConfig
        @OptIn(MSC3814::class)
        experimentalFeatures.enableMSC3814 = true
        lastRelevantEventFilter = {
            val content = it.content
            val isReplace = content is MessageEventContent && content.relatesTo is RelatesTo.Replace
            val isMessage = content is RoomMessageEventContent || content is EncryptedMessageEventContent
            (!isReplace && isMessage)
        }
        modulesFactories += {
            module {
                single<EventContentSerializerMappings> {
                    val customEventContentSerializerMappings = getAll<CustomEventContentSerializerMappings>()
                    customEventContentSerializerMappings.fold(EventContentSerializerMappings.default) { a, b -> a + b }
                }
            }
        }
    }
}

interface CustomEventContentSerializerMappings : EventContentSerializerMappings {
    companion object {
        operator fun invoke(
            builder: EventContentSerializerMappingsBuilder.() -> Unit
        ): CustomEventContentSerializerMappings =
            object :
                CustomEventContentSerializerMappings,
                EventContentSerializerMappings by EventContentSerializerMappingsBuilder().apply(builder).build() {}
    }
}
