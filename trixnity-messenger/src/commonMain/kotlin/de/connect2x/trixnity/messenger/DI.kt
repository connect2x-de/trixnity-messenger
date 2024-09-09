package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.export.TimelineEventContentToString
import de.connect2x.trixnity.messenger.export.TimelineEventContentToStringImpl
import de.connect2x.trixnity.messenger.export.exportModule
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.multi.platformDeleteProfileDataModule
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.DownloadManagerImpl
import de.connect2x.trixnity.messenger.util.DragAndDropHandler
import de.connect2x.trixnity.messenger.util.DragAndDropHandlerBase
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.SearchImpl
import de.connect2x.trixnity.messenger.util.convertSecretByteArrayModule
import de.connect2x.trixnity.messenger.util.platformCloseAppModule
import de.connect2x.trixnity.messenger.util.platformProcessImageUploadModule
import de.connect2x.trixnity.messenger.util.platformDeleteAccountDataModule
import de.connect2x.trixnity.messenger.util.platformGetDefaultDisplayNameModule
import de.connect2x.trixnity.messenger.util.platformGetSecretByteArrayKey
import de.connect2x.trixnity.messenger.util.platformIsNetworkAvailableModule
import de.connect2x.trixnity.messenger.util.platformPathsModule
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import de.connect2x.trixnity.messenger.util.platformUriCallerModule
import de.connect2x.trixnity.messenger.util.platformUrlHandlerModule
import de.connect2x.trixnity.messenger.viewmodel.MainViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterNewAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.StoreFailureViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.files.ImageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.files.VideoViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangeRoomAvatarViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsHistoryVisibilityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsJoinRulesViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNameViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNotificationsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsSecurityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RelevantTimelineEvents
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReplyToViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportToMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.AudioMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.DefaultTimelineElementRules
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.DefaultTimelineEventSubViewmodelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EmoteMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FallbackMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.FileMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.HistoryVisibilityChangeStatusViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.ImageMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.LocationMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.MemberStatusViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.NoticeMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomAvatarChangeStatusViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomCreatedStatusViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomEncryptionEnabledViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomNameChangeStatusViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomTopicChangeStatusViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TextMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementRules
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineEventSubViewmodelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.UserVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.VideoMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.RichRepliesComputations
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.RichRepliesComputationsImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ThumbnailsImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsOverviewViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.DevicesSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsAllAccountsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsSingleAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfileSingleViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfileViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.platformNotificationSettingsSingleAccountViewModelFactoryModule
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaImpl
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoomImpl
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviter
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviterImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.RoomNameImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopic
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopicImpl
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTrigger
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTriggerImpl
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlockingImpl
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresence
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresenceImpl
import de.connect2x.trixnity.messenger.viewmodel.verification.AcceptSasStartViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerificationsImpl
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.SelectVerificationMethodViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCancelledViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepCompareViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRejectedViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepRequestViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepSuccessViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationStepTimeoutViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerifyAccount
import de.connect2x.trixnity.messenger.viewmodel.verification.VerifyAccountImpl
import io.ktor.client.*
import kotlinx.datetime.Clock
import net.folivo.trixnity.api.client.defaultTrixnityHttpClientFactory
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.store.isEncrypted
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
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
            MatrixClientsImpl(get(), get(), get(), get(), get())
        }

        single<TimelineEventContentToString> { TimelineEventContentToStringImpl(get()) }
        single<Initials> { Initials }
        single<VerifyAccount> { VerifyAccountImpl() }
        single<RelevantTimelineEvents> { RelevantTimelineEvents }

        single<Languages> { DefaultLanguages }
        single<I18n> { object : I18n(get(), get(), get()) {} }
        single<RoomName> { RoomNameImpl(get(), get()) }
        single<RoomTopic> { RoomTopicImpl() }
        single<RoomInviter> { RoomInviterImpl() }
        single<UserBlocking> { UserBlockingImpl() }

        single<DownloadManager> { DownloadManagerImpl() }
        single<Thumbnails> { ThumbnailsImpl() }
        single<RichRepliesComputations> { RichRepliesComputationsImpl(get(), get()) }
        single<DirectRoom> { DirectRoomImpl() }
        single<ActiveVerifications> { ActiveVerificationsImpl() }
        single<UserPresence> { UserPresenceImpl(get()) }
        single<Search> { SearchImpl(get(), get()) }
        single<RunInitialSync> { RunInitialSync }
        single<DragAndDropHandler> { DragAndDropHandlerBase() }

        single<RootViewModelFactory> { RootViewModelFactory }
        single<MainViewModelFactory> { MainViewModelFactory }
        single<SelfVerificationTrigger> { SelfVerificationTriggerImpl() }

        single<AuthorizeUia> { AuthorizeUiaImpl() }
        single<UiaActionConfirmationViewModelFactory> { UiaActionConfirmationViewModelFactory }
        single<UiaStepDummyViewModelFactory> { UiaStepDummyViewModelFactory }
        single<UiaStepPasswordViewModelFactory> { UiaStepPasswordViewModelFactory }
        single<UiaStepRegistrationTokenViewModelFactory> { UiaStepRegistrationTokenViewModelFactory }
        single<UiaStepFallbackViewModelFactory> { UiaStepFallbackViewModelFactory }
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
    exportModule(),

    // platform-specific view models
    platformNotificationSettingsSingleAccountViewModelFactoryModule(),

    // platform-specific implementations
    platformPathsModule(),
    platformCreateRepositoriesModuleModule(),
    platformCreateMediaStoreModule(),
    platformGetSecretByteArrayKey(),
    convertSecretByteArrayModule(),
    platformGetSystemLangModule(),
    platformDeleteAccountDataModule(),
    platformMatrixMessengerSettingsHolderModule(),
    platformSendLogToDevsModule(),
    platformGetDefaultDisplayNameModule(),
    platformIsNetworkAvailableModule(),
    platformCloseAppModule(),
    platformUrlHandlerModule(),
    platformUriCallerModule(),
    platformDeleteProfileDataModule(),
    platformProcessImageUploadModule()
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
    single<DevicesSettingsViewModelFactory> { DevicesSettingsViewModelFactory }
    single<NotificationSettingsAllAccountsViewModelFactory> { NotificationSettingsAllAccountsViewModelFactory }
    single<ProfileViewModelFactory> { ProfileViewModelFactory }
    single<ProfileSingleViewModelFactory> { ProfileSingleViewModelFactory }
    single<UserSettingsViewModelFactory> { UserSettingsViewModelFactory }
    single<PrivacySettingsAllAccountsViewModelFactory> { PrivacySettingsAllAccountsViewModelFactory }
    single<PrivacySettingsSingleAccountViewModelFactory> { PrivacySettingsSingleAccountViewModelFactory }
    single<AppearanceSettingsViewModelFactory> { AppearanceSettingsViewModelFactory }
    single<BlockedContactsSettingsViewModelFactory> { BlockedContactsSettingsViewModelFactory }
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
    single<RoomEncryptionEnabledViewModelFactory> { RoomEncryptionEnabledViewModelFactory }
    single<RoomNameChangeStatusViewModelFactory> { RoomNameChangeStatusViewModelFactory }
    single<RoomTopicChangeStatusViewModelFactory> { RoomTopicChangeStatusViewModelFactory }
    single<HistoryVisibilityChangeStatusViewModelFactory> { HistoryVisibilityChangeStatusViewModelFactory }
    single<TextMessageViewModelFactory> { TextMessageViewModelFactory }
    single<EmoteMessageViewModelFactory> { EmoteMessageViewModelFactory }
    single<LocationMessageViewModelFactory> { LocationMessageViewModelFactory }
    single<NoticeMessageViewModelFactory> { NoticeMessageViewModelFactory }
    single<FallbackMessageViewModelFactory> { FallbackMessageViewModelFactory }
    single<TimelineElementHolderViewModelFactory> { TimelineElementHolderViewModelFactory }
    single<TimelineEventSubViewmodelFactory> { DefaultTimelineEventSubViewmodelFactory() }
    single<UserVerificationViewModelFactory> { UserVerificationViewModelFactory }
    single<RoomAvatarChangeStatusViewModelFactory> { RoomAvatarChangeStatusViewModelFactory }
}

private fun roomViewModels() = module {
    single<RoomViewModelFactory> { RoomViewModelFactory }
}

private fun roomSettingsViewModels() = module {
    single<AddMembersViewModelFactory> { AddMembersViewModelFactory }
    single<ChangePowerLevelViewModelFactory> { ChangePowerLevelViewModelFactory }
    single<ChangeRoomAvatarViewModelFactory> { ChangeRoomAvatarViewModelFactory }
    single<MemberListElementViewModelFactory> { MemberListElementViewModelFactory }
    single<MemberListViewModelFactory> { MemberListViewModelFactory }
    single<PotentialMembersViewModelFactory> { PotentialMembersViewModelFactory }
    single<RoomSettingsViewModelFactory> { RoomSettingsViewModelFactory }
    single<RoomSettingsNameViewModelFactory> { RoomSettingsNameViewModelFactory }
    single<RoomSettingsTopicViewModelFactory> { RoomSettingsTopicViewModelFactory }
    single<RoomSettingsNotificationsViewModelFactory> { RoomSettingsNotificationsViewModelFactory }
    single<RoomSettingsHistoryVisibilityViewModelFactory> { RoomSettingsHistoryVisibilityViewModelFactory }
    single<RoomSettingsJoinRulesViewModelFactory> { RoomSettingsJoinRulesViewModelFactory }
    single<RoomSettingsSecurityViewModelFactory> { RoomSettingsSecurityViewModelFactory }
}

private fun timelineViewModels() = module {
    single<InputAreaViewModelFactory> { InputAreaViewModelFactory }
    single<ReportToMessageViewModelFactory> { ReportToMessageViewModelFactory }
    single<ExportRoomViewModelFactory> { ExportRoomViewModelFactory }
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
