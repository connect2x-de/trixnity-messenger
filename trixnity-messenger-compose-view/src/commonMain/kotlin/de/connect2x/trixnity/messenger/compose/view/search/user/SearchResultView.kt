package de.connect2x.trixnity.messenger.compose.view.search.user

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.search.user.UserSearchResult
import kotlin.reflect.KClass

interface SearchResultView<V : UserSearchResult> {
    val supports: KClass<out V>

    @Composable
    fun create(
        userSearchResult: V,
        showOrigin: Boolean,
        index: Int,
        interactionSource: MutableInteractionSource,
        onClick: (V) -> Unit,
    )
}

object EmptySearchResultView : SearchResultView<UserSearchResult> {
    override val supports: KClass<out UserSearchResult> = UserSearchResult::class

    @Composable
    override fun create(
        userSearchResult: UserSearchResult,
        showOrigin: Boolean,
        index: Int,
        interactionSource: MutableInteractionSource,
        onClick: (UserSearchResult) -> Unit,
    ) {
        // empty
    }
}
