package de.connect2x.messenger.previews.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.IsDebug
import de.connect2x.messenger.compose.view.IsFocused
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.composeViewModule
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.CloseApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.core.model.UserId
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools


@Composable
internal fun InitMessengerPreview(
    koinApplication: KoinApplication = createKoinApplication(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        Platform provides PlatformType.ANDROID,
        IsFocused provides true,
        IsDebug provides false,
        DI provides koinApplication.koin,
    ) {
        MessengerTheme(typography = MaterialTheme.typography) { // TODO we have to disable our own typography here, since there is a bug in compose resources (https://github.com/JetBrains/compose-multiplatform/pull/4965)
            content()
        }
    }
}

private fun createKoinApplication(): KoinApplication {
    val koinApplication = koinApplication {
        modules(
            composeViewModule(),
            module {
                single<I18nView> { object : I18nView(DefaultLanguages, get(), get(), get()) {} }
                single<MatrixMessengerConfiguration> {
                    MatrixMessengerConfiguration()
                }
                single<MatrixMultiMessengerConfiguration> {
                    MatrixMultiMessengerConfiguration()
                }
                single<GetSystemLang> {
                    GetSystemLang { "de" }
                }
                single<MatrixMessengerSettingsHolder> {
                    val settingsHolder: MutableStateFlow<MatrixMessengerSettings?> =
                        MutableStateFlow(MatrixMessengerSettings(mapOf("preferredLang" to JsonPrimitive("en"))))
                    val dummyStorage = object : SettingsStorage {
                        override suspend fun read(): String? = null
                        override suspend fun write(settings: String) {}
                    }
                    val delegate = MatrixMessengerSettingsHolderImpl(dummyStorage, settingsHolder)
                    object : MatrixMessengerSettingsHolder by delegate {
                        override fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> = flow {
                            val hasNoEntry = delegate[userId].first() == null
                            if (hasNoEntry) delegate.update<MatrixMessengerAccountSettingsBase>(userId) { it }
                            emitAll(delegate[userId])
                        }
                    }
                }
                single<CloseApp> {
                    CloseApp { }
                }
            },
        )
    }
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin(koinApplication)
    }
    return koinApplication
}
