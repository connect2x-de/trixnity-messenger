package de.connect2x.trixnity.messenger.search.provider

/**
 * Any context that the search needs. For a user search this might be the current account for which the homeserver
 * search is executed, when searching for messages in a room, this might be the roomId.
 */
interface SearchContext

data object EmptySearchContext : SearchContext
