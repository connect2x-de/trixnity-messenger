package de.connect2x.trixnity.messenger.util

/**
 * Returns the number of grapheme clusters
 * (or perceived characters) of this string instance.
 * This takes into account ZWJs, modifiers and diacritics.
 */
expect val String.graphemeClusters: Int
