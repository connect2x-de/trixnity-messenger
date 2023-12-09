package de.connect2x.trixnity.messenger

import com.russhwolf.settings.Settings
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.MainViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.RoomName
import de.connect2x.trixnity.messenger.viewmodel.RoomNameImpl
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.*
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManager
import de.connect2x.trixnity.messenger.viewmodel.files.DownloadManagerImpl
import de.connect2x.trixnity.messenger.viewmodel.files.ImageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.files.VideoViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.initialsync.InitialSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.*
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.*
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.*
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.*
import de.connect2x.trixnity.messenger.viewmodel.roomlist.*
import de.connect2x.trixnity.messenger.viewmodel.settings.*
import de.connect2x.trixnity.messenger.viewmodel.util.*
import de.connect2x.trixnity.messenger.viewmodel.verification.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.Clock
import net.folivo.trixnity.api.client.defaultTrixnityHttpClientFactory
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.store.isEncrypted
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger {}

data class NamedMatrixClients(val list: StateFlow<List<NamedMatrixClient>>)

fun interface HttpUserAgent {
    operator fun invoke(): String
}

fun interface HttpClientFactory {
    operator fun invoke(): (HttpClientConfig<*>.() -> Unit) -> HttpClient
}

// error is only in IDE (@see https://youtrack.jetbrains.com/issue/KTIJ-7642/HMPP-IDE-False-positive-suspend-modifier-is-not-allowed-on-a-single-abstract-member-for-common-code-if-JVM-target-present)
fun interface CreateRepositoriesModule {
    suspend operator fun invoke(accountName: String): Module
}

fun interface CreateMediaStore {
    suspend operator fun invoke(accountName: String): MediaStore
}

fun interface CreateMatrixClientConfiguration {
    operator fun invoke(): MatrixClientConfiguration.() -> Unit
}

fun interface DebugName {
    operator fun invoke(): String
}

fun trixnityMessengerModule() = module {
    single<Clock> { Clock.System }
    single<CoroutineScope> {
        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            log.error(throwable) { "Exception in coroutineContext $coroutineContext" }
        }
        CoroutineScope(Dispatchers.Default + CoroutineName("trixnity-messenger-global") + SupervisorJob() + exceptionHandler)
    }

    single<HttpClientFactory> {
        val userAgent = getOrNull<HttpUserAgent>()?.invoke()
        HttpClientFactory {
            if (userAgent != null) defaultTrixnityHttpClientFactory(userAgent = userAgent)
            else defaultTrixnityHttpClientFactory(userAgent = userAgent)
        }
    }
    single<Secrets> { Secrets }
    single<DbPassword> {
        DbPasswordImpl(get())
    }
    single<CreateRepositoriesModule> {
        CreateRepositoriesModule { createRepositoriesModule(it, get()) }
    }
    single<CreateMediaStore> {
        CreateMediaStore { createMediaStore(it) }
    }
    single<CreateMatrixClientConfiguration> {
        CreateMatrixClientConfiguration {
            {
                name = getOrNull<DebugName>()?.invoke()
                setOwnMessagesAsFullyRead = true
                httpClientFactory = get<HttpClientFactory>()()
                lastRelevantEventFilter =
                    { it.content is RoomMessageEventContent || it.isEncrypted }
            }
        }
    }

    single<FilteringUrlHandler> {
        val urlHandler = getOrNull<UrlHandler>() ?: UrlHandlerBase()
        FilteringUrlHandler(urlHandler, get())
    }
    single<Settings> { createSettings() }
    single<MessengerSettings> { MessengerSettingsImpl(get()) }
    single<GetAccountNames> { GetAccountNamesImpl() }

    single<Initials> { Initials }
    single<VerifyAccount> { VerifyAccountImpl() }
    single<IsNetworkAvailable> { IsNetworkAvailable }
    single<GetFileInfo> { GetFileInfo }
    single<RelevantTimelineEvents> { RelevantTimelineEvents }

    single<Languages> { DefaultLanguages }
    single<I18n> { object : I18n(get(), get()) {} }
    single<RoomName> { RoomNameImpl(get(), get()) }
    single<RoomInviter> { RoomInviterImpl() }
    single<UserBlocking> { UserBlockingImpl() }

    single<DownloadManager> { DownloadManagerImpl() }
    single<Thumbnails> { ThumbnailsImpl() }
    single<RichRepliesComputations> {
        RichRepliesComputationsImpl(
            get(),
            get(),
            FileNameComputations(get())
        )
    }
    single<DirectRoom> { DirectRoomImpl() }
    single<GetActiveVerification> { GetActiveVerificationImpl() }
    single<ActiveVerifications> { ActiveVerificationsImpl() }
    single<UserPresence> { UserPresenceImpl(get()) }
    single<Search> { SearchImpl(get(), get()) }
    single<RunInitialSync> {
        object : RunInitialSync {
            override fun invoke(accountName: String): Flow<Boolean> {
                // on Desktop, when the scope of the caller (a view model) is ended, the app in most cases is ended as well
                // -> it is OK to cancel the initial sync in this case as the JVM is not running anymore
                return channelFlow {
                    val matrixClient =
                        (get<NamedMatrixClients>().list.value.find { it.accountName == accountName }?.matrixClient?.value
                            ?: throw IllegalStateException("Cannot find account '$accountName'."))
                    log.info { "matrixClient: $matrixClient" }
                    send(
                        InitialSync.run(matrixClient)
                    )
                }
            }
        }
    }

    single<RootViewModelFactory> { RootViewModelFactory }
    single<MainViewModelFactory> { MainViewModelFactory }

    includes(timelineElementModule())

    includes(connectingViewModels())
    includes(filesViewModels())
    includes(syncViewModels())
    includes(roomListViewModels())
    includes(settingsViewModels())
    includes(timelineElementsViewModels())
    includes(timelineViewModels())
    includes(verificationViewModels())
    includes(roomViewModels())
    includes(roomSettingsViewModels())
}

private fun timelineElementModule() = module {
    single<TimelineElementRules> { DefaultTimelineElementRules }
}

/*
 * Factories for view models; provide your own factory to change or enhance behaviour of existing view models
 */

private fun connectingViewModels() = module {
    single<MatrixClientInitializationViewModelFactory> {
        MatrixClientInitializationViewModelFactory
    }
    single<MatrixClientLogoutViewModelFactory> { MatrixClientLogoutViewModelFactory }
    single<StoreFailureViewModelFactory> { StoreFailureViewModelFactory }
    single<AddMatrixAccountViewModelFactory> { AddMatrixAccountViewModelFactory }
    single<PasswordLoginViewModelFactory> { PasswordLoginViewModelFactory }
    single<SSOLoginViewModelFactory> { SSOLoginViewModelFactory }
    single<RegisterNewAccountViewModelFactory> { RegisterNewAccountViewModelFactory }
}

private fun filesViewModels() = module {
    single<ImageViewModelFactory> { ImageViewModelFactory }
    single<VideoViewModelFactory> { VideoViewModelFactory }
}

private fun syncViewModels() = module {
    single<SyncViewModelFactory> { SyncViewModelFactory }
}

private fun roomListViewModels() = module {
    single<AccountViewModelFactory> { AccountViewModelFactory }
    single<CreateNewChatViewModelFactory> { CreateNewChatViewModelFactory }
    single<CreateNewGroupViewModelFactory> { CreateNewGroupViewModelFactory }
    single<CreateNewRoomViewModelFactory> { CreateNewRoomViewModelFactory }
    single<SearchGroupViewModelFactory> { SearchGroupViewModelFactory }
    single<RoomListElementViewModelFactory> { RoomListElementViewModelFactory }
    single<RoomListViewModelFactory> { RoomListViewModelFactory }
}

private fun settingsViewModels() = module {
    single<AccountsOverviewViewModelFactory> { AccountsOverviewViewModelFactory }
    single<AppInfoViewModelFactory> { AppInfoViewModelFactory }
    single<AvatarCutterViewModelFactory> { AvatarCutterViewModelFactory }
    single<ConfigureNotificationsViewModelFactory> { ConfigureNotificationsViewModelFactory }
    single<DevicesSettingsViewModelFactory> { DevicesSettingsViewModelFactory }
    single<NotificationsSettingsViewModelFactory> { NotificationsSettingsViewModelFactory }
    single<ProfileViewModelFactory> { ProfileViewModelFactory }
    single<UserSettingsViewModelFactory> { UserSettingsViewModelFactory }
    single<PrivacySettingsViewModelFactory> { PrivacySettingsViewModelFactory }
    single<PrivacySettingViewModelFactory> { PrivacySettingViewModelFactory }
}

private fun timelineElementsViewModels() = module {
    single<EncryptedMessageViewModelFactory> { EncryptedMessageViewModelFactory }
    single<FileMessageViewModelFactory> { FileMessageViewModelFactory }
    single<ImageMessageViewModelFactory> { ImageMessageViewModelFactory }
    single<VideoMessageViewModelFactory> { VideoMessageViewModelFactory }
    single<AudioMessageViewModelFactory> { AudioMessageViewModelFactory }
    single<MemberStatusViewModelFactory> { MemberStatusViewModelFactory }
    single<OutboxElementHolderViewModelFactory> { OutboxElementHolderViewModelFactory }
    single<RedactedMessageViewModelFactory> { RedactedMessageViewModelFactory }
    single<RoomCreatedStatusViewModelFactory> { RoomCreatedStatusViewModelFactory }
    single<RoomNameChangeStatusViewModelFactory> { RoomNameChangeStatusViewModelFactory }
    single<TextMessageViewModelFactory> { TextMessageViewModelFactory }
    single<NoticeMessageViewModelFactory> { NoticeMessageViewModelFactory }
    single<FallbackMessageViewModelFactory> { FallbackMessageViewModelFactory }
    single<TimelineElementHolderViewModelFactory> { TimelineElementHolderViewModelFactory }
    single<UserVerificationViewModelFactory> { UserVerificationViewModelFactory }
}

private fun roomViewModels() = module {
    single<RoomViewModelFactory> { RoomViewModelFactory }
}

private fun roomSettingsViewModels() = module {
    single<AddMembersViewModelFactory> { AddMembersViewModelFactory }
    single<ChangePowerLevelViewModelFactory> { ChangePowerLevelViewModelFactory }
    single<MemberListElementViewModelFactory> { MemberListElementViewModelFactory }
    single<MemberListViewModelFactory> { MemberListViewModelFactory }
    single<PotentialMembersViewModelFactory> { PotentialMembersViewModelFactory }
    single<RoomSettingsViewModelFactory> { RoomSettingsViewModelFactory }
    single<RoomSettingsNameViewModelFactory> { RoomSettingsNameViewModelFactory }
    single<RoomSettingsNotificationsViewModelFactory> { RoomSettingsNotificationsViewModelFactory }

}

private fun timelineViewModels() = module {
    single<InputAreaViewModelFactory> { InputAreaViewModelFactory }
    single<ReplyToViewModelFactory> { ReplyToViewModelFactory }
    single<RoomHeaderViewModelFactory> { RoomHeaderViewModelFactory }
    single<SendAttachmentViewModelFactory> { SendAttachmentViewModelFactory }
    single<TimelineViewModelFactory> { TimelineViewModelFactory }
    single<TimelineViewModelConfig> {
        object : TimelineViewModelConfig {
            override val autoLoadBefore: Boolean = true
        }
    }
}

private fun verificationViewModels() = module {
    single<AcceptSasStartViewModelFactory> { AcceptSasStartViewModelFactory }
    single<BootstrapViewModelFactory> { BootstrapViewModelFactory }
    single<RedoSelfVerificationViewModelFactory> { RedoSelfVerificationViewModelFactory }
    single<SelectVerificationMethodViewModelFactory> { SelectVerificationMethodViewModelFactory }
    single<SelfVerificationViewModelFactory> { SelfVerificationViewModelFactory }
    single<VerificationStepCancelledViewModelFactory> { VerificationStepCancelledViewModelFactory }
    single<VerificationStepCompareViewModelFactory> { VerificationStepCompareViewModelFactory }
    single<VerificationStepRejectedViewModelFactory> { VerificationStepRejectedViewModelFactory }
    single<VerificationStepRequestViewModelFactory> { VerificationStepRequestViewModelFactory }
    single<VerificationStepSuccessViewModelFactory> { VerificationStepSuccessViewModelFactory }
    single<VerificationStepTimeoutViewModelFactory> { VerificationStepTimeoutViewModelFactory }
    single<VerificationViewModelFactory> { VerificationViewModelFactory }
}