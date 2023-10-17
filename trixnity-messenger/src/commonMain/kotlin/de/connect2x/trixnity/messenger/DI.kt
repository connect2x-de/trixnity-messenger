package de.connect2x.trixnity.messenger

import com.russhwolf.settings.Settings
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.MainViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.RoomName
import de.connect2x.trixnity.messenger.viewmodel.RoomNameImpl
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
import net.folivo.trixnity.api.client.defaultTrixnityHttpClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.module.Module
import org.koin.dsl.module

private val log = KotlinLogging.logger {}

data class NamedMatrixClients(val list: StateFlow<List<NamedMatrixClient>>)

fun interface HttpClientConfiguration {
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

fun trixnityMessengerModule() = module {
    single<Clock> { Clock.System }
    single<CoroutineScope> {
        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            log.error(throwable) { "Exception in coroutineContext $coroutineContext" }
        }
        CoroutineScope(Dispatchers.Default + CoroutineName("trixnity-messenger-global") + SupervisorJob() + exceptionHandler)
    }

    single<HttpClientConfiguration> {
        HttpClientConfiguration { defaultTrixnityHttpClient() }
    }
    single<CreateRepositoriesModule> {
        CreateRepositoriesModule { createRepositoriesModule(it) }
    }
    single<CreateMediaStore> {
        CreateMediaStore { createMediaStore(it) }
    }
    single<CreateMatrixClientConfiguration> {
        CreateMatrixClientConfiguration {
            {
                setOwnMessagesAsFullyRead = true
                httpClientFactory = get<HttpClientConfiguration>()()
                lastRelevantEventFilter =
                    { it.content is RoomMessageEventContent || it.content is EncryptedEventContent }
            }
        }
    }

    single<Settings> { createSettings() }
    single<MessengerSettings> { MessengerSettingsImpl(get()) }
    single<GetAccountNames> { GetAccountNamesImpl() }

    single<UrlHandler> { createFilteringUrlHandler(get()) }

    single<Initials> { object : Initials {} }
    single<VerifyAccount> { VerifyAccountImpl() }
    single<IsNetworkAvailable> { object : IsNetworkAvailable {} }
    single<GetFileInfo> { object : GetFileInfo {} }
    single<Secrets> { object : Secrets {} }
    single<RelevantTimelineEvents> { object : RelevantTimelineEvents {} }

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

    single<MainViewModelFactory> { object : MainViewModelFactory {} }

    includes(timelineElementModule)

    includes(connectingViewModels)
    includes(filesViewModels)
    includes(syncViewModels)
    includes(roomListViewModels)
    includes(settingsViewModels)
    includes(timelineElementsViewModels)
    includes(timelineViewModels)
    includes(verificationViewModels)
    includes(roomViewModels)
    includes(roomSettingsViewModels)
}

private val timelineElementModule = module {
    single<TimelineElementRules> { DefaultTimelineElementRules }
}

/*
 * Factories for view models; provide your own factory to change or enhance behaviour of existing view models
 */

private val connectingViewModels = module {
    single<MatrixClientInitializationViewModelFactory> {
        object : MatrixClientInitializationViewModelFactory {}
    }
    single<MatrixClientLogoutViewModelFactory> { object : MatrixClientLogoutViewModelFactory {} }
    single<StoreFailureViewModelFactory> { object : StoreFailureViewModelFactory {} }
    single<AddMatrixAccountViewModelFactory> { object : AddMatrixAccountViewModelFactory {} }
    single<PasswordLoginViewModelFactory> { object : PasswordLoginViewModelFactory {} }
    single<SSOLoginViewModelFactory> { object : SSOLoginViewModelFactory {} }
    single<RegisterNewAccountViewModelFactory> { object : RegisterNewAccountViewModelFactory {} }
}

private val filesViewModels = module {
    single<ImageViewModelFactory> { object : ImageViewModelFactory {} }
    single<VideoViewModelFactory> { object : VideoViewModelFactory {} }
}

private val syncViewModels = module {
    single<SyncViewModelFactory> { object : SyncViewModelFactory {} }
}

private val roomListViewModels = module {
    single<AccountViewModelFactory> { object : AccountViewModelFactory {} }
    single<CreateNewChatViewModelFactory> { object : CreateNewChatViewModelFactory {} }
    single<CreateNewGroupViewModelFactory> { object : CreateNewGroupViewModelFactory {} }
    single<CreateNewRoomViewModelFactory> { object : CreateNewRoomViewModelFactory {} }
    single<SearchGroupViewModelFactory> { object : SearchGroupViewModelFactory {} }
    single<RoomListElementViewModelFactory> { object : RoomListElementViewModelFactory {} }
    single<RoomListViewModelFactory> { object : RoomListViewModelFactory {} }
}

private val settingsViewModels = module {
    single<AccountsOverviewViewModelFactory> { object : AccountsOverviewViewModelFactory {} }
    single<AppInfoViewModelFactory> { object : AppInfoViewModelFactory {} }
    single<AvatarCutterViewModelFactory> { object : AvatarCutterViewModelFactory {} }
    single<ConfigureNotificationsViewModelFactory> {
        object : ConfigureNotificationsViewModelFactory {}
    }
    single<DevicesSettingsViewModelFactory> { object : DevicesSettingsViewModelFactory {} }
    single<NotificationsSettingsViewModelFactory> {
        object : NotificationsSettingsViewModelFactory {}
    }
    single<ProfileViewModelFactory> { object : ProfileViewModelFactory {} }
    single<UserSettingsViewModelFactory> { object : UserSettingsViewModelFactory {} }
    single<PrivacySettingsViewModelFactory> { object : PrivacySettingsViewModelFactory {} }
    single<PrivacySettingViewModelFactory> { object : PrivacySettingViewModelFactory {} }
}

private val timelineElementsViewModels = module {
    single<EncryptedMessageViewModelFactory> { object : EncryptedMessageViewModelFactory {} }
    single<FileMessageViewModelFactory> { object : FileMessageViewModelFactory {} }
    single<ImageMessageViewModelFactory> { object : ImageMessageViewModelFactory {} }
    single<VideoMessageViewModelFactory> { object : VideoMessageViewModelFactory {} }
    single<AudioMessageViewModelFactory> { object : AudioMessageViewModelFactory {} }
    single<MemberStatusViewModelFactory> { object : MemberStatusViewModelFactory {} }
    single<OutboxElementHolderViewModelFactory> { object : OutboxElementHolderViewModelFactory {} }
    single<RedactedMessageViewModelFactory> { object : RedactedMessageViewModelFactory {} }
    single<RoomCreatedStatusViewModelFactory> { object : RoomCreatedStatusViewModelFactory {} }
    single<RoomNameChangeStatusViewModelFactory> {
        object : RoomNameChangeStatusViewModelFactory {}
    }
    single<TextMessageViewModelFactory> { object : TextMessageViewModelFactory {} }
    single<NoticeMessageViewModelFactory> { object : NoticeMessageViewModelFactory {} }
    single<FallbackMessageViewModelFactory> { object : FallbackMessageViewModelFactory {} }
    single<TimelineElementHolderViewModelFactory> { object : TimelineElementHolderViewModelFactory {} }
    single<UserVerificationViewModelFactory> { object : UserVerificationViewModelFactory {} }
}

private val roomViewModels = module {
    single<RoomViewModelFactory> { object : RoomViewModelFactory {} }
}

private val roomSettingsViewModels = module {
    single<AddMembersViewModelFactory> {
        object : AddMembersViewModelFactory {}
    }
    single<ChangePowerLevelViewModelFactory> { object : ChangePowerLevelViewModelFactory {} }
    single<MemberListElementViewModelFactory> { object : MemberListElementViewModelFactory {} }
    single<MemberListViewModelFactory> { object : MemberListViewModelFactory {} }
    single<PotentialMembersViewModelFactory> {
        object : PotentialMembersViewModelFactory {}
    }
    single<RoomSettingsViewModelFactory> { object : RoomSettingsViewModelFactory {} }
    single<RoomSettingsNameViewModelFactory> { object : RoomSettingsNameViewModelFactory {} }
    single<RoomSettingsNotificationsViewModelFactory> { object : RoomSettingsNotificationsViewModelFactory {} }

}
private val timelineViewModels = module {
    single<InputAreaViewModelFactory> { object : InputAreaViewModelFactory {} }
    single<ReplyToViewModelFactory> { object : ReplyToViewModelFactory {} }
    single<RoomHeaderViewModelFactory> { object : RoomHeaderViewModelFactory {} }
    single<SendAttachmentViewModelFactory> { object : SendAttachmentViewModelFactory {} }
    single<TimelineViewModelFactory> { object : TimelineViewModelFactory {} }
    single<TimelineViewModelConfig> {
        object : TimelineViewModelConfig {
            override val autoLoadBefore: Boolean = true
        }
    }
}

private val verificationViewModels = module {
    single<AcceptSasStartViewModelFactory> { object : AcceptSasStartViewModelFactory {} }
    single<BootstrapViewModelFactory> { object : BootstrapViewModelFactory {} }
    single<RedoSelfVerificationViewModelFactory> {
        object : RedoSelfVerificationViewModelFactory {}
    }
    single<SelectVerificationMethodViewModelFactory> {
        object : SelectVerificationMethodViewModelFactory {}
    }
    single<SelfVerificationViewModelFactory> {
        object : SelfVerificationViewModelFactory {}
    }
    single<VerificationStepCancelledViewModelFactory> {
        object : VerificationStepCancelledViewModelFactory {}
    }
    single<VerificationStepCompareViewModelFactory> {
        object : VerificationStepCompareViewModelFactory {}
    }
    single<VerificationStepRejectedViewModelFactory> {
        object : VerificationStepRejectedViewModelFactory {}
    }
    single<VerificationStepRequestViewModelFactory> {
        object : VerificationStepRequestViewModelFactory {}
    }
    single<VerificationStepSuccessViewModelFactory> {
        object : VerificationStepSuccessViewModelFactory {}
    }
    single<VerificationStepTimeoutViewModelFactory> {
        object : VerificationStepTimeoutViewModelFactory {}
    }
    single<VerificationViewModelFactory> { object : VerificationViewModelFactory {} }
}