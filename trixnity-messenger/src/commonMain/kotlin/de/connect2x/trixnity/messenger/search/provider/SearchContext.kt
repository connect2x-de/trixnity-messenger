package de.connect2x.trixnity.messenger.search.provider

/**
 * Any context that the search needs. For a user search this might be the current account for which the homeserver
 * search is executed.
 */
interface SearchContext

data object EmptySearchContext : SearchContext
