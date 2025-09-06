package de.connect2x.messenger.compose.view.search

import de.connect2x.trixnity.messenger.util.Search

interface SearchResultState {
    object Loading : SearchResultState
    object Placeholder : SearchResultState
    data class Results(val users: List<Search.SearchUserElement>) : SearchResultState
}
