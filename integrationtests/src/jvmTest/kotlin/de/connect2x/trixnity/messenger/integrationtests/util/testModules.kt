package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.trixnity.messenger.CreateRepositoriesModule
import de.connect2x.trixnity.messenger.DebugName
import de.connect2x.trixnity.messenger.MatrixMessenger
import de.connect2x.trixnity.messenger.create
import de.connect2x.trixnity.messenger.integrationtests.messenger.MatrixMessengerWithRoot
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerImpl
import de.connect2x.trixnity.messenger.multi.singleModeMatrixMessenger
import de.connect2x.trixnity.messenger.util.SecretByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

fun createTrixnityMessengerTestModule(debugName: String = "client") = module {
    single<DebugName> {
        DebugName { debugName }
    }
    single<CreateRepositoriesModule> {
        object : CreateRepositoriesModule {
            val modules: MutableMap<UserId, Module> = HashMap()

            override suspend fun create(userId: UserId): CreateRepositoriesModule.CreateResult {
                val module = createInMemoryRepositoriesModule()
                modules += (userId to module)
                return CreateRepositoriesModule.CreateResult(module, null)
            }

            override suspend fun load(userId: UserId, databaseKey: SecretByteArray?): Module =
                modules[userId] ?: throw IllegalStateException("Repositories module for $userId not instantiated")
        }
    }
    single<FileSystem> {
        FakeFileSystem()
    }
}

suspend fun createTestMatrixMessenger(debugName: String = "client") =
    MatrixMessengerWithRoot(
        MatrixMessenger.create {
            modulesFactories += { createTrixnityMessengerTestModule(debugName) }
        }
    )

suspend fun createTestMatrixMultiMessenger(
    debugName: String = "client",
    coroutineContext: CoroutineContext = Dispatchers.Default
) =
    MatrixMultiMessengerImpl(coroutineContext) {
        messenger = {
            modulesFactories += { createTrixnityMessengerTestModule(debugName) }
        }
        modulesFactories += {
            module {
                single<FileSystem> {
                    FakeFileSystem()
                }
            }
        }
    }

suspend fun createTestMatrixMessengerFromMultiMessenger(debugName: String = "client") =
    MatrixMessengerWithRoot(
        createTestMatrixMultiMessenger(debugName).singleModeMatrixMessenger().first()
    )
