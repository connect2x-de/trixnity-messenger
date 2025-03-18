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
import de.connect2x.trixnity.messenger.util.RelevantTimelineEvents
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.SearchImpl
import de.connect2x.trixnity.messenger.util.SharedDataHandler
import de.connect2x.trixnity.messenger.util.SharedDataHandlerImpl
import de.connect2x.trixnity.messenger.util.convertSecretByteArrayModule
import de.connect2x.trixnity.messenger.util.JoinRoom
import de.connect2x.trixnity.messenger.util.JoinRoomImpl
import de.connect2x.trixnity.messenger.util.platformCloseAppModule
import de.connect2x.trixnity.messenger.util.platformDeleteAccountDataModule
import de.connect2x.trixnity.messenger.util.platformGetDefaultDisplayNameModule
import de.connect2x.trixnity.messenger.util.platformGetSecretByteArrayKey
import de.connect2x.trixnity.messenger.util.platformIsNetworkAvailableModule
import de.connect2x.trixnity.messenger.util.platformMinimizeAppModule
import de.connect2x.trixnity.messenger.util.platformPathsModule
import de.connect2x.trixnity.messenger.util.platformProcessImageUploadModule
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import de.connect2x.trixnity.messenger.util.platformUriCallerModule
import de.connect2x.trixnity.messenger.util.platformUrlHandlerModule
import de.connect2x.trixnity.messenger.viewmodel.MainViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModelFactory
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
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsAliasViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsHistoryVisibilityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsJoinRulesViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNameViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNotificationsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsSecurityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportToMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedErrorTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EncryptedWaitTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactorySelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactorySelectorImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.AudioRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EmoteRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.FileRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.ImageRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.LocationRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.NoticeRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.TextRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationRequestRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VideoRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.AvatarStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CanonicalAliasStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CreateStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.EncryptionStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.HistoryVisibilityStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.MemberStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.NameStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.TopicStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ThumbnailsImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModelFactory
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
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareDataViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaImpl
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepEmailIdentityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepMsisdnViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoom
import de.connect2x.trixnity.messenger.viewmodel.util.DirectRoomImpl
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReactionsImpl
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReaders
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReadersImpl
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviter
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviterImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.RoomNameImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopic
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopicImpl
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlockingImpl
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresence
import de.connect2x.trixnity.messenger.viewmodel.util.UserPresenceImpl
import de.connect2x.trixnity.messenger.viewmodel.verification.AcceptSasStartViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerificationsImpl
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.RedoSelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.SelectVerificationMethodViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTrigger
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationTriggerImpl
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.ModuleFactory
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientFactory
import net.folivo.trixnity.core.serialization.events.DefaultEventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMappings
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module


fun interface ConfigureMatrixClientConfiguration {
    operator fun MatrixClientConfiguration.invoke()
}

fun interface DebugName {
    operator fun invoke(): String
}

fun createTrixnityMessengerDefaultModuleFactories(): List<ModuleFactory> = listOf(
    {
        module {
            single<Clock> { Clock.System }
            single<TimeZone> { TimeZone.currentSystemDefault() }

            single<ConfigureMatrixClientConfiguration>(named("DefaultConfigureMatrixClientConfiguration")) {
                val config = get<MatrixMessengerConfiguration>()
                val relevantTimelineEvents = get<RelevantTimelineEvents>()
                val eventContentSerializerMappings = getAll<EventContentSerializerMappings>()
                ConfigureMatrixClientConfiguration {
                    name = getOrNull<DebugName>()?.invoke()
                    markOwnMessageAsRead = true
                    httpClientEngine = config.httpClientEngine
                    httpClientConfig = config.httpClientConfig
                    lastRelevantEventFilter =
                        { relevantTimelineEvents.isRelevantTimelineEvent(it.content) }
                    if (eventContentSerializerMappings.isNotEmpty()) {
                        modulesFactories += {
                            module {
                                single<EventContentSerializerMappings> {
                                    eventContentSerializerMappings
                                        .fold(DefaultEventContentSerializerMappings) { a, b -> a + b }
                                }
                            }
                        }
                    }
                    getOrNull<MatrixClientServerApiClientFactory>()?.let {
                        matrixClientServerApiClientFactory = it
                    }
                }
            }

            single<MatrixClientFactory> {
                MatrixClientFactoryImpl(get(), get(), getAll())
            }
            single<MatrixClients> {
                MatrixClientsImpl(get(), get(), get(), get(), get())
            }

            single<TimelineEventContentToString> { TimelineEventContentToStringImpl(get()) }
            single<Initials> { Initials }
            single<VerifyAccount> { VerifyAccountImpl() }
            single<RelevantTimelineEvents> { RelevantTimelineEvents }

            single<Languages> { DefaultLanguages }
            single<I18n> { I18n(get(), get(), get(), get()) }
            single<RoomName> { RoomNameImpl(get(), get()) }
            single<RoomTopic> { RoomTopicImpl() }
            single<RoomInviter> { RoomInviterImpl() }
            single<UserBlocking> { UserBlockingImpl() }
            single<JoinRoom> { JoinRoomImpl() }

            single<DownloadManager> { DownloadManagerImpl() }
            single<Thumbnails> { ThumbnailsImpl() }
            single<DirectRoom> { DirectRoomImpl() }
            single<ActiveVerifications> { ActiveVerificationsImpl() }
            single<UserPresence> { UserPresenceImpl(get()) }
            single<Search> { SearchImpl(get(), get()) }
            single<RunInitialSync> { RunInitialSync }
            single<DragAndDropHandler> { DragAndDropHandlerBase() }
            single<AccountSetupViewModelFactory> { AccountSetupViewModelFactory }

            single<RootViewModelFactory> { RootViewModelFactory }
            single<MainViewModelFactory> { MainViewModelFactory }
            single<SelfVerificationTrigger> { SelfVerificationTriggerImpl() }

            single<AuthorizeUia> { AuthorizeUiaImpl() }
            single<UiaActionConfirmationViewModelFactory> { UiaActionConfirmationViewModelFactory }
            single<UiaStepDummyViewModelFactory> { UiaStepDummyViewModelFactory }
            single<UiaStepPasswordViewModelFactory> { UiaStepPasswordViewModelFactory }
            single<UiaStepRegistrationTokenViewModelFactory> { UiaStepRegistrationTokenViewModelFactory }
            single<UiaStepEmailIdentityViewModelFactory> { UiaStepEmailIdentityViewModelFactory }
            single<UiaStepMsisdnViewModelFactory> { UiaStepMsisdnViewModelFactory }
            single<UiaStepFallbackViewModelFactory> { UiaStepFallbackViewModelFactory }

            single<ShareDataViewModelFactory> { ShareDataViewModelFactory }
            single<SharedDataHandler> { SharedDataHandlerImpl() }
        }
    },
    ::connectingViewModels,
    ::syncViewModels,
    ::roomListViewModels,
    ::settingsViewModels,
    ::timelineElementViewModels,
    ::timelineViewModels,
    ::verificationViewModels,
    ::roomViewModels,
    ::roomSettingsViewModels,
    ::exportModule,

    // Platform-specific view models:
    ::platformNotificationSettingsSingleAccountViewModelFactoryModule,

    // Platform-specific implementations:
    ::platformModule,
    ::platformPathsModule,
    ::platformCreateRepositoriesModuleModule,
    ::platformCreateMediaStoreModule,
    ::platformGetSecretByteArrayKey,
    ::convertSecretByteArrayModule,
    ::platformGetSystemLangModule,
    ::platformDeleteAccountDataModule,
    ::platformMatrixMessengerSettingsHolderModule,
    ::platformSendLogToDevsModule,
    ::platformGetDefaultDisplayNameModule,
    ::platformIsNetworkAvailableModule,
    ::platformCloseAppModule,
    ::platformMinimizeAppModule,
    ::platformUrlHandlerModule,
    ::platformUriCallerModule,
    ::platformDeleteProfileDataModule,
    ::platformProcessImageUploadModule,
)

/*
 * Factories for view models: Provide your own factory to
 * change or enhance behaviours of existing view models.
 */

private fun connectingViewModels() = module {
    single<MatrixClientInitializationViewModelFactory> {
        MatrixClientInitializationViewModelFactory
    }
    single<RemoveMatrixAccountViewModelFactory> { RemoveMatrixAccountViewModelFactory }
    single<MatrixClientInitializationFailureViewModelFactory> { MatrixClientInitializationFailureViewModelFactory }
    single<AddMatrixAccountViewModelFactory> { AddMatrixAccountViewModelFactory }
    single<PasswordLoginViewModelFactory> { PasswordLoginViewModelFactory }
    single<SSOLoginViewModelFactory> { SSOLoginViewModelFactory }
    single<RegisterMatrixAccountViewModelFactory> { RegisterMatrixAccountViewModelFactory }
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

inline fun <reified F : TimelineElementViewModelFactory<*>> Module.timelineElementViewModelFactory(
    noinline definition: Scope.(ParametersHolder) -> F,
) = single<F>(named<F>(), definition = definition).bind<TimelineElementViewModelFactory<*>>()

private fun timelineElementViewModels() = module {
    // Message:
    timelineElementViewModelFactory<FileRoomMessageTimelineElementViewModelFactory> { FileRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<ImageRoomMessageTimelineElementViewModelFactory> { ImageRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<VideoRoomMessageTimelineElementViewModelFactory> { VideoRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<AudioRoomMessageTimelineElementViewModelFactory> { AudioRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<TextRoomMessageTimelineElementViewModelFactory> { TextRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<NoticeRoomMessageTimelineElementViewModelFactory> { NoticeRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<EmoteRoomMessageTimelineElementViewModelFactory> { EmoteRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<LocationRoomMessageTimelineElementViewModelFactory> { LocationRoomMessageTimelineElementViewModelFactory }
    timelineElementViewModelFactory<VerificationRequestRoomMessageTimelineElementViewModelFactory> { VerificationRequestRoomMessageTimelineElementViewModelFactory }

    // State:
    timelineElementViewModelFactory<CreateStateTimelineElementViewModelFactory> { CreateStateTimelineElementViewModelFactory }
    timelineElementViewModelFactory<NameStateTimelineElementViewModelFactory> { NameStateTimelineElementViewModelFactory }
    timelineElementViewModelFactory<TopicStateTimelineElementViewModelFactory> { TopicStateTimelineElementViewModelFactory }
    timelineElementViewModelFactory<AvatarStateTimelineElementViewModelFactory> { AvatarStateTimelineElementViewModelFactory }
    timelineElementViewModelFactory<MemberStateTimelineElementViewModelFactory> { MemberStateTimelineElementViewModelFactory }
    timelineElementViewModelFactory<CanonicalAliasStateTimelineElementViewModelFactory> { CanonicalAliasStateTimelineElementViewModelFactory }
    timelineElementViewModelFactory<HistoryVisibilityStateTimelineElementViewModelFactory> { HistoryVisibilityStateTimelineElementViewModelFactory }
    timelineElementViewModelFactory<EncryptionStateTimelineElementViewModelFactory> { EncryptionStateTimelineElementViewModelFactory }

    // Common:
    timelineElementViewModelFactory<RedactedTimelineElementViewModelFactory> { RedactedTimelineElementViewModelFactory }

    // Select from timelineElementViewModelFactory:
    single<EncryptedWaitTimelineElementViewModelFactory> { EncryptedWaitTimelineElementViewModelFactory }
    single<EncryptedErrorTimelineElementViewModelFactory> { EncryptedErrorTimelineElementViewModelFactory }
    single<TimelineElementViewModelFactorySelector> {
        TimelineElementViewModelFactorySelectorImpl(getAll(), get(), get())
    }
    single<TimelineElementViewModelFactorySelector> {
        TimelineElementViewModelFactorySelectorImpl(getAll(), get(), get())
    }

    single<TimelineElementHolderViewModelFactory> { TimelineElementHolderViewModelFactory }
    single<OutboxElementHolderViewModelFactory> { OutboxElementHolderViewModelFactory }
}

private fun roomViewModels() = module {
    single<RoomViewModelFactory> { RoomViewModelFactory }
    single<GetEventReactions> { GetEventReactionsImpl() }
    single<GetEventReaders> { GetEventReadersImpl() }
}

private fun roomSettingsViewModels() = module {
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
    single<RoomSettingsAliasViewModelFactory> { RoomSettingsAliasViewModelFactory }
    single<RoomSettingsJoinRulesViewModelFactory> { RoomSettingsJoinRulesViewModelFactory }
    single<RoomSettingsSecurityViewModelFactory> { RoomSettingsSecurityViewModelFactory }
    single<TimelineElementMetadataViewModelFactory> { TimelineElementMetadataViewModelFactory }
    single<UserProfileViewModelFactory> { UserProfileViewModelFactory }
    single<AddMembersViewModelFactory> { AddMembersViewModelFactory }
    single<ExportRoomViewModelFactory> { ExportRoomViewModelFactory }
}

private fun timelineViewModels() = module {
    single<InputAreaViewModelFactory> { InputAreaViewModelFactory }
    single<ReportToMessageViewModelFactory> { ReportToMessageViewModelFactory }
    single<RoomHeaderViewModelFactory> { RoomHeaderViewModelFactory }
    single<SendAttachmentViewModelFactory> { SendAttachmentViewModelFactory }
    single<TimelineViewModelFactory> { TimelineViewModelFactory }
}

private fun verificationViewModels() = module {
    single<AcceptSasStartViewModelFactory> { AcceptSasStartViewModelFactory }
    single<CrossSigningBootstrapViewModelFactory> { CrossSigningBootstrapViewModelFactory }
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

expect fun platformModule(): Module
