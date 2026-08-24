package de.connect2x.trixnity.messenger.internal

import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.Worker
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi
import de.connect2x.trixnity.messenger.internal.adapters.AccountSetupViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.AccountsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.AddMatrixAccountViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.AddMembersViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.AppInfoViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.AppearanceSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.AvatarCutterViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.BlockedContactsSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.CreateNewChatViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.CreateNewGroupViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.CrossSigningBootstrapViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.DeviceSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.ExportRoomViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.JoinRoomActionViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.MatrixClientInitializationFailureViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.MatrixClientInitializationViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.NotificationSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.OAuth2AuthorizationCodeLoginViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.OAuth2DeviceAuthorizationLoginViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.PasswordLoginViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.PowerlevelViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.PrivacySettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.ProfilesSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.RedoSelfVerificationViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.RegisterMatrixAccountViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.RemoveMatrixAccountViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.ReportMessageViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.RoomDevInfoViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.RoomListViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.RoomSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SSOLoginViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SearchGroupViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SelfVerificationViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SendAttachmentViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.ShareDataViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.SyncViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.TimelineElementDevInfoViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.TimelineElementMetadataViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.TimelineViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UiaActionConfirmationViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UiaStepDummyViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UiaStepEmailIdentityViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UiaStepFallbackViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UiaStepMsisdnViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UiaStepPasswordViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UiaStepRegistrationTokenViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UserProfileViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.UserSettingsViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.adapters.VerificationViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory
import de.connect2x.trixnity.messenger.internal.factories.UnsupportedMainViewModelFactory
import de.connect2x.trixnity.messenger.internal.factories.UnsupportedRoomViewModelFactory
import de.connect2x.trixnity.messenger.internal.factories.WorkerBasedSyncViewModelFactory
import de.connect2x.trixnity.messenger.internal.logic.AccountSetupFinishedLogic
import de.connect2x.trixnity.messenger.internal.logic.AccountSyncStatesLogic
import de.connect2x.trixnity.messenger.internal.logic.ActiveDeviceVerificationStatesLogic
import de.connect2x.trixnity.messenger.internal.logic.MatrixClientSelfVerificationMethodsLogic
import de.connect2x.trixnity.messenger.internal.logic.OpenMentionLogic
import de.connect2x.trixnity.messenger.internal.logic.ReportMessageLogic
import de.connect2x.trixnity.messenger.internal.logic.SelectedRoomIdLogic
import de.connect2x.trixnity.messenger.internal.logic.SendAttachmentLogic
import de.connect2x.trixnity.messenger.internal.logic.SendLogsLogic
import de.connect2x.trixnity.messenger.internal.navigation.DefaultInitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.GetRouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ResultEventBus
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.UpdateRouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.viewModelFactoryAdapterOf
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.internal.uia.UIALogic
import de.connect2x.trixnity.messenger.internal.uia.UIANavigator
import de.connect2x.trixnity.messenger.internal.uia.UIASessionHolder
import de.connect2x.trixnity.messenger.internal.uia.UIAStateHolder
import de.connect2x.trixnity.messenger.internal.util.AnySelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.internal.util.CloseAppIfPossible
import de.connect2x.trixnity.messenger.internal.util.MinimizeAppIfPossible
import de.connect2x.trixnity.messenger.internal.util.SendLogToDevsIfPossible
import de.connect2x.trixnity.messenger.internal.workers.CrossSigningBootstrapWorker
import de.connect2x.trixnity.messenger.internal.workers.DeviceVerificationWorker
import de.connect2x.trixnity.messenger.internal.workers.FallbackBackCallbackWorker
import de.connect2x.trixnity.messenger.internal.workers.InitialSyncWorker
import de.connect2x.trixnity.messenger.internal.workers.OAuth2AuthorizationCodeLoginWorker
import de.connect2x.trixnity.messenger.internal.workers.RequestNotificationPermissionsWorker
import de.connect2x.trixnity.messenger.internal.workers.SSOLoginWorker
import de.connect2x.trixnity.messenger.internal.workers.SharedDataWorker
import de.connect2x.trixnity.messenger.internal.workers.ShowAccountSetupWorker
import de.connect2x.trixnity.messenger.internal.workers.ShowSyncWorker
import de.connect2x.trixnity.messenger.internal.workers.SyncService
import de.connect2x.trixnity.messenger.internal.workers.UIAWorker
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import de.connect2x.trixnity.messenger.util.CloseApp
import de.connect2x.trixnity.messenger.util.MinimizeApp
import de.connect2x.trixnity.messenger.util.SendLogToDevs
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.onOptions
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

@OptIn(InternalDecomposeApi::class)
@TrixnityMessengerPrivateApi
fun MatrixMultiMessengerConfiguration.nav3ViewModelOptIn() {
    modulesFactories += ::nav3OptInModuleFactory

    messengerConfiguration {
        modulesFactories += ::nav3OptInModuleFactory
        modulesFactories += ::nav3ViewModelModuleFactory
    }
}

private fun nav3ViewModelModuleFactory(): Module {
    return module {
        single<CloseAppIfPossible> { CloseAppIfPossible(closeApp = getOrNull<CloseApp>()) }
        single<MinimizeAppIfPossible> { MinimizeAppIfPossible(minimizeApp = getOrNull<MinimizeApp>()) }
        single<SendLogToDevsIfPossible> { SendLogToDevsIfPossible(sendLogToDevs = getOrNull<SendLogToDevs>()) }
        single<AnySelfVerificationViewModelFactory> {
            AnySelfVerificationViewModelFactory(
                matrixMessengerConfiguration = get<MatrixMessengerConfiguration>(),
                selfVerificationV1ViewModelFactory =
                    get<
                        @Suppress("DEPRECATION")
                        de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory
                    >(
                        named("v1")
                    ),
                selfVerificationV2ViewModelFactory =
                    get<de.connect2x.trixnity.messenger.viewmodel.verification.v2.SelfVerificationViewModelFactory>(),
            )
        }

        single<LifecycleRegistry> { LifecycleRegistry() }.onOptions { bind<Lifecycle>() }

        single<CloseableComponentFactory> {
            CloseableComponentFactory(appLifecycle = get<Lifecycle>(), koin = getKoin())
        }

        viewModelFactoryAdapterOf(::AccountSetupViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::AccountsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::AddMatrixAccountViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::AddMembersViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::AppearanceSettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::AppInfoViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::AvatarCutterViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::BlockedContactsSettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::CreateNewChatViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::CreateNewGroupViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::CrossSigningBootstrapViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::DeviceSettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::ExportRoomViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::JoinRoomActionViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::MatrixClientInitializationFailureViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::MatrixClientInitializationViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::NotificationSettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::OAuth2AuthorizationCodeLoginViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::OAuth2DeviceAuthorizationLoginViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::PasswordLoginViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::PowerlevelViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::PrivacySettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::ProfilesSettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::RedoSelfVerificationViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::RegisterMatrixAccountViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::RemoveMatrixAccountViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::ReportMessageViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::RoomDevInfoViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::RoomListViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::RoomSettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::SearchGroupViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::SelfVerificationViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::SendAttachmentViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::ShareDataViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::SSOLoginViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::SyncViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::TimelineElementDevInfoViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::TimelineElementMetadataViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::TimelineViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UiaActionConfirmationViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UiaStepDummyViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UiaStepEmailIdentityViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UiaStepFallbackViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UiaStepMsisdnViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UiaStepPasswordViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UiaStepRegistrationTokenViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UserProfileViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::UserSettingsViewModelFactoryAdapter)
        viewModelFactoryAdapterOf(::VerificationViewModelFactoryAdapter)

        singleOf(::DefaultInitialRoutes)
        singleOf(::RouteNavigation) {
            bind<GetRouteNavigation>()
            bind<UpdateRouteNavigation>()
        }
        singleOf(::ResultEventBus)
        singleOf(::UIAStateHolder)
        singleOf(::UIANavigator)
        singleOf(::UIASessionHolder)

        singleOf(::UIAController)
        singleOf(::UIALogic)
        singleOf(::AccountSetupFinishedLogic)
        singleOf(::SendAttachmentLogic)
        singleOf(::SelectedRoomIdLogic)
        singleOf(::ReportMessageLogic)
        singleOf(::AccountSyncStatesLogic)
        singleOf(::OpenMentionLogic)
        singleOf(::ActiveDeviceVerificationStatesLogic)
        singleOf(::SendLogsLogic)

        singleOf(::WorkerBasedSyncViewModelFactory)
        singleOf(::UnsupportedRoomViewModelFactory)
        singleOf(::UnsupportedMainViewModelFactory)

        singleOf(::MatrixClientSelfVerificationMethodsLogic)

        singleOf(::SyncService) { bind<Worker>() }
        singleOf(::SSOLoginWorker) { bind<Worker>() }
        singleOf(::OAuth2AuthorizationCodeLoginWorker) { bind<Worker>() }
        singleOf(::SharedDataWorker) { bind<Worker>() }
        singleOf(::CrossSigningBootstrapWorker) { bind<Worker>() }
        singleOf(::InitialSyncWorker) { bind<Worker>() }
        singleOf(::DeviceVerificationWorker) { bind<Worker>() }
        singleOf(::UIAWorker) { bind<Worker>() }
        singleOf(::RequestNotificationPermissionsWorker) { bind<Worker>() }
        singleOf(::ShowSyncWorker) { bind<Worker>() }
        singleOf(::ShowAccountSetupWorker) { bind<Worker>() }
        singleOf(::FallbackBackCallbackWorker) { bind<Worker>() }
    }
}
