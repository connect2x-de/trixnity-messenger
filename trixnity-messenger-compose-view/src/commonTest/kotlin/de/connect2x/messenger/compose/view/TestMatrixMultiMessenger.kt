package de.connect2x.messenger.compose.view

import de.connect2x.trixnity.messenger.CreateMediaStoreModule
import de.connect2x.trixnity.messenger.CreateRepositoriesModule
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.platformMatrixMessengerSettingsHolderModule
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.media.createInMemoryMediaStoreModule
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.core.model.UserId
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

val messengerTestConfiguration: MatrixMultiMessengerConfiguration.() -> Unit = {
    modulesFactories += listOf(
        // TODO this needs to be removed and fixed, as there is no MatrixMessengerSettingsHolderImpl at MultiMessenger level!
        ::platformMatrixMessengerSettingsHolderModule,
        // TODO there should be a more clean way for I18n
        ::platformGetSystemLangModule,
        {
            module {
                single<Languages> { DefaultLanguages }
                single<TimeZone> { TimeZone.currentSystemDefault() }
                single<I18n> { object : I18n(get(), get(), get(), get()) {} }
            }
        },
        { composeViewModule(null) },
        {
            module {
                single<FileSystem> {
                    FakeFileSystem()
                }
            }
        }
    )
    messengerConfiguration {
        modulesFactories += listOf(
            { composeViewModule(this) },
            {
                module {
                    single<CreateRepositoriesModule> {
                        object : CreateRepositoriesModule {
                            val modules: MutableMap<UserId, Module> = HashMap()

                            override suspend fun generateDatabaseKey(): ByteArray =
                                throw IllegalStateException("cannot create database key")

                            override suspend fun create(userId: UserId, databaseKey: ByteArray?): Module {
                                val module = createInMemoryRepositoriesModule()
                                modules += (userId to module)
                                return module
                            }

                            override suspend fun load(userId: UserId, databaseKey: ByteArray?): Module =
                                modules[userId]
                                    ?: throw IllegalStateException("Repositories module for $userId not instantiated")
                        }
                    }
                    single<CreateMediaStoreModule> {
                        object : CreateMediaStoreModule {
                            val store by lazy { createInMemoryMediaStoreModule() }
                            override suspend fun invoke(userId: UserId): Module = store
                        }
                    }

                    single<FileSystem> {
                        FakeFileSystem()
                    }
                }
            }
        )
    }
}


expect suspend fun createTestMatrixMultiMessenger(): MatrixMultiMessenger
