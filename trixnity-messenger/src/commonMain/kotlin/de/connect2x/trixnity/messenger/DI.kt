package de.connect2x.trixnity.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.client.ModuleFactory
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.export.TimelineEventContentToString
import de.connect2x.trixnity.messenger.export.TimelineEventContentToStringImpl
import de.connect2x.trixnity.messenger.export.exportModule
import de.connect2x.trixnity.messenger.i18n.DefaultLanguages
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.i18n.Languages
import de.connect2x.trixnity.messenger.i18n.platformGetSystemLangModule
import de.connect2x.trixnity.messenger.media.AudioRecorderHolder
import de.connect2x.trixnity.messenger.media.AudioRecorderImpl
import de.connect2x.trixnity.messenger.media.PlatformAudioRecorder
import de.connect2x.trixnity.messenger.multi.platformDeleteProfileDataModule
import de.connect2x.trixnity.messenger.notification.notificationModule
import de.connect2x.trixnity.messenger.search.provider.SearchProviderFactory
import de.connect2x.trixnity.messenger.search.provider.SearchProviderSorter
import de.connect2x.trixnity.messenger.search.provider.SearchProviderSorterImpl
import de.connect2x.trixnity.messenger.search.user.homeserver.HomeserverSearchProviderFactory
import de.connect2x.trixnity.messenger.secrets.secretsModule
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.BackHandlerImpl
import de.connect2x.trixnity.messenger.util.DownloadManager
import de.connect2x.trixnity.messenger.util.DownloadManagerImpl
import de.connect2x.trixnity.messenger.util.DragAndDropHandler
import de.connect2x.trixnity.messenger.util.DragAndDropHandlerBase
import de.connect2x.trixnity.messenger.util.EnterRoom
import de.connect2x.trixnity.messenger.util.EnterRoomImpl
import de.connect2x.trixnity.messenger.util.InformationMarkdownFlavour
import de.connect2x.trixnity.messenger.util.InformationMarkdownFlavourImpl
import de.connect2x.trixnity.messenger.util.LeaveRoom
import de.connect2x.trixnity.messenger.util.LeaveRoomImpl
import de.connect2x.trixnity.messenger.util.MatrixMarkdownFlavour
import de.connect2x.trixnity.messenger.util.MatrixMarkdownFlavourImpl
import de.connect2x.trixnity.messenger.util.Search
import de.connect2x.trixnity.messenger.util.SearchImpl
import de.connect2x.trixnity.messenger.util.SharedDataHandler
import de.connect2x.trixnity.messenger.util.SharedDataHandlerImpl
import de.connect2x.trixnity.messenger.util.audioMetadataFactoryModule
import de.connect2x.trixnity.messenger.util.platformCloseAppModule
import de.connect2x.trixnity.messenger.util.platformDeleteAccountDataModule
import de.connect2x.trixnity.messenger.util.platformGetDefaultDisplayNameModule
import de.connect2x.trixnity.messenger.util.platformGetImageDimensionsModule
import de.connect2x.trixnity.messenger.util.platformIsNetworkAvailableModule
import de.connect2x.trixnity.messenger.util.platformMinimizeAppModule
import de.connect2x.trixnity.messenger.util.platformPathsModule
import de.connect2x.trixnity.messenger.util.platformProcessImageUploadModule
import de.connect2x.trixnity.messenger.util.platformSendLogToDevsModule
import de.connect2x.trixnity.messenger.util.platformStringsModule
import de.connect2x.trixnity.messenger.util.platformUriCallerModule
import de.connect2x.trixnity.messenger.util.platformUriHandlerModule
import de.connect2x.trixnity.messenger.viewmodel.MainViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.RootViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.AddMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationFailureViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.MatrixClientInitializationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2DeviceAuthorizationLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.PasswordLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RegisterMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.RemoveMatrixAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOLoginViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSync
import de.connect2x.trixnity.messenger.viewmodel.initialsync.RunInitialSyncImpl
import de.connect2x.trixnity.messenger.viewmodel.initialsync.SyncViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.media.MediaPlayerViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersNewSearchViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.AddMembersViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangePowerLevelViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ChangeRoomAvatarViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExportRoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MemberListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersNewSearchViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PotentialMembersViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.room.settings.PowerlevelViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomDevInfoViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsAliasViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsHistoryVisibilityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsJoinRulesViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNameViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsNotificationsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsSecurityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsTopicViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementDevInfoViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.settings.UserProfileViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.AudioRecordingAreaViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.InputAreaViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.ReportToMessageViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.RoomHeaderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.SendAttachmentViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactorySelector
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactorySelectorImpl
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.AudioRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EmoteRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EncryptedErrorTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.EncryptedWaitTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.FileRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.ImageRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.LocationRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.NoticeRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.TextRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationCancelTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationDoneTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationRequestRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VideoRoomMessageTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.AvatarStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CanonicalAliasStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.CreateStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.EncryptionStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.HistoryVisibilityStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.MemberStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.NameStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.PowerLevelsTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.TombstoneStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.state.TopicStateTimelineElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.Thumbnails
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.ThumbnailsImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatNewSearchViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewChatViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupNewSearchViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewGroupViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.CreateNewRoomViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.roomlist.SearchGroupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.search.SearchViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSingleViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AppInfoViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.AvatarCutterViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.BlockedContactsSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceSettingsAllAccountsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.DeviceSettingsSingleAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsAllAccountsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsAllAccountsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.PrivacySettingsSingleAccountViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsSingleViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.sharing.ShareDataViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaImpl
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaActionConfirmationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepDummyViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepEmailIdentityViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepMsisdnViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepOAuth2ViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReactionsImpl
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReaders
import de.connect2x.trixnity.messenger.viewmodel.util.GetEventReadersImpl
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.InitialsImpl
import de.connect2x.trixnity.messenger.viewmodel.util.IsOneToOneRoom
import de.connect2x.trixnity.messenger.viewmodel.util.IsOneToOneRoomImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviter
import de.connect2x.trixnity.messenger.viewmodel.util.RoomInviterImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomName
import de.connect2x.trixnity.messenger.viewmodel.util.RoomNameImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomPresence
import de.connect2x.trixnity.messenger.viewmodel.util.RoomPresenceImpl
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopic
import de.connect2x.trixnity.messenger.viewmodel.util.RoomTopicImpl
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlocking
import de.connect2x.trixnity.messenger.viewmodel.util.UserBlockingImpl
import de.connect2x.trixnity.messenger.viewmodel.verification.AcceptSasStartViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerifications
import de.connect2x.trixnity.messenger.viewmodel.verification.ActiveVerificationsImpl
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModelFactory
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
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

private val log = Logger("de.connect2x.trixnity.messenger.createTrixnityMessengerDefaultModuleFactories")

fun createTrixnityMessengerDefaultModuleFactories(): List<ModuleFactory> =
    listOf(
        {
            module {
                single<Clock> { Clock.System }
                single<TimeZone> { TimeZone.currentSystemDefault() }
                single<MatrixClientFactory> {
                    MatrixClientFactoryImpl(
                        secretByteArrays = get(),
                        createRepositoriesModule = get(),
                        createMediaStoreModule = get(),
                        createCryptoDriverModule = get(),
                        appCoroutineContext = get<CoroutineScope>().coroutineContext,
                        messengerConfiguration = get(),
                    )
                }
                single<MatrixClients> {
                        MatrixClientsImpl(
                            matrixClientFactory = get(),
                            deleteAccountData = get(),
                            settings = get(),
                            config = get(),
                            secretByteArrays = get(),
                            i18n = get(),
                        )
                    }
                    .apply {
                        bind<AutoCloseable>()
                        bind<Worker>()
                    }

                single<TimelineEventContentToString> { TimelineEventContentToStringImpl(get()) }
                single<Initials> { InitialsImpl(get()) }
                single<VerifyAccount> { VerifyAccountImpl() }

                single<Languages> { DefaultLanguages }
                single<I18n> { I18n(get(), get(), get(), get()) }
                single<RoomName> { RoomNameImpl(get(), get()) }
                single<RoomTopic> { RoomTopicImpl() }
                single<RoomInviter> { RoomInviterImpl }
                single<UserBlocking> { UserBlockingImpl() }
                single<EnterRoom> { EnterRoomImpl() }
                single<LeaveRoom> { LeaveRoomImpl() }

                single<DownloadManager> { DownloadManagerImpl(get<CoroutineScope>().coroutineContext) }
                single<Thumbnails> { ThumbnailsImpl() }
                single<IsOneToOneRoom> { IsOneToOneRoomImpl }
                single<ActiveVerifications> { ActiveVerificationsImpl() }
                single<RoomPresence> { RoomPresenceImpl }
                single<Search> { SearchImpl(get(), get(), get()) }
                single<RunInitialSync> { RunInitialSyncImpl }
                single<DragAndDropHandler> { DragAndDropHandlerBase() }
                single<AccountSetupViewModelFactory> { AccountSetupViewModelFactory }
                single<MatrixMarkdownFlavour> { MatrixMarkdownFlavourImpl() }
                single<InformationMarkdownFlavour> { InformationMarkdownFlavourImpl() }

                single<RootViewModelFactory> { RootViewModelFactory }
                single<MainViewModelFactory> { MainViewModelFactory }

                single<AuthorizeUia> { AuthorizeUiaImpl() }
                single<UiaActionConfirmationViewModelFactory> { UiaActionConfirmationViewModelFactory }
                single<UiaStepDummyViewModelFactory> { UiaStepDummyViewModelFactory }
                single<UiaStepPasswordViewModelFactory> { UiaStepPasswordViewModelFactory }
                single<UiaStepOAuth2ViewModelFactory> { UiaStepOAuth2ViewModelFactory }
                single<UiaStepRegistrationTokenViewModelFactory> { UiaStepRegistrationTokenViewModelFactory }
                single<UiaStepEmailIdentityViewModelFactory> { UiaStepEmailIdentityViewModelFactory }
                single<UiaStepMsisdnViewModelFactory> { UiaStepMsisdnViewModelFactory }
                single<UiaStepFallbackViewModelFactory> { UiaStepFallbackViewModelFactory }

                single<ShareDataViewModelFactory> { ShareDataViewModelFactory }
                single<SharedDataHandler> { SharedDataHandlerImpl() }

                single<BackHandler> { BackHandlerImpl() }
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
        ::mediaViewModels,
        ::exportModule,
        ::notificationModule,
        // include last as they override some other already registered view models
        ::newSearchViewModels,

        // Platform-specific implementations:
        ::platformModule,
        ::platformPathsModule,
        ::platformStringsModule,
        ::platformCreateRepositoriesModuleModule,
        ::platformCreateMediaStoreModuleModule,
        ::createCryptoDriverModuleModule,
        ::secretsModule,
        ::platformGetSystemLangModule,
        ::platformDeleteAccountDataModule,
        ::platformMatrixMessengerSettingsHolderModule,
        ::platformSendLogToDevsModule,
        ::platformGetDefaultDisplayNameModule,
        ::platformIsNetworkAvailableModule,
        ::platformCloseAppModule,
        ::platformMinimizeAppModule,
        ::platformUriHandlerModule,
        ::platformUriCallerModule,
        ::platformDeleteProfileDataModule,
        ::platformProcessImageUploadModule,
        ::platformGetImageDimensionsModule,
        ::audioMetadataFactoryModule,
    )

/*
 * Factories for view models: Provide your own factory to
 * change or enhance behaviors of existing view models.
 */

private fun connectingViewModels() = module {
    single<MatrixClientInitializationViewModelFactory> { MatrixClientInitializationViewModelFactory }
    single<RemoveMatrixAccountViewModelFactory> { RemoveMatrixAccountViewModelFactory }
    single<MatrixClientInitializationFailureViewModelFactory> { MatrixClientInitializationFailureViewModelFactory }
    single<AddMatrixAccountViewModelFactory> { AddMatrixAccountViewModelFactory }
    single<PasswordLoginViewModelFactory> { PasswordLoginViewModelFactory }
    single<OAuth2AuthorizationCodeLoginViewModelFactory> { OAuth2AuthorizationCodeLoginViewModelFactory }
    single<OAuth2DeviceAuthorizationLoginViewModelFactory> { OAuth2DeviceAuthorizationLoginViewModelFactory }
    single<SSOLoginViewModelFactory> { SSOLoginViewModelFactory }
    single<RegisterMatrixAccountViewModelFactory> { RegisterMatrixAccountViewModelFactory }
}

private fun syncViewModels() = module { single<SyncViewModelFactory> { SyncViewModelFactory } }

private fun roomListViewModels() = module {
    single<AccountViewModelFactory> { AccountViewModelFactory }
    single<CreateNewChatViewModelFactory> { CreateNewChatViewModelFactory }
    single<CreateNewGroupViewModelFactory> { CreateNewGroupViewModelFactory }
    single<CreateNewRoomViewModelFactory> { CreateNewRoomViewModelFactory }
    single<SearchGroupViewModelFactory> { SearchGroupViewModelFactory }
    single<RoomListElementViewModelFactory> { RoomListElementViewModelFactory }
    single<RoomListViewModelFactory> { RoomListViewModelFactory }
}

inline fun <reified F : SearchProviderFactory<*, *>> Module.searchProviderFactory(
    noinline definition: Scope.(ParametersHolder) -> F
) = single<SearchProviderFactory<*, *>>(named<F>(), definition = definition)

private fun newSearchViewModels() = module {
    searchProviderFactory { HomeserverSearchProviderFactory(get(), get(), get(), get()) }
    single<SearchProviderSorter> { SearchProviderSorterImpl() }
    single<SearchViewModelFactory> { SearchViewModelFactory }
    single<UserSearchViewModelFactory> { UserSearchViewModelFactory }
    single<CreateNewChatViewModelFactory> {
        val config = get<MatrixMessengerConfiguration>()
        object : CreateNewChatViewModelFactory {
            override fun create(
                viewModelContext: MatrixClientViewModelContext,
                createNewRoomViewModel: CreateNewRoomViewModel,
                onCreateGroup: (UserId) -> Unit,
                onSearchGroup: (UserId) -> Unit,
                onCancel: () -> Unit,
            ): CreateNewChatViewModel {
                return if (config.features.enableNewSearch) {
                    CreateNewChatNewSearchViewModelImpl(
                            viewModelContext = viewModelContext,
                            createNewChatViewModel =
                                CreateNewChatViewModelImpl(
                                    viewModelContext = viewModelContext,
                                    createNewRoomViewModel = createNewRoomViewModel,
                                    onCreateGroup = onCreateGroup,
                                    onSearchGroup = onSearchGroup,
                                    onCancel = onCancel,
                                ),
                        )
                        .also { log.debug { "CreateNewChatViewModel -> CreateNewChatNewSearchViewModel" } }
                } else {
                    CreateNewChatViewModelImpl(
                            viewModelContext = viewModelContext,
                            createNewRoomViewModel = createNewRoomViewModel,
                            onCreateGroup = onCreateGroup,
                            onSearchGroup = onSearchGroup,
                            onCancel = onCancel,
                        )
                        .also { log.debug { "CreateNewChatViewModel" } }
                }
            }
        }
    }
    single<CreateNewGroupViewModelFactory> {
        val config = get<MatrixMessengerConfiguration>()
        object : CreateNewGroupViewModelFactory {
            override fun create(
                viewModelContext: MatrixClientViewModelContext,
                createNewRoomViewModel: CreateNewRoomViewModel,
                onBack: () -> Unit,
            ): CreateNewGroupViewModel {
                return if (config.features.enableNewSearch) {
                    CreateNewGroupNewSearchViewModelImpl(
                            viewModelContext = viewModelContext,
                            createNewGroupViewModel =
                                CreateNewGroupViewModelImpl(
                                    viewModelContext = viewModelContext,
                                    createNewRoomViewModel = createNewRoomViewModel,
                                    onBack = onBack,
                                ),
                        )
                        .also { log.debug { "CreateNewGroupViewModel -> CreateNewGroupNewSearchViewModelImpl" } }
                } else {
                    CreateNewGroupViewModelImpl(
                            viewModelContext = viewModelContext,
                            createNewRoomViewModel = createNewRoomViewModel,
                            onBack = onBack,
                        )
                        .also { log.debug { "CreateNewGroupViewModel" } }
                }
            }
        }
    }
    single<AddMembersViewModelFactory> {
        val config = get<MatrixMessengerConfiguration>()
        object : AddMembersViewModelFactory {
            override fun create(
                viewModelContext: MatrixClientViewModelContext,
                roomId: RoomId,
                potentialMembersViewModel: PotentialMembersViewModel,
                onBack: () -> Unit,
            ): AddMembersViewModel {
                return if (config.features.enableNewSearch) {
                    AddMembersNewSearchViewModelImpl(
                            viewModelContext = viewModelContext,
                            addMembersViewModel =
                                AddMembersViewModelImpl(
                                    viewModelContext = viewModelContext,
                                    roomId = roomId,
                                    potentialMembersViewModel = potentialMembersViewModel,
                                    onBack = onBack,
                                ),
                            roomId = roomId,
                            onBack = onBack,
                            potentialMembersNewSearchViewModel =
                                PotentialMembersNewSearchViewModelImpl(
                                    viewModelContext = viewModelContext,
                                    potentialMembersViewModel = potentialMembersViewModel,
                                ),
                        )
                        .also { log.debug { "AddMembersViewModel -> AddMembersNewSearchViewModelImpl" } }
                } else {
                    AddMembersViewModelImpl(
                            viewModelContext = viewModelContext,
                            roomId = roomId,
                            potentialMembersViewModel = potentialMembersViewModel,
                            onBack = onBack,
                        )
                        .also { log.debug { "AddMembersViewModel" } }
                }
            }
        }
    }
    single<PotentialMembersViewModelFactory> {
        val config = get<MatrixMessengerConfiguration>()
        object : PotentialMembersViewModelFactory {
            override fun create(
                viewModelContext: MatrixClientViewModelContext,
                roomId: RoomId,
            ): PotentialMembersViewModel {
                return if (config.features.enableNewSearch) {
                    PotentialMembersNewSearchViewModelImpl(
                            viewModelContext = viewModelContext,
                            potentialMembersViewModel =
                                PotentialMembersViewModelImpl(viewModelContext = viewModelContext, roomId = roomId),
                        )
                        .also { log.debug { "PotentialMembersViewModel -> PotentialMembersNewSearchViewModelImpl" } }
                } else {
                    PotentialMembersViewModelImpl(viewModelContext = viewModelContext, roomId = roomId).also {
                        log.debug { "PotentialMembersViewModel" }
                    }
                }
            }
        }
    }
}

private fun settingsViewModels() = module {
    single<AppInfoViewModelFactory> { AppInfoViewModelFactory }
    single<AvatarCutterViewModelFactory> { AvatarCutterViewModelFactory }
    single<DeviceSettingsSingleAccountViewModelFactory> { DeviceSettingsSingleAccountViewModelFactory }
    single<DeviceSettingsAllAccountsViewModelFactory> { DeviceSettingsAllAccountsViewModelFactory }
    single<NotificationSettingsAllAccountsViewModelFactory> { NotificationSettingsAllAccountsViewModelFactory }
    single<AccountsViewModelFactory> { AccountsViewModelFactory }
    single<ProfilesSettingsViewModelFactory> { ProfilesSettingsViewModelFactory }
    single<ProfilesSettingsSingleViewModelFactory> { ProfilesSettingsSingleViewModelFactory }
    single<AccountSingleViewModelFactory> { AccountSingleViewModelFactory }
    single<UserSettingsViewModelFactory> { UserSettingsViewModelFactory }
    single<PrivacySettingsAllAccountsViewModelFactory> { PrivacySettingsAllAccountsViewModelFactory }
    single<PrivacySettingsSingleAccountViewModelFactory> { PrivacySettingsSingleAccountViewModelFactory }
    single<AppearanceSettingsViewModelFactory> { AppearanceSettingsViewModelFactory }
    single<BlockedContactsSettingsViewModelFactory> { BlockedContactsSettingsViewModelFactory }
    single<NotificationSettingsSingleAccountViewModelFactory> { NotificationSettingsSingleAccountViewModelFactory }
}

inline fun <reified F : TimelineElementViewModelFactory<*>> Module.timelineElementViewModelFactory(
    noinline definition: Scope.(ParametersHolder) -> F
) = single<F>(named<F>(), definition = definition).bind<TimelineElementViewModelFactory<*>>()

private fun timelineElementViewModels() = module {
    // Message:
    timelineElementViewModelFactory<FileRoomMessageTimelineElementViewModelFactory> {
        FileRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<ImageRoomMessageTimelineElementViewModelFactory> {
        ImageRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<VideoRoomMessageTimelineElementViewModelFactory> {
        VideoRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<AudioRoomMessageTimelineElementViewModelFactory> {
        AudioRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<TextRoomMessageTimelineElementViewModelFactory> {
        TextRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<NoticeRoomMessageTimelineElementViewModelFactory> {
        NoticeRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<EmoteRoomMessageTimelineElementViewModelFactory> {
        EmoteRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<LocationRoomMessageTimelineElementViewModelFactory> {
        LocationRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<VerificationRequestRoomMessageTimelineElementViewModelFactory> {
        VerificationRequestRoomMessageTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<VerificationDoneTimelineElementViewModelFactory> {
        VerificationDoneTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<VerificationCancelTimelineElementViewModelFactory> {
        VerificationCancelTimelineElementViewModelFactory
    }

    // State:
    timelineElementViewModelFactory<CreateStateTimelineElementViewModelFactory> {
        CreateStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<NameStateTimelineElementViewModelFactory> {
        NameStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<TopicStateTimelineElementViewModelFactory> {
        TopicStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<AvatarStateTimelineElementViewModelFactory> {
        AvatarStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<MemberStateTimelineElementViewModelFactory> {
        MemberStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<CanonicalAliasStateTimelineElementViewModelFactory> {
        CanonicalAliasStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<HistoryVisibilityStateTimelineElementViewModelFactory> {
        HistoryVisibilityStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<EncryptionStateTimelineElementViewModelFactory> {
        EncryptionStateTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<PowerLevelsTimelineElementViewModelFactory> {
        PowerLevelsTimelineElementViewModelFactory
    }
    timelineElementViewModelFactory<TombstoneStateTimelineElementViewModelFactory> {
        TombstoneStateTimelineElementViewModelFactory
    }

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
    single<JoinRoomActionViewModelFactory> { JoinRoomActionViewModelFactory }
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
    single<TimelineElementDevInfoViewModelFactory> { TimelineElementDevInfoViewModelFactory }
    single<UserProfileViewModelFactory> { UserProfileViewModelFactory }
    single<AddMembersViewModelFactory> { AddMembersViewModelFactory }
    single<RoomDevInfoViewModelFactory> { RoomDevInfoViewModelFactory }
    single<ExportRoomViewModelFactory> { ExportRoomViewModelFactory }
    single<PowerlevelViewModelFactory> { PowerlevelViewModelFactory }
}

private fun mediaViewModels() = module {
    single<MediaPlayerViewModelFactory> { MediaPlayerViewModelFactory }
    factory<AudioRecorderHolder> {
        val config = get<MatrixMessengerConfiguration>()
        val platformAudioRecorder = getOrNull<PlatformAudioRecorder>()
        val audioRecorder =
            if (config.features.enableAudioRecorder && platformAudioRecorder != null) {
                AudioRecorderImpl(platformAudioRecorder, get(), get())
            } else {
                null
            }
        AudioRecorderHolder(audioRecorder)
    }
}

private fun timelineViewModels() = module {
    single<InputAreaViewModelFactory> { InputAreaViewModelFactory }
    single<AudioRecordingAreaViewModelFactory> { AudioRecordingAreaViewModelFactory }
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
