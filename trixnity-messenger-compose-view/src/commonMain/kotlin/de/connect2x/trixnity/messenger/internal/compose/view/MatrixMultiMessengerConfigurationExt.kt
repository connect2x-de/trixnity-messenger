package de.connect2x.trixnity.messenger.internal.compose.view

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.compose.view.ClientView
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.EntryDecoratorFactory
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.component.ComponentStoreNavEntryDecoratorFactory
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.entryDecoratorOf
import de.connect2x.trixnity.messenger.internal.compose.view.decorator.saveable.SaveableStateHolderNavEntryDecoratorFactory
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AccountSetupEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AccountsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AddMatrixAccountEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AddMembersEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AppInfoEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AppearanceSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.AvatarCutterEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.BlockedContactsSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.CreateNewChatEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.CreateNewGroupEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.CrossSigningBootstrapEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.DeviceSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.ExportRoomEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.JoinRoomActionEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.MatrixClientInitializationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.MatrixClientInitializationFailureEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.NotificationSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.OAuth2AuthorizationCodeLoginEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.OAuth2DeviceAuthorizationLoginEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.PasswordLoginEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.PowerlevelEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.PrivacySettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.ProfilesSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.RedoSelfVerificationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.RegisterMatrixAccountEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.RemoveMatrixAccountEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.ReportMessageEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.RoomDevInfoEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.RoomListEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.RoomSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SSOLoginEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SearchGroupEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SelfVerificationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SendAttachmentEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.ShareDataEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.SyncEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.TimelineElementDevInfoEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.TimelineElementMetadataEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.TimelineEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UiaActionConfirmationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UiaStepDummyEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UiaStepEmailIdentityEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UiaStepFallbackEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UiaStepMsisdnEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UiaStepPasswordEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UiaStepRegistrationTokenEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UserProfileEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.UserSettingsEntry
import de.connect2x.trixnity.messenger.internal.compose.view.entries.VerificationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.AnyNavigationEntry
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.EntryProvider
import de.connect2x.trixnity.messenger.internal.compose.view.navigation.navigationOf
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.SceneStrategyFactory
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.overlay.OverlaySceneStrategyFactory
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.sceneStrategyOf
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.singlepane.SinglePaneSceneStrategyFactory
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.threepane.ThreePaneSceneStrategyFactory
import de.connect2x.trixnity.messenger.internal.compose.view.scenes.twopane.TwoPaneSceneStrategyFactory
import de.connect2x.trixnity.messenger.internal.nav3ViewModelOptIn
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.sort.after
import de.connect2x.trixnity.messenger.internal.sort.before
import de.connect2x.trixnity.messenger.internal.sort.getSorted
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.util.BackHandler
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

@TrixnityMessengerPrivateApi
fun MatrixMultiMessengerConfiguration.nav3ViewOptIn() {
    nav3ViewModelOptIn()

    modulesFactories += { module { singleOf(::WithProfileSelectionView) } }

    messengerConfiguration { modulesFactories += ::nav3ViewModuleFactory }
}

private fun nav3ViewModuleFactory(): Module {
    return module {
        singleOf(::ClientView)
        single<NavigationView> {
            NavigationView(
                backHandler = get<BackHandler>(),
                navigation = get<GetRouteNavigation>(),
                entryDecorators = getSorted<EntryDecoratorFactory>(),
                sceneStrategies = getSorted<SceneStrategyFactory>(),
                entryProvider = EntryProvider(entries = getAll<AnyNavigationEntry<*>>()),
            )
        }

        sceneStrategyOf(::OverlaySceneStrategyFactory) { before<ThreePaneSceneStrategyFactory>() }
        sceneStrategyOf(::ThreePaneSceneStrategyFactory) {
            after<OverlaySceneStrategyFactory>()
            before<TwoPaneSceneStrategyFactory>()
        }
        sceneStrategyOf(::TwoPaneSceneStrategyFactory) {
            after<ThreePaneSceneStrategyFactory>()
            before<SinglePaneSceneStrategyFactory>()
        }
        sceneStrategyOf(::SinglePaneSceneStrategyFactory) { after<ThreePaneSceneStrategyFactory>() }

        entryDecoratorOf(::SaveableStateHolderNavEntryDecoratorFactory)
        entryDecoratorOf(::ComponentStoreNavEntryDecoratorFactory)

        navigationOf(::AccountsEntry)
        navigationOf(::AccountSetupEntry)
        navigationOf(::AddMatrixAccountEntry)
        navigationOf(::AddMembersEntry)
        navigationOf(::AppearanceSettingsEntry)
        navigationOf(::AppInfoEntry)
        navigationOf(::AvatarCutterEntry)
        navigationOf(::BlockedContactsSettingsEntry)
        navigationOf(::CreateNewChatEntry)
        navigationOf(::CreateNewGroupEntry)
        navigationOf(::CrossSigningBootstrapEntry)
        navigationOf(::DeviceSettingsEntry)
        navigationOf(::ExportRoomEntry)
        navigationOf(::JoinRoomActionEntry)
        navigationOf(::MatrixClientInitializationEntry)
        navigationOf(::MatrixClientInitializationFailureEntry)
        navigationOf(::NotificationSettingsEntry)
        navigationOf(::OAuth2AuthorizationCodeLoginEntry)
        navigationOf(::OAuth2DeviceAuthorizationLoginEntry)
        navigationOf(::PasswordLoginEntry)
        navigationOf(::PowerlevelEntry)
        navigationOf(::PrivacySettingsEntry)
        navigationOf(::ProfilesSettingsEntry)
        navigationOf(::RedoSelfVerificationEntry)
        navigationOf(::RegisterMatrixAccountEntry)
        navigationOf(::RemoveMatrixAccountEntry)
        navigationOf(::ReportMessageEntry)
        navigationOf(::RoomDevInfoEntry)
        navigationOf(::RoomListEntry)
        navigationOf(::RoomSettingsEntry)
        navigationOf(::SearchGroupEntry)
        navigationOf(::SelfVerificationEntry)
        navigationOf(::SendAttachmentEntry)
        navigationOf(::ShareDataEntry)
        navigationOf(::SSOLoginEntry)
        navigationOf(::SyncEntry)
        navigationOf(::TimelineElementDevInfoEntry)
        navigationOf(::TimelineElementMetadataEntry)
        navigationOf(::TimelineEntry)
        navigationOf(::UiaActionConfirmationEntry)
        navigationOf(::UiaStepDummyEntry)
        navigationOf(::UiaStepEmailIdentityEntry)
        navigationOf(::UiaStepFallbackEntry)
        navigationOf(::UiaStepMsisdnEntry)
        navigationOf(::UiaStepPasswordEntry)
        navigationOf(::UiaStepRegistrationTokenEntry)
        navigationOf(::UserProfileEntry)
        navigationOf(::UserSettingsEntry)
        navigationOf(::VerificationEntry)
    }
}
