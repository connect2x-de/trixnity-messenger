package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.util.*
import de.connect2x.trixnity.messenger.viewmodel.MainViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.*
import de.connect2x.trixnity.messenger.viewmodel.files.ImageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.files.VideoViewModelFactory
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
import io.ktor.client.*
import kotlinx.datetime.Clock
import net.folivo.trixnity.api.client.defaultTrixnityHttpClientFactory
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.store.isEncrypted
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.module.Module
import org.koin.dsl.module

fun interface HttpUserAgent {
    operator fun invoke(): String
}

fun interface HttpClientFactory {
    operator fun invoke(): (HttpClientConfig<*>.() -> Unit) -> HttpClient
}

fun interface CreateMatrixClientConfiguration {
    operator fun invoke(): MatrixClientConfiguration.() -> Unit
}

fun interface DebugName {
    operator fun invoke(): String
}

fun createDefaultTrixnityMessengerModules() = listOf(
    module {
        single<Clock> { Clock.System }

        single<HttpClientFactory> {
            val userAgent = getOrNull<HttpUserAgent>()?.invoke()
            HttpClientFactory {
                if (userAgent != null) defaultTrixnityHttpClientFactory(userAgent = userAgent)
                else defaultTrixnityHttpClientFactory(userAgent = userAgent)
            }
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

        single<MatrixClientFactory> {
            MatrixClientFactoryImpl(get(), get(), get(), get())
        }
        single<MatrixClients> {
            MatrixClientsImpl(get(), get(), get(), get())
        }

        single<Initials> { Initials }
        single<VerifyAccount> { VerifyAccountImpl() }
        single<RelevantTimelineEvents> { RelevantTimelineEvents }

        single<Languages> { DefaultLanguages }
        single<I18n> { object : I18n(get(), get(), get()) {} }
        single<RoomName> { RoomNameImpl(get(), get()) }
        single<RoomInviter> { RoomInviterImpl() }
        single<UserBlocking> { UserBlockingImpl() }
        single<ComputeFileName> { ComputeFileNameImpl(get()) }

        single<DownloadManager> { DownloadManagerImpl() }
        single<Thumbnails> { ThumbnailsImpl() }
        single<RichRepliesComputations> { RichRepliesComputationsImpl(get(), get(), get()) }
        single<DirectRoom> { DirectRoomImpl() }
        single<ActiveVerifications> { ActiveVerificationsImpl() }
        single<UserPresence> { UserPresenceImpl(get()) }
        single<Search> { SearchImpl(get(), get()) }
        single<RunInitialSync> { RunInitialSync }
        single<DragAndDropHandler> { DragAndDropHandlerBase() }

        single<RootViewModelFactory> { RootViewModelFactory }
        single<MainViewModelFactory> { MainViewModelFactory }
    },
    timelineElementModule(),
    connectingViewModels(),
    filesViewModels(),
    syncViewModels(),
    roomListViewModels(),
    settingsViewModels(),
    timelineElementsViewModels(),
    timelineViewModels(),
    verificationViewModels(),
    roomViewModels(),
    roomSettingsViewModels(),
    // platform-specific implementations
    generalPlatformModule(),
    platformCreateRepositoriesModuleModule(),
    platformCreateMediaStoreModule(),
    platformConvertSecretByteArray(),
    platformGetSystemLangModule(),
    platformGetFileInfoModule(),
    platformDeleteAccountDataModule(),
    platformMatrixMessengerSettingsHolderModule(),
    platformSendLogToDevsModule(),
    platformGetDefaultDisplayNameModule(),
    platformIsNetworkAvailableModule(),
    platformCloseAppModule(),
    platformUrlHandlerModule(),
)

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
    single<RemoveMatrixAccountViewModelFactory> { RemoveMatrixAccountViewModelFactory }
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

expect fun generalPlatformModule(): Module