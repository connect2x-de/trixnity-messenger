package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.util.Paths
import de.connect2x.trixnity.messenger.util.SecretString
import io.ktor.client.*
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

fun createTestTrixnityMessengerModule(debugName: String = "client") = module {
    single<DebugName> {
        DebugName { debugName }
    }
    single<HttpClientFactory> {
        HttpClientFactory {
            { config ->
                HttpClient {
                    config()
// TODO activate for better debugging
//                    install(Logging) {
//                        logger = Logger.DEFAULT
//                        level = LogLevel.ALL
//                    }
                }
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
    single<Paths> {
        object : Paths {
            override val fileSystem: FileSystem = FakeFileSystem()
            override val rootPath: Path = "/test".toPath()
        }
    }
}

suspend fun createTestMatrixMessenger() = createMatrixMessenger {
    modules = createDefaultTrixnityMessengerModules() + createTestTrixnityMessengerModule()
}