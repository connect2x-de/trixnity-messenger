package de.connect2x.trixnity.messenger.integrationtests.util

import com.russhwolf.settings.MapSettings
import de.connect2x.trixnity.messenger.*
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettingsImpl
import io.ktor.client.*
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.store.repository.exposed.createExposedRepositoriesModule
import org.koin.dsl.module

fun itModules(name: String = "client") = module {
    single<MessengerSettings> { MessengerSettingsImpl(MapSettings()) }
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
        CreateRepositoriesModule { accountName ->
            getAppFolder(accountName) // also create a folder for the accounts
            createExposedRepositoriesModule(newDatabase(accountName))
        }
    }
    single<CreateMediaStore> {
        CreateMediaStore { InMemoryMediaStore() }
    }
    single<DebugName> {
        DebugName { name }
    }
}