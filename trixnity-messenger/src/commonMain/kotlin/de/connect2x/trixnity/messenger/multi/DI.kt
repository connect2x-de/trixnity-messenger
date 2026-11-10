package de.connect2x.trixnity.messenger.multi

import de.connect2x.trixnity.client.ModuleFactory
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.util.InformationMarkdownFlavour
import de.connect2x.trixnity.messenger.util.InformationMarkdownFlavourImpl
import de.connect2x.trixnity.messenger.util.SharedDataHandler
import de.connect2x.trixnity.messenger.util.SharedDataHandlerImpl
import de.connect2x.trixnity.messenger.util.platformCloseAppModule
import de.connect2x.trixnity.messenger.util.platformMinimizeAppModule
import de.connect2x.trixnity.messenger.util.platformPathsModule
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import de.connect2x.trixnity.messenger.util.platformUriHandlerModule
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

fun createTrixnityMultiMessengerDefaultModuleFactories(): List<ModuleFactory> =
    listOf(
        {
            module {
                single<Clock> { Clock.System }
                single<TimeZone> { TimeZone.currentSystemDefault() }
                single<ProfileManager> { ProfileManagerImpl(get(), get(), get(), get()) }
                single<CopyMultiMessengerSingletons>(named("DefaultCopyMultiMessengerSingletons")) {
                    DefaultCopyMultiMessengerSingletons
                }
                single<BackHandler> { BackHandlerImpl() }
                single<SharedDataHandler> { SharedDataHandlerImpl() }
                // Imprint and Privacy are shown by the multi messenger itself (login, profile selection),
                // where this is the root scope, and resolve it from there.
                single<InformationMarkdownFlavour> { InformationMarkdownFlavourImpl() }
                single<MultiProfileMigrator> { MultiProfileMigratorImpl(get()) }.apply { bind<Worker>() }
                // sensible default as some instance in the multi messenger might need i18n and it should not crash
                single<Languages> { DefaultLanguages }
                single<I18n> { object : I18n(get(), get(), get(), get<TimeZone>()) {} }
            }
        },
        ::profileViewModule,
        ::platformPathsModule,
        ::platformMatrixMultiMessengerSettingsHolderModule,
        ::matrixMessengerFactoryModule,
        ::platformUriHandlerModule,
        ::platformCloseAppModule,
        ::platformMinimizeAppModule,
        ::platformSendLogToDevsModule,
        ::platformDeleteProfileDataModule,
    )

private fun profileViewModule() = module { single<ProfileCreationViewModelFactory> { ProfileCreationViewModelFactory } }
