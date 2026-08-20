package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.store.repository.inMemory
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.CreateRepositoriesModule
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.create
import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerImpl
import de.connect2x.trixnity.messenger.multi.singleModeMatrixMessenger
import de.connect2x.trixnity.messenger.secrets.GetKey
import de.connect2x.trixnity.messenger.secrets.SecretByteArrayKeyProvider
import de.connect2x.trixnity.messenger.update
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.koin.dsl.module

fun createTrixnityMessengerTestModule() = module {
    single<CreateRepositoriesModule> {
        object : CreateRepositoriesModule {
            val modules: MutableMap<UserId, RepositoriesModule> = HashMap()

            override suspend fun generateDatabaseKey(): ByteArray? = null

            override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
                val module = RepositoriesModule.inMemory()
                modules += (userId to module)
                return module
            }

            override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule =
                modules[userId] ?: throw IllegalStateException("Repositories module for $userId not instantiated")

            override fun handleExceptions(exc: Exception) {}
        }
    }
    single<FileSystem> { FakeFileSystem() }
    single<SecretByteArrayKeyProvider> {
        object : SecretByteArrayKeyProvider {
            override val id: String = "dummy"
            override val level: Int = 0

            override suspend fun get(extra: JsonObject?, getInputKey: GetKey?): GetKey? = null

            override suspend fun rotate(
                oldExtra: JsonObject?,
                getOldInputKey: GetKey?,
                getNewInputKey: GetKey?,
            ): SecretByteArrayKeyProvider.RotateResult =
                SecretByteArrayKeyProvider.RotateResult(
                    getOldKey = get(null, getOldInputKey),
                    getNewKey = get(null, getNewInputKey),
                    newExtra = null,
                )

            @Deprecated("for backwards compatibility") override suspend fun getLegacy(): ByteArray? = null
        }
    }
}

suspend fun createTestMatrixMessenger(): MatrixMessengerWithRoot {
    val matrixMessenger =
        MatrixMessenger.create(Dispatchers.Default) {
            features.apply { enableNewAccountWizard = false }
            modulesFactories += { createTrixnityMessengerTestModule() }
        }
    matrixMessenger.di.get<MatrixMessengerSettingsHolder>().update<MatrixMessengerSettingsBase> {
        it.copy(preferredLang = "en")
    }
    return MatrixMessengerWithRoot(matrixMessenger)
}

suspend fun createTestMatrixMultiMessenger(coroutineContext: CoroutineContext = Dispatchers.Default) =
    MatrixMultiMessengerImpl(coroutineContext) {
        messenger = {
            features.apply { enableNewAccountWizard = false }
            modulesFactories += { createTrixnityMessengerTestModule() }
        }
        modulesFactories += { module { single<FileSystem> { FakeFileSystem() } } }
    }

suspend fun createTestMatrixMessengerFromMultiMessenger(debugName: String = "client") =
    MatrixMessengerWithRoot(createTestMatrixMultiMessenger().singleModeMatrixMessenger().first())
