package de.connect2x.trixnity.messenger.compose.view

import de.connect2x.trixnity.client.MediaStoreModule
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.media.inMemory
import de.connect2x.trixnity.client.store.repository.inMemory
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.CreateMediaStoreModule
import de.connect2x.trixnity.messenger.CreateRepositoriesModule
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.util.createTestMatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.util.createTestMatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessenger
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerSettingsHolder
import de.connect2x.trixnity.messenger.platformMatrixMessengerSettingsHolderModule
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

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

                single<MatrixMultiMessengerSettingsHolder> {
                    createTestMatrixMultiMessengerSettingsHolder()
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
                            val modules: MutableMap<UserId, RepositoriesModule> = HashMap()

                            override suspend fun generateDatabaseKey(): ByteArray? = null
                            override suspend fun create(userId: UserId, databaseKey: ByteArray?): RepositoriesModule {
                                val module = RepositoriesModule.inMemory()
                                modules += (userId to module)
                                return module
                            }

                            override suspend fun load(userId: UserId, databaseKey: ByteArray?): RepositoriesModule =
                                modules[userId]
                                    ?: throw IllegalStateException("Repositories module for $userId not instantiated")
                        }
                    }
                    single<CreateMediaStoreModule> {
                        object : CreateMediaStoreModule {
                            val store by lazy { MediaStoreModule.inMemory() }
                            override suspend fun invoke(userId: UserId): MediaStoreModule = store
                        }
                    }

                    single<FileSystem> {
                        FakeFileSystem()
                    }

                    single<MatrixMessengerSettingsHolder> {
                        createTestMatrixMessengerSettingsHolder()
                    }
                }
            }
        )
    }
}


expect suspend fun createTestMatrixMultiMessenger(coroutineContext: CoroutineContext): MatrixMultiMessenger
