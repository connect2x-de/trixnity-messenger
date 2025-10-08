package de.connect2x.trixnity.messenger.compose.view.previews.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.Platform
import de.connect2x.trixnity.messenger.compose.view.PlatformType
import de.connect2x.trixnity.messenger.compose.view.composeViewModule
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.root.IsSinglePane
import de.connect2x.trixnity.messenger.compose.view.theme.MessengerTheme
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerFeatures
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.GetSystemLang
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.DownloadManagerImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ThumbnailsImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonPrimitive
import net.folivo.trixnity.core.model.UserId
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
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
        IsSinglePane provides false,
        DI provides koinApplication.koin,
    ) {
        MessengerTheme(typography = MaterialTheme.typography) { // TODO we have to disable our own typography here, since there is a bug in compose resources (https://github.com/JetBrains/compose-multiplatform/pull/4965)
            content()
        }
    }
}

fun createKoinApplication(): KoinApplication {
    val koinApplication = koinApplication {
        modules(
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
                single<Languages> { DefaultLanguages }
                single<TimeZone> { TimeZone.currentSystemDefault() }
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
                single<DownloadManager> { DownloadManagerImpl() }
                single<Thumbnails> { ThumbnailsImpl() }
            },
            composeViewModule(MatrixMessengerConfiguration(features = MatrixMessengerFeatures(enablePdfReader = true))),
        )
        logger(PrintLogger(level = Level.DEBUG))
    }
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin(koinApplication)
    }
    return koinApplication
}
