package de.connect2x.trixnity.messenger.integrationtests.util

import com.russhwolf.settings.MapSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettingsImpl
import org.koin.dsl.module

fun settingsModule() = module {
    single<MessengerSettings> { MessengerSettingsImpl(MapSettings()) }
}