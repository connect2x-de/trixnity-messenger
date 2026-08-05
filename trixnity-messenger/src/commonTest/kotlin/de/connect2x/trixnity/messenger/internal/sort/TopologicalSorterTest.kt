package de.connect2x.trixnity.messenger.internal.sort

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TopologicalSorterTest {

    @Test
    fun `TopologicalSorter should not allow self references`() {
        val sorter = TopologicalSorter<Base>(metadata = listOf(SortedMetadata<Base>(A::class) { before<A>() }))

        val failure = assertFailsWith<IllegalArgumentException> { sorter.sort(listOf(AImpl)) }
        assertEquals("self reference is not allowed", failure.message)
    }

    @Test
    fun `TopologicalSorter should not allow loops`() {
        val sorter =
            TopologicalSorter<Base>(
                metadata =
                    listOf(
                        SortedMetadata<Base>(A::class) { before<B>() },
                        SortedMetadata<Base>(B::class) { before<A>() },
                    )
            )

        val failure = assertFailsWith<IllegalArgumentException> { sorter.sort(listOf(BImpl, AImpl)) }
        assertEquals("dependency cycle: ${listOf(B::class, A::class)}", failure.message)
    }

    @Test
    fun `TopologicalSorter should sort when everything is explicit`() {
        val sorter =
            TopologicalSorter<Base>(
                metadata =
                    listOf(
                        SortedMetadata<Base>(A::class) { before<B>() },
                        SortedMetadata<Base>(B::class) {
                            after<A>()
                            before<C>()
                        },
                        SortedMetadata<Base>(C::class) { after<B>() },
                    )
            )

        assertEquals(listOf(AImpl, BImpl, CImpl), sorter.sort(listOf(CImpl, BImpl, AImpl)))
    }

    @Test
    fun `TopologicalSorter should sort when before is explicit`() {
        val sorter =
            TopologicalSorter<Base>(
                metadata =
                    listOf(
                        SortedMetadata<Base>(A::class) { before<B>() },
                        SortedMetadata<Base>(B::class) { before<C>() },
                        SortedMetadata<Base>(C::class) {},
                    )
            )

        assertEquals(listOf(AImpl, BImpl, CImpl), sorter.sort(listOf(CImpl, BImpl, AImpl)))
    }

    @Test
    fun `TopologicalSorter should sort when after is explicit`() {
        val sorter =
            TopologicalSorter<Base>(
                metadata =
                    listOf(
                        SortedMetadata<Base>(A::class) {},
                        SortedMetadata<Base>(B::class) { after<A>() },
                        SortedMetadata<Base>(C::class) { after<B>() },
                    )
            )

        assertEquals(listOf(AImpl, BImpl, CImpl), sorter.sort(listOf(CImpl, BImpl, AImpl)))
    }

    @Test
    fun `TopologicalSorter should preserve order when multiple orders exist`() {
        val sorter =
            TopologicalSorter<Base>(
                metadata =
                    listOf(
                        SortedMetadata<Base>(A::class) {},
                        SortedMetadata<Base>(B::class) { after<A>() },
                        SortedMetadata<Base>(C::class) { after<A>() },
                    )
            )

        assertEquals(listOf(AImpl, CImpl, BImpl), sorter.sort(listOf(CImpl, BImpl, AImpl)))

        assertEquals(listOf(AImpl, BImpl, CImpl), sorter.sort(listOf(BImpl, CImpl, AImpl)))
    }

    @Test
    fun `TopologicalSorter should allow insertion between items`() {
        val sorter =
            TopologicalSorter<Base>(
                metadata =
                    listOf(
                        SortedMetadata<Base>(A::class) { before<B>() },
                        SortedMetadata<Base>(B::class) { after<A>() },
                        SortedMetadata<Base>(C::class) {
                            after<A>()
                            before<B>()
                        },
                    )
            )

        assertEquals(listOf(AImpl, CImpl, BImpl), sorter.sort(listOf(CImpl, BImpl, AImpl)))
    }

    private interface Base

    private interface A : Base

    private interface B : Base

    private interface C : Base

    private data object AImpl : A

    private data object BImpl : B

    private data object CImpl : C
}
