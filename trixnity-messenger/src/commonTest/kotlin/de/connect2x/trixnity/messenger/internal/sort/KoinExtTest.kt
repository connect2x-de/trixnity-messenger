package de.connect2x.trixnity.messenger.internal.sort

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class KoinExtTest {

    private val sortableModule = module {
        singleOf(::A).bind<Base>().sorted { before<B>() }

        singleOf(::C).bind<Base>().sorted { after<B>() }

        singleOf(::B).bind<Base>().sorted {
            after<A>()
            before<C>()
        }
    }

    private val notSortableModule = module {
        singleOf(::A).bind<Base>()
        singleOf(::C).bind<Base>()
        singleOf(::B).bind<Base>()
    }

    private val sortableMetadataModule = module {
        single(named<A>()) { SortedMetadata<Base>(A::class) { before<B>() } }
        single(named<C>()) { SortedMetadata<Base>(C::class) { after<B>() } }
        single(named<B>()) {
            SortedMetadata<Base>(B::class) {
                before<C>()
                after<A>()
            }
        }
    }

    private val sorterModule = module { singleOf(::AlphabeticalSorter) { named<Base>() } }

    private val dModule = module {
        singleOf(::D).bind<Base>().sorted {
            before<C>()
            after<B>()
        }
    }

    @Test
    fun `getSorted should sort using TopologicalSorted per default`() {
        val application = koinApplication { modules(sortableModule) }

        assertEquals(listOf(AImpl, BImpl, CImpl), application.koin.getSorted<Base>())
    }

    @Test
    fun `getSorted should fail when no Sorter is available`() {
        val application = koinApplication { modules(notSortableModule) }

        assertFails { application.koin.getSorted<Base>() }
    }

    @Test
    fun `SortedMetadata can be registered manually`() {
        val application = koinApplication { modules(notSortableModule, sortableMetadataModule) }

        assertEquals(listOf(AImpl, BImpl, CImpl), application.koin.getSorted<Base>())
    }

    @Test
    fun `Sorter can be manually registered instead of using TopologicalSorter`() {
        val application = koinApplication { modules(notSortableModule, sorterModule) }

        assertEquals(listOf(AImpl, BImpl, CImpl), application.koin.getSorted<Base>())
    }

    @Test
    fun `New elements can be added`() {
        val application = koinApplication { modules(sortableModule, dModule) }

        assertEquals(listOf(AImpl, BImpl, DImpl, CImpl), application.koin.getSorted<Base>())
    }

    private interface Base

    private interface A : Base

    private interface B : Base

    private interface C : Base

    private interface D : Base

    private data object AImpl : A

    private data object BImpl : B

    private data object CImpl : C

    private data object DImpl : D

    private fun A(): A = AImpl

    private fun B(): B = BImpl

    private fun C(): C = CImpl

    private fun D(): D = DImpl

    private object AlphabeticalSorter : Sorter<Base> {
        override fun sort(items: Collection<Base>): List<Base> {
            return items.sortedBy { it::class.simpleName }
        }
    }

    private fun AlphabeticalSorter(): Sorter<Base> = AlphabeticalSorter
}
