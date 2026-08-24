package de.connect2x.trixnity.messenger.internal.factories

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.MainViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext

internal fun UnsupportedMainViewModelFactory(): MainViewModelFactory {
    return UnsupportedMainViewModelFactoryImpl()
}

private class UnsupportedMainViewModelFactoryImpl : MainViewModelFactory {
    override fun create(
        viewModelContext: ViewModelContext,
        onCreateNewAccount: () -> Unit,
        onRemoveAccount: (userId: UserId) -> Unit,
    ): MainViewModel {
        error("MainViewModel is not supported when using nav3")
    }
}
