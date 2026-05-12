package de.connect2x.trixnity.messenger.compose.view.roomlist.search

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.viewmodel.search.UserSearchResult
import kotlin.reflect.KClass

interface SearchResultView<V : UserSearchResult> {
    val supports: KClass<out V>

    @Composable
    fun create(userSearchResult: V, showOrigin: Boolean, onClick: (V) -> Unit)
}

object EmptySearchResultView : SearchResultView<UserSearchResult> {
    override val supports: KClass<out UserSearchResult>
        get() = TODO("Not yet implemented")

    @Composable
    override fun create(userSearchResult: UserSearchResult, showOrigin: Boolean, onClick: (UserSearchResult) -> Unit) {
        // TODO empty
    }


}
