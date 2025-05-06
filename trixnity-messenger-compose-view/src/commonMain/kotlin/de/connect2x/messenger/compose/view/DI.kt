package de.connect2x.messenger.compose.view

import de.connect2x.messenger.compose.view.common.AdaptiveDialog
import de.connect2x.messenger.compose.view.common.AdaptiveDialogImpl
import de.connect2x.messenger.compose.view.common.FilePickerTypeSelectionView
import de.connect2x.messenger.compose.view.common.FilePickerTypeSelectionViewImpl
import de.connect2x.messenger.compose.view.common.MatrixUsernameView
import de.connect2x.messenger.compose.view.common.MatrixUsernameViewImpl
import de.connect2x.messenger.compose.view.connecting.AddMatrixAccountView
import de.connect2x.messenger.compose.view.connecting.AddMatrixAccountViewImpl
import de.connect2x.messenger.compose.view.connecting.AdditionalConnectingWizardStep
import de.connect2x.messenger.compose.view.connecting.AdditionalConnectingWizardStepImpl
import de.connect2x.messenger.compose.view.connecting.MatrixClientInitializationFailureView
import de.connect2x.messenger.compose.view.connecting.MatrixClientInitializationFailureViewImpl
import de.connect2x.messenger.compose.view.connecting.MatrixClientInitializationView
import de.connect2x.messenger.compose.view.connecting.MatrixClientInitializationViewImpl
import de.connect2x.messenger.compose.view.connecting.PasswordLoginView
import de.connect2x.messenger.compose.view.connecting.PasswordLoginViewImpl
import de.connect2x.messenger.compose.view.connecting.RegisterNewAccountView
import de.connect2x.messenger.compose.view.connecting.RegisterNewAccountViewImpl
import de.connect2x.messenger.compose.view.connecting.RemoveMatrixAccountView
import de.connect2x.messenger.compose.view.connecting.RemoveMatrixAccountViewImpl
import de.connect2x.messenger.compose.view.connecting.SSOLoginView
import de.connect2x.messenger.compose.view.connecting.SSOLoginViewImpl
import de.connect2x.messenger.compose.view.connecting.ServerDiscoveryStateView
import de.connect2x.messenger.compose.view.connecting.ServerDiscoveryStateViewImpl
import de.connect2x.messenger.compose.view.connecting.ServerInputFieldView
import de.connect2x.messenger.compose.view.connecting.ServerInputFieldViewImpl
import de.connect2x.messenger.compose.view.i18n.i18nViewModule
import de.connect2x.messenger.compose.view.profiles.ProfileCreationView
import de.connect2x.messenger.compose.view.profiles.ProfileCreationViewImpl
import de.connect2x.messenger.compose.view.profiles.ProfileSelectionView
import de.connect2x.messenger.compose.view.profiles.ProfileSelectionViewImpl
import de.connect2x.messenger.compose.view.profiles.ProfilesView
import de.connect2x.messenger.compose.view.profiles.ProfilesViewImpl
import de.connect2x.messenger.compose.view.room.RoomView
import de.connect2x.messenger.compose.view.room.RoomViewImpl
import de.connect2x.messenger.compose.view.room.settings.AddMembersToRoomView
import de.connect2x.messenger.compose.view.room.settings.AddMembersToRoomViewImpl
import de.connect2x.messenger.compose.view.room.settings.ChangeRoomAvatarView
import de.connect2x.messenger.compose.view.room.settings.ChangeRoomAvatarViewImpl
import de.connect2x.messenger.compose.view.room.settings.ExportRoomView
import de.connect2x.messenger.compose.view.room.settings.ExportRoomViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsExportRoomView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsExportRoomViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsHistoryVisibilityView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsHistoryVisibilityViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsJoinRulesView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsJoinRulesViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsLeaveRoomView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsLeaveRoomViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsMemberListElementView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsMemberListElementViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsMemberListView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsMemberListViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsNameView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsNameViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsNotificationsView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsNotificationsViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsSecurityView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsSecurityViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsTopicView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsTopicViewImpl
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsView
import de.connect2x.messenger.compose.view.room.settings.RoomSettingsViewImpl
import de.connect2x.messenger.compose.view.room.settings.SearchUsersSettingsView
import de.connect2x.messenger.compose.view.room.settings.SearchUsersSettingsViewImpl
import de.connect2x.messenger.compose.view.room.settings.TimelineElementMetadataView
import de.connect2x.messenger.compose.view.room.settings.TimelineElementMetadataViewImpl
import de.connect2x.messenger.compose.view.room.settings.UserProfileView
import de.connect2x.messenger.compose.view.room.settings.UserProfileViewImpl
import de.connect2x.messenger.compose.view.room.timeline.AudioReplyView
import de.connect2x.messenger.compose.view.room.timeline.AudioReplyViewImpl
import de.connect2x.messenger.compose.view.room.timeline.FileReplyView
import de.connect2x.messenger.compose.view.room.timeline.FileReplyViewImpl
import de.connect2x.messenger.compose.view.room.timeline.ImageReplyDefaultView
import de.connect2x.messenger.compose.view.room.timeline.ImageReplyDefaultViewImpl
import de.connect2x.messenger.compose.view.room.timeline.ImageReplyView
import de.connect2x.messenger.compose.view.room.timeline.ImageReplyViewImpl
import de.connect2x.messenger.compose.view.room.timeline.InputAreaView
import de.connect2x.messenger.compose.view.room.timeline.InputAreaViewImpl
import de.connect2x.messenger.compose.view.room.timeline.LocationReplyView
import de.connect2x.messenger.compose.view.room.timeline.LocationReplyViewImpl
import de.connect2x.messenger.compose.view.room.timeline.ReplyToAreaView
import de.connect2x.messenger.compose.view.room.timeline.ReplyToAreaViewImpl
import de.connect2x.messenger.compose.view.room.timeline.RoomHeaderView
import de.connect2x.messenger.compose.view.room.timeline.RoomHeaderViewImpl
import de.connect2x.messenger.compose.view.room.timeline.ScrollToEndButtonView
import de.connect2x.messenger.compose.view.room.timeline.ScrollToEndButtonViewImpl
import de.connect2x.messenger.compose.view.room.timeline.SendAttachmentSendButtonView
import de.connect2x.messenger.compose.view.room.timeline.SendAttachmentSendButtonViewImpl
import de.connect2x.messenger.compose.view.room.timeline.SendAttachmentTitleView
import de.connect2x.messenger.compose.view.room.timeline.SendAttachmentTitleViewImpl
import de.connect2x.messenger.compose.view.room.timeline.SendAttachmentView
import de.connect2x.messenger.compose.view.room.timeline.SendAttachmentViewImpl
import de.connect2x.messenger.compose.view.room.timeline.TextReplyView
import de.connect2x.messenger.compose.view.room.timeline.TextReplyViewImpl
import de.connect2x.messenger.compose.view.room.timeline.TimelineView
import de.connect2x.messenger.compose.view.room.timeline.TimelineViewImpl
import de.connect2x.messenger.compose.view.room.timeline.TypingIndicatorView
import de.connect2x.messenger.compose.view.room.timeline.TypingIndicatorViewImpl
import de.connect2x.messenger.compose.view.room.timeline.UnknownReplyView
import de.connect2x.messenger.compose.view.room.timeline.UnknownReplyViewImpl
import de.connect2x.messenger.compose.view.room.timeline.VideoReplyDefaultView
import de.connect2x.messenger.compose.view.room.timeline.VideoReplyDefaultViewImpl
import de.connect2x.messenger.compose.view.room.timeline.VideoReplyView
import de.connect2x.messenger.compose.view.room.timeline.VideoReplyViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.EncryptedErrorTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.EncryptedWaitTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.MessageReactionsView
import de.connect2x.messenger.compose.view.room.timeline.element.MessageReactionsViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.ReadMarkerView
import de.connect2x.messenger.compose.view.room.timeline.element.ReadMarkerViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.RedactedTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementHolderView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementHolderViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelectorImpl
import de.connect2x.messenger.compose.view.room.timeline.element.details.ElementDetailsViewSelector
import de.connect2x.messenger.compose.view.room.timeline.element.details.ElementDetailsViewSelectorImpl
import de.connect2x.messenger.compose.view.room.timeline.element.details.ImageTimelineElementDetailsView
import de.connect2x.messenger.compose.view.room.timeline.element.details.TimelineElementDetailsView
import de.connect2x.messenger.compose.view.room.timeline.element.message.AudioRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.EmoteRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.FileBasedRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.FileBasedRoomMessageTimelineElementViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.message.FileRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.ImageRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.LocationRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.NoticeRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.TextRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.UnknownRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.VerificationRequestRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.VideoRoomMessageTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleViewImpl
import de.connect2x.messenger.compose.view.room.timeline.element.state.AvatarStateTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.state.CanonicalAliasStateTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.state.CreateStateTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.state.EncryptionStateTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.state.HistoryVisibilityStateTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.state.MemberStateTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.state.NameStateTimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.state.TopicStateTimelineElementView
import de.connect2x.messenger.compose.view.roomlist.RoomListContainerView
import de.connect2x.messenger.compose.view.roomlist.RoomListContainerViewImpl
import de.connect2x.messenger.compose.view.roomlist.RoomListView
import de.connect2x.messenger.compose.view.roomlist.RoomListViewImpl
import de.connect2x.messenger.compose.view.roomlist.create.CreateGroupOptionsView
import de.connect2x.messenger.compose.view.roomlist.create.CreateGroupOptionsViewImpl
import de.connect2x.messenger.compose.view.roomlist.create.CreateNewChatView
import de.connect2x.messenger.compose.view.roomlist.create.CreateNewChatViewImpl
import de.connect2x.messenger.compose.view.roomlist.create.CreateNewGroupView
import de.connect2x.messenger.compose.view.roomlist.create.CreateNewGroupViewImpl
import de.connect2x.messenger.compose.view.roomlist.create.UsersInGroupView
import de.connect2x.messenger.compose.view.roomlist.create.UsersInGroupViewImpl
import de.connect2x.messenger.compose.view.roomlist.header.AccountAvatarView
import de.connect2x.messenger.compose.view.roomlist.header.AccountAvatarViewImpl
import de.connect2x.messenger.compose.view.roomlist.header.AccountDataView
import de.connect2x.messenger.compose.view.roomlist.header.AccountDataViewImpl
import de.connect2x.messenger.compose.view.roomlist.header.AccountOptionsView
import de.connect2x.messenger.compose.view.roomlist.header.AccountOptionsViewImpl
import de.connect2x.messenger.compose.view.roomlist.header.CloseProfileView
import de.connect2x.messenger.compose.view.roomlist.header.CloseProfileViewImpl
import de.connect2x.messenger.compose.view.roomlist.header.ShowSearchView
import de.connect2x.messenger.compose.view.roomlist.header.ShowSearchViewImpl
import de.connect2x.messenger.compose.view.roomlist.room.KnockRoomListElement
import de.connect2x.messenger.compose.view.roomlist.room.KnockRoomListElementImpl
import de.connect2x.messenger.compose.view.roomlist.room.ArchivedRoomListElement
import de.connect2x.messenger.compose.view.roomlist.room.ArchivedRoomListElementImpl
import de.connect2x.messenger.compose.view.roomlist.room.InviteRoomListElement
import de.connect2x.messenger.compose.view.roomlist.room.InviteRoomListElementImpl
import de.connect2x.messenger.compose.view.roomlist.room.RoomListElementContainerView
import de.connect2x.messenger.compose.view.roomlist.room.RoomListElementContainerViewImpl
import de.connect2x.messenger.compose.view.roomlist.room.RoomListElementView
import de.connect2x.messenger.compose.view.roomlist.room.RoomListElementViewImpl
import de.connect2x.messenger.compose.view.roomlist.search.SearchGroupView
import de.connect2x.messenger.compose.view.roomlist.search.SearchGroupViewImpl
import de.connect2x.messenger.compose.view.roomlist.search.SearchUsersView
import de.connect2x.messenger.compose.view.roomlist.search.SearchUsersViewImpl
import de.connect2x.messenger.compose.view.root.MainView
import de.connect2x.messenger.compose.view.root.MainViewImpl
import de.connect2x.messenger.compose.view.root.MessengerView
import de.connect2x.messenger.compose.view.root.MessengerViewImpl
import de.connect2x.messenger.compose.view.root.SyncOverlayView
import de.connect2x.messenger.compose.view.root.SyncOverlayViewImpl
import de.connect2x.messenger.compose.view.search.UserSearchFieldView
import de.connect2x.messenger.compose.view.search.UserSearchFieldViewImpl
import de.connect2x.messenger.compose.view.search.UserSearchResultListView
import de.connect2x.messenger.compose.view.search.UserSearchResultListViewImpl
import de.connect2x.messenger.compose.view.settings.AccountSetupWizardStepList
import de.connect2x.messenger.compose.view.settings.AccountSetupWizardStepListImpl
import de.connect2x.messenger.compose.view.settings.AccountsOverviewView
import de.connect2x.messenger.compose.view.settings.AccountsOverviewViewImpl
import de.connect2x.messenger.compose.view.settings.AdditionalAccountSetupWizardStep
import de.connect2x.messenger.compose.view.settings.AdditionalAccountSetupWizardStepImpl
import de.connect2x.messenger.compose.view.settings.AppInfoImprintView
import de.connect2x.messenger.compose.view.settings.AppInfoImprintViewImpl
import de.connect2x.messenger.compose.view.settings.AppInfoLicensesView
import de.connect2x.messenger.compose.view.settings.AppInfoLicensesViewImpl
import de.connect2x.messenger.compose.view.settings.AppInfoPrivacyView
import de.connect2x.messenger.compose.view.settings.AppInfoPrivacyViewImpl
import de.connect2x.messenger.compose.view.settings.AppInfoVersionView
import de.connect2x.messenger.compose.view.settings.AppInfoVersionViewImpl
import de.connect2x.messenger.compose.view.settings.AppInfoView
import de.connect2x.messenger.compose.view.settings.AppInfoViewImpl
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsColorView
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsColorViewImpl
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsSizeView
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsSizeViewImpl
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsThemeView
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsThemeViewImpl
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsView
import de.connect2x.messenger.compose.view.settings.AppearanceSettingsViewImpl
import de.connect2x.messenger.compose.view.settings.AvatarCutterView
import de.connect2x.messenger.compose.view.settings.AvatarCutterViewImpl
import de.connect2x.messenger.compose.view.settings.BlockedContactsSettingsView
import de.connect2x.messenger.compose.view.settings.BlockedContactsSettingsViewImpl
import de.connect2x.messenger.compose.view.settings.DevicesSettingsView
import de.connect2x.messenger.compose.view.settings.DevicesSettingsViewImpl
import de.connect2x.messenger.compose.view.settings.NotificationsSettingsView
import de.connect2x.messenger.compose.view.settings.NotificationsSettingsViewImpl
import de.connect2x.messenger.compose.view.settings.PrivacySettingsView
import de.connect2x.messenger.compose.view.settings.PrivacySettingsViewImpl
import de.connect2x.messenger.compose.view.settings.ProfileSettingsView
import de.connect2x.messenger.compose.view.settings.ProfileSettingsViewImpl
import de.connect2x.messenger.compose.view.settings.UserSettingsView
import de.connect2x.messenger.compose.view.settings.UserSettingsViewImpl
import de.connect2x.messenger.compose.view.sharing.ShareDataView
import de.connect2x.messenger.compose.view.sharing.ShareDataViewImpl
import de.connect2x.messenger.compose.view.theme.DefaultAccentColor
import de.connect2x.messenger.compose.view.theme.DefaultAccentColorImpl
import de.connect2x.messenger.compose.view.theme.DefaultSizes
import de.connect2x.messenger.compose.view.theme.DefaultSizesImpl
import de.connect2x.messenger.compose.view.theme.MessengerColorScheme
import de.connect2x.messenger.compose.view.theme.MessengerColorSchemeImpl
import de.connect2x.messenger.compose.view.theme.Theme
import de.connect2x.messenger.compose.view.theme.ThemeComponents
import de.connect2x.messenger.compose.view.theme.ThemeComponentsImpl
import de.connect2x.messenger.compose.view.theme.ThemeDarkColorScheme
import de.connect2x.messenger.compose.view.theme.ThemeDarkColorSchemeImpl
import de.connect2x.messenger.compose.view.theme.ThemeHighContrastDarkColorScheme
import de.connect2x.messenger.compose.view.theme.ThemeHighContrastDarkColorSchemeImpl
import de.connect2x.messenger.compose.view.theme.ThemeHighContrastLightColorScheme
import de.connect2x.messenger.compose.view.theme.ThemeHighContrastLightColorSchemeImpl
import de.connect2x.messenger.compose.view.theme.ThemeImpl
import de.connect2x.messenger.compose.view.theme.ThemeLightColorScheme
import de.connect2x.messenger.compose.view.theme.ThemeLightColorSchemeImpl
import de.connect2x.messenger.compose.view.theme.ThemeTypography
import de.connect2x.messenger.compose.view.theme.ThemeTypographyImpl
import de.connect2x.messenger.compose.view.uia.UiaActionConfirmationView
import de.connect2x.messenger.compose.view.uia.UiaActionConfirmationViewImpl
import de.connect2x.messenger.compose.view.uia.UiaDummyStepView
import de.connect2x.messenger.compose.view.uia.UiaDummyStepViewImpl
import de.connect2x.messenger.compose.view.uia.UiaEmailIdentityStepView
import de.connect2x.messenger.compose.view.uia.UiaEmailIdentityStepViewImpl
import de.connect2x.messenger.compose.view.uia.UiaFallbackFlowView
import de.connect2x.messenger.compose.view.uia.UiaFallbackFlowViewImpl
import de.connect2x.messenger.compose.view.uia.UiaModalBoxView
import de.connect2x.messenger.compose.view.uia.UiaModalBoxViewImpl
import de.connect2x.messenger.compose.view.uia.UiaMsisdnStepView
import de.connect2x.messenger.compose.view.uia.UiaMsisdnStepViewImpl
import de.connect2x.messenger.compose.view.uia.UiaPasswordInputView
import de.connect2x.messenger.compose.view.uia.UiaPasswordInputViewImpl
import de.connect2x.messenger.compose.view.uia.UiaRegistrationTokenView
import de.connect2x.messenger.compose.view.uia.UiaRegistrationTokenViewImpl
import de.connect2x.messenger.compose.view.util.platformCopyToClipboardModule
import de.connect2x.messenger.compose.view.verification.DeviceVerificationWizardView
import de.connect2x.messenger.compose.view.verification.DeviceVerificationWizardViewImpl
import de.connect2x.messenger.compose.view.verification.RedoSelfVerificationWizardView
import de.connect2x.messenger.compose.view.verification.RedoSelfVerificationWizardViewImpl
import de.connect2x.messenger.compose.view.verification.SelfVerificationWizardView
import de.connect2x.messenger.compose.view.verification.SelfVerificationWizardViewImpl
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

fun composeViewModule(): Module = module {
    includes(
        i18nViewModule(),
        themeViewModule(),
        commonViewModule(),
        rootViewModule(),
        connectingViewModule(),
        filesViewModule(),
        profileViewModule(),
        roomListViewModule(),
        roomListHeaderViewModule(),
        createRoomsViewModule(),
        searchViewModule(),
        roomViewModule(),
        roomSettingsViewModule(),
        timelineViewModule(),
        userSearchViewModule(),
        settingsViewModule(),
        verificationViewModule(),
        uiaViewModule(),
        platformCopyToClipboardModule()
    )
}

fun themeViewModule(): Module = module {
    single<Theme> { ThemeImpl() }
    single<ThemeTypography> { ThemeTypographyImpl() }
    single<ThemeComponents> { ThemeComponentsImpl() }
    single<DefaultAccentColor> { DefaultAccentColorImpl() }
    single<DefaultSizes> { DefaultSizesImpl() }
    single<MessengerColorScheme> { MessengerColorSchemeImpl(get(), get(), get(), get()) }
    single<ThemeLightColorScheme> { ThemeLightColorSchemeImpl() }
    single<ThemeDarkColorScheme> { ThemeDarkColorSchemeImpl() }
    single<ThemeHighContrastLightColorScheme> { ThemeHighContrastLightColorSchemeImpl(get()) }
    single<ThemeHighContrastDarkColorScheme> { ThemeHighContrastDarkColorSchemeImpl(get()) }
}

fun commonViewModule() = module {
    single<MatrixUsernameView> { MatrixUsernameViewImpl() }
    single<AdaptiveDialog> { AdaptiveDialogImpl() }
}

fun rootViewModule() = module {
    single<ClientView> { ClientViewImpl() }
    single<MainView> { MainViewImpl() }
    single<MessengerView> { MessengerViewImpl() }
}

fun connectingViewModule() = module {
    single<AddMatrixAccountView> { AddMatrixAccountViewImpl() }
    single<ServerDiscoveryStateView> { ServerDiscoveryStateViewImpl() }
    single<ServerInputFieldView> { ServerInputFieldViewImpl() }
    single<MatrixClientInitializationView> { MatrixClientInitializationViewImpl() }
    single<RemoveMatrixAccountView> { RemoveMatrixAccountViewImpl() }
    single<PasswordLoginView> { PasswordLoginViewImpl() }
    single<RegisterNewAccountView> { RegisterNewAccountViewImpl() }
    single<SSOLoginView> { SSOLoginViewImpl() }
    single<MatrixClientInitializationFailureView> { MatrixClientInitializationFailureViewImpl() }
    single<AdditionalConnectingWizardStep> { AdditionalConnectingWizardStepImpl() }
    single<SyncOverlayView> { SyncOverlayViewImpl() }
}

fun filesViewModule() = module {
    single<ShareDataView> { ShareDataViewImpl() }
    single<FilePickerTypeSelectionView> { FilePickerTypeSelectionViewImpl() }
}

fun profileViewModule() = module {
    single<ProfilesView> { ProfilesViewImpl() }
    single<ProfileCreationView> { ProfileCreationViewImpl() }
    single<ProfileSelectionView> { ProfileSelectionViewImpl() }
}

fun roomListViewModule() = module {
    single<RoomListContainerView> { RoomListContainerViewImpl() }
    single<RoomListView> { RoomListViewImpl() }
    single<RoomListElementContainerView> { RoomListElementContainerViewImpl() }
    single<RoomListElementView> { RoomListElementViewImpl() }
    single<InviteRoomListElement> { InviteRoomListElementImpl() }
    single<ArchivedRoomListElement> { ArchivedRoomListElementImpl() }
    single<KnockRoomListElement> { KnockRoomListElementImpl() }
    single<RoomView> { RoomViewImpl() }
}

fun roomListHeaderViewModule() = module {
    single<AccountDataView> { AccountDataViewImpl() }
    single<AccountAvatarView> { AccountAvatarViewImpl() }
    single<CloseProfileView> { CloseProfileViewImpl() }
    single<ShowSearchView> { ShowSearchViewImpl() }
    single<AccountOptionsView> { AccountOptionsViewImpl() }
}

fun createRoomsViewModule() = module {
    single<CreateNewChatView> { CreateNewChatViewImpl() }
    single<CreateNewGroupView> { CreateNewGroupViewImpl() }
    single<UsersInGroupView> { UsersInGroupViewImpl() }
    single<CreateGroupOptionsView> { CreateGroupOptionsViewImpl() }
}

fun searchViewModule() = module {
    single<SearchGroupView> { SearchGroupViewImpl() }
    single<SearchUsersView> { SearchUsersViewImpl() }
}

fun roomViewModule() = module {
    single<RoomListElementView> { RoomListElementViewImpl() }
    single<SearchUsersSettingsView> { SearchUsersSettingsViewImpl() }
}

fun roomSettingsViewModule() = module {
    single<RoomSettingsView> { RoomSettingsViewImpl() }
    single<TimelineElementMetadataView> { TimelineElementMetadataViewImpl() }
    single<ChangeRoomAvatarView> { ChangeRoomAvatarViewImpl() }
    single<RoomSettingsNameView> { RoomSettingsNameViewImpl() }
    single<RoomSettingsTopicView> { RoomSettingsTopicViewImpl() }
    single<RoomSettingsMemberListView> { RoomSettingsMemberListViewImpl() }
    single<RoomSettingsMemberListElementView> { RoomSettingsMemberListElementViewImpl() }
    single<RoomSettingsSecurityView> { RoomSettingsSecurityViewImpl() }
    single<RoomSettingsNotificationsView> { RoomSettingsNotificationsViewImpl() }
    single<RoomSettingsExportRoomView> { RoomSettingsExportRoomViewImpl() }
    single<RoomSettingsLeaveRoomView> { RoomSettingsLeaveRoomViewImpl() }
    single<RoomSettingsHistoryVisibilityView> { RoomSettingsHistoryVisibilityViewImpl() }
    single<RoomSettingsJoinRulesView> { RoomSettingsJoinRulesViewImpl() }
    single<ExportRoomView> { ExportRoomViewImpl() }
    single<AddMembersToRoomView> { AddMembersToRoomViewImpl() }
    single<UserProfileView> { UserProfileViewImpl() }
}

inline fun <reified F : TimelineElementView<*>> Module.timelineElementView(
    noinline definition: Scope.(ParametersHolder) -> F
) = single<F>(named<F>(), definition = definition).bind<TimelineElementView<*>>()

inline fun <reified F : TimelineElementDetailsView<*>> Module.timelineElementDetailsView(
    noinline definition: Scope.(ParametersHolder) -> F
) = single<F>(named<F>(), definition = definition).bind<TimelineElementDetailsView<*>>()

fun timelineViewModule() = module {
    timelineElementView<EncryptedErrorTimelineElementView> { EncryptedErrorTimelineElementView() }
    timelineElementView<EncryptedWaitTimelineElementView> { EncryptedWaitTimelineElementView() }
    timelineElementView<RedactedTimelineElementView> { RedactedTimelineElementView() }
    timelineElementView<AudioRoomMessageTimelineElementView> { AudioRoomMessageTimelineElementView() }
    timelineElementView<EmoteRoomMessageTimelineElementView> { EmoteRoomMessageTimelineElementView() }
    timelineElementView<FileRoomMessageTimelineElementView> { FileRoomMessageTimelineElementView() }
    timelineElementView<ImageRoomMessageTimelineElementView> { ImageRoomMessageTimelineElementView() }
    timelineElementView<LocationRoomMessageTimelineElementView> { LocationRoomMessageTimelineElementView() }
    timelineElementView<NoticeRoomMessageTimelineElementView> { NoticeRoomMessageTimelineElementView() }
    timelineElementView<TextRoomMessageTimelineElementView> { TextRoomMessageTimelineElementView() }
    timelineElementView<VideoRoomMessageTimelineElementView> { VideoRoomMessageTimelineElementView() }
    timelineElementView<VerificationRequestRoomMessageTimelineElementView> { VerificationRequestRoomMessageTimelineElementView() }
    timelineElementView<UnknownRoomMessageTimelineElementView> { UnknownRoomMessageTimelineElementView() }
    timelineElementView<AvatarStateTimelineElementView> { AvatarStateTimelineElementView() }
    timelineElementView<CanonicalAliasStateTimelineElementView> { CanonicalAliasStateTimelineElementView() }
    timelineElementView<CreateStateTimelineElementView> { CreateStateTimelineElementView() }
    timelineElementView<EncryptionStateTimelineElementView> { EncryptionStateTimelineElementView() }
    timelineElementView<HistoryVisibilityStateTimelineElementView> { HistoryVisibilityStateTimelineElementView() }
    timelineElementView<MemberStateTimelineElementView> { MemberStateTimelineElementView() }
    timelineElementView<NameStateTimelineElementView> { NameStateTimelineElementView() }
    timelineElementView<TopicStateTimelineElementView> { TopicStateTimelineElementView() }
    timelineElementDetailsView<ImageTimelineElementDetailsView> { ImageTimelineElementDetailsView() }
    // TODO enable when fixed and all platforms supported
    // timelineElementDetailsView<PdfTimelineElementDetailsView> { PdfTimelineElementDetailsView() }
    single<TimelineElementViewSelector> { TimelineElementViewSelectorImpl(getAll()) }
    single<ElementDetailsViewSelector> { ElementDetailsViewSelectorImpl(getAll()) }

    single<RoomHeaderView> { RoomHeaderViewImpl() }
    single<InputAreaView> { InputAreaViewImpl() }
    single<TimelineView> { TimelineViewImpl() }
    single<TimelineElementHolderView> { TimelineElementHolderViewImpl() }
    single<ScrollToEndButtonView> { ScrollToEndButtonViewImpl() }
    single<MessageBubbleView> { MessageBubbleViewImpl() }
    single<MessageReactionsView> { MessageReactionsViewImpl() }
    single<FileBasedRoomMessageTimelineElementView> { FileBasedRoomMessageTimelineElementViewImpl() }
    single<ReadMarkerView> { ReadMarkerViewImpl() }
    single<ReplyToAreaView> { ReplyToAreaViewImpl() }
    single<TextReplyView> { TextReplyViewImpl() }
    single<ImageReplyView> { ImageReplyViewImpl() }
    single<ImageReplyDefaultView> { ImageReplyDefaultViewImpl() }
    single<VideoReplyView> { VideoReplyViewImpl() }
    single<VideoReplyDefaultView> { VideoReplyDefaultViewImpl() }
    single<AudioReplyView> { AudioReplyViewImpl() }
    single<FileReplyView> { FileReplyViewImpl() }
    single<LocationReplyView> { LocationReplyViewImpl() }
    single<UnknownReplyView> { UnknownReplyViewImpl() }
    single<SendAttachmentSendButtonView> { SendAttachmentSendButtonViewImpl() }
    single<SendAttachmentView> { SendAttachmentViewImpl() }
    single<SendAttachmentTitleView> { SendAttachmentTitleViewImpl() }
    single<TypingIndicatorView> { TypingIndicatorViewImpl() }
}

fun userSearchViewModule() = module {
    single<UserSearchFieldView> { UserSearchFieldViewImpl() }
    single<UserSearchResultListView> { UserSearchResultListViewImpl() }
}

fun settingsViewModule() = module {
    single<AccountsOverviewView> { AccountsOverviewViewImpl() }
    single<AppearanceSettingsView> { AppearanceSettingsViewImpl() }
    single<AppearanceSettingsThemeView> { AppearanceSettingsThemeViewImpl() }
    single<AppearanceSettingsColorView> { AppearanceSettingsColorViewImpl() }
    single<AppearanceSettingsSizeView> { AppearanceSettingsSizeViewImpl() }
    single<AppInfoView> { AppInfoViewImpl() }
    single<AppInfoVersionView> { AppInfoVersionViewImpl() }
    single<AppInfoPrivacyView> { AppInfoPrivacyViewImpl() }
    single<AppInfoImprintView> { AppInfoImprintViewImpl() }
    single<AppInfoLicensesView> { AppInfoLicensesViewImpl() }
    single<AvatarCutterView> { AvatarCutterViewImpl() }
    single<BlockedContactsSettingsView> { BlockedContactsSettingsViewImpl() }
    single<DevicesSettingsView> { DevicesSettingsViewImpl() }
    single<NotificationsSettingsView> { NotificationsSettingsViewImpl() }
    single<PrivacySettingsView> { PrivacySettingsViewImpl() }
    single<ProfileSettingsView> { ProfileSettingsViewImpl() }
    single<UserSettingsView> { UserSettingsViewImpl() }
    single<AdditionalAccountSetupWizardStep> { AdditionalAccountSetupWizardStepImpl() }
    single<AccountSetupWizardStepList> { AccountSetupWizardStepListImpl() }
    single<SelfVerificationWizardView> { SelfVerificationWizardViewImpl() }
    single<DeviceVerificationWizardView> { DeviceVerificationWizardViewImpl() }
}

fun verificationViewModule() = module {
    single<RedoSelfVerificationWizardView> { RedoSelfVerificationWizardViewImpl() }
}

fun uiaViewModule() = module {
    single<UiaModalBoxView> { UiaModalBoxViewImpl() }
    single<UiaPasswordInputView> { UiaPasswordInputViewImpl() }
    single<UiaRegistrationTokenView> { UiaRegistrationTokenViewImpl() }
    single<UiaFallbackFlowView> { UiaFallbackFlowViewImpl() }
    single<UiaDummyStepView> { UiaDummyStepViewImpl() }
    single<UiaActionConfirmationView> { UiaActionConfirmationViewImpl() }
    single<UiaEmailIdentityStepView> { UiaEmailIdentityStepViewImpl() }
    single<UiaMsisdnStepView> { UiaMsisdnStepViewImpl() }
}
