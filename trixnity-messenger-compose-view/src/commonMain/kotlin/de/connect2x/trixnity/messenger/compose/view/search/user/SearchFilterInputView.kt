package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchViewModel
import de.connect2x.trixnity.messenger.viewmodel.search.provider.SearchFilterValue
import kotlin.reflect.KClass

interface SearchFilterInputView<S : SearchFilterValue.Key<*>> {
    val supports: KClass<out S>

    @Composable fun create(userSearchViewModel: UserSearchViewModel, searchFilterValueKey: SearchFilterValue.Key<*>)
}

object EmptySearchFilterInputView : SearchFilterInputView<SearchFilterValue.Key<*>> {
    override val supports: KClass<SearchFilterValue.Key<*>>
        get() = SearchFilterValue.Key::class

    @Composable
    override fun create(userSearchViewModel: UserSearchViewModel, searchFilterValueKey: SearchFilterValue.Key<*>) {}
}
