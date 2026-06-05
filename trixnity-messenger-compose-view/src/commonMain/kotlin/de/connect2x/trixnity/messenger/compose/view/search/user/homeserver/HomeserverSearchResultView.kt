package de.connect2x.trixnity.messenger.compose.view.search.user.homeserver

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.search.user.SearchResultView
import de.connect2x.trixnity.messenger.viewmodel.search.provider.homeserver.HomeserverUserSearchResult
import kotlin.reflect.KClass

class HomeserverSearchResultView : SearchResultView<HomeserverUserSearchResult> {
    override val supports: KClass<out HomeserverUserSearchResult> = HomeserverUserSearchResult::class

    @Composable
    override fun create(
        userSearchResult: HomeserverUserSearchResult,
        showOrigin: Boolean,
        onClick: (HomeserverUserSearchResult) -> Unit,
    ) {
        HomeserverSearchResultElement(userSearchResult, showOrigin, onClick)
    }
}
