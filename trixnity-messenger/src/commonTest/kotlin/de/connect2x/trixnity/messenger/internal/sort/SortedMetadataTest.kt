package de.connect2x.trixnity.messenger.internal.sort

import kotlin.test.Test
import kotlin.test.assertEquals

class SortedMetadataTest {

    @Test
    fun `SortedMetadata should contain before and after`() {
        val metadata =
            SortedMetadata(Base::class) {
                before<A>()
                after<B>()
                before<C>()
                after<D>()
            }

        assertEquals(Base::class, metadata.clazz)
        assertEquals(setOf(A::class, C::class), metadata.before)
        assertEquals(setOf(B::class, D::class), metadata.after)
    }

    private interface Base

    private interface A : Base

    private interface B : Base

    private interface C : Base

    private interface D : Base
}
