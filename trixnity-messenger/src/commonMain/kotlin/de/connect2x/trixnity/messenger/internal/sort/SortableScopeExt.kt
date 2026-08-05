package de.connect2x.trixnity.messenger.internal.sort

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerPrivateApi

@TrixnityMessengerPrivateApi
inline fun <reified I : Any> SortableScope<I>.after() {
    after(I::class)
}

@TrixnityMessengerPrivateApi
inline fun <reified I : Any> SortableScope<I>.before() {
    before(I::class)
}
