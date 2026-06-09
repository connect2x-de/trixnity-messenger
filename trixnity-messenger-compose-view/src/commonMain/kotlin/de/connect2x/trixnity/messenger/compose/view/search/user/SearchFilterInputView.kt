package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilter
import kotlin.reflect.KClass

interface SearchFilterInputView<S : SearchFilter.Key<*>> {
    val supports: KClass<out S>

    @Composable fun create(userSearchViewModel: UserSearchViewModel, searchFilterKey: SearchFilter.Key<*>)
}

object EmptySearchFilterInputView : SearchFilterInputView<SearchFilter.Key<*>> {
    override val supports: KClass<SearchFilter.Key<*>>
        get() = SearchFilter.Key::class

    @Composable override fun create(userSearchViewModel: UserSearchViewModel, searchFilterKey: SearchFilter.Key<*>) {}
}
