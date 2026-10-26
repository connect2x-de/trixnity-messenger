package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.AccountViewModelFactory
import org.koin.core.component.get

interface SpaceBrowserViewModel {
    val content: SpaceContentViewModel
    val account: AccountViewModel
}

interface SpaceBrowserViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onSelect: (SpaceBrowserItem) -> Unit,
        onBack: () -> Unit,
        onAccountSelected: (UserId?) -> Unit,
        onUserSettingsSelected: () -> Unit,
        onShowAppInfo: () -> Unit,
        onShowAccounts: () -> Unit,
    ): SpaceBrowserViewModel =
        SpaceBrowserViewModelImpl(
            content =
                viewModelContext
                    .get<SpaceContentViewModelFactory>()
                    .create(viewModelContext.childContext("Content"), null, null, onSelect, onBack),
            account =
                viewModelContext
                    .get<AccountViewModelFactory>()
                    .create(
                        viewModelContext.childContext("Account"),
                        onAccountSelected,
                        onUserSettingsSelected,
                        onShowAppInfo,
                        onShowAccounts,
                    ),
        )

    companion object : SpaceBrowserViewModelFactory
}

internal class SpaceBrowserViewModelImpl(
    override val content: SpaceContentViewModel,
    override val account: AccountViewModel,
) : SpaceBrowserViewModel
