package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
interface Sorter<T> {
    fun sort(items: Collection<T>): List<T>
}
