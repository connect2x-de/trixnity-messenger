package de.connect2x.trixnity.messenger.internal.sort

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class SortableScopeTest {

    @Test
    fun `SortableScope should track before and after`() {
        val fixture = Fixture()

        assertEquals(emptySet(), fixture.before)
        assertEquals(emptySet(), fixture.after)

        fixture.scope.before<A>()

        assertEquals(setOf(A::class), fixture.before)
        assertEquals(emptySet(), fixture.after)

        fixture.scope.before<A>()

        assertEquals(setOf(A::class), fixture.before)
        assertEquals(emptySet(), fixture.after)

        fixture.scope.after<B>()

        assertEquals(setOf(A::class), fixture.before)
        assertEquals(setOf(B::class), fixture.after)

        fixture.scope.before<C>()
        fixture.scope.after<D>()

        assertEquals(setOf(A::class, C::class), fixture.before)
        assertEquals(setOf(B::class, D::class), fixture.after)
    }

    private fun Fixture(
        before: Set<KClass<out Base>> = emptySet(),
        after: Set<KClass<out Base>> = emptySet(),
    ): Fixture {
        val before = before.toMutableSet()
        val after = after.toMutableSet()

        return Fixture(before = before, after = after, scope = SortableScope(before = before, after = after))
    }

    private class Fixture(
        val before: Set<KClass<out Base>>,
        val after: Set<KClass<out Base>>,
        val scope: SortableScope<Base>,
    )

    private interface Base

    private interface A : Base

    private interface B : Base

    private interface C : Base

    private interface D : Base
}
