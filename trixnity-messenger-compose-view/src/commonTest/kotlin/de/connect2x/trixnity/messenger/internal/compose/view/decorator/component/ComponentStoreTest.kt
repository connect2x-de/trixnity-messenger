package de.connect2x.trixnity.messenger.internal.compose.view.decorator.component

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import de.connect2x.trixnity.messenger.internal.component.CloseableComponent
import de.connect2x.trixnity.messenger.internal.component.CloseableComponentFactory
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ComponentStoreTest {
    private class Browser

    private class Other {
        class Browser
    }

    @Test
    fun sameNamedComponentTypesAreCachedSeparatelyAndBothAreClosed() {
        assertEquals(Browser::class.simpleName, Other.Browser::class.simpleName)
        val browser = Browser()
        val management = Other.Browser()
        val created = mutableListOf<KClass<*>>()
        val closed = mutableListOf<KClass<*>>()
        val owner =
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        val factory =
            object : CloseableComponentFactory {
                override fun <T : Any> create(clazz: KClass<out T>, parameters: Set<Any?>): CloseableComponent<T> {
                    created += clazz
                    assertEquals(setOf("route"), parameters)
                    return object : CloseableComponent<T> {
                        @Suppress("UNCHECKED_CAST")
                        override val value =
                            when (clazz) {
                                Browser::class -> browser
                                Other.Browser::class -> management
                                else -> error("Unexpected component")
                            }
                                as T

                        override fun close() {
                            closed += clazz
                        }
                    }
                }
            }
        val store = ComponentStore(owner, factory, setOf("route"))
        assertSame(browser, store.getOrCreate(Browser::class))
        assertSame(management, store.getOrCreate(Other.Browser::class))
        val restored = ComponentStore(owner, factory, setOf("route"))
        assertSame(browser, restored.getOrCreate(Browser::class))
        assertSame(management, restored.getOrCreate(Other.Browser::class))
        assertEquals(listOf(Browser::class, Other.Browser::class), created)
        owner.viewModelStore.clear()
        assertEquals(setOf(Browser::class, Other.Browser::class), closed.toSet())
        assertEquals(2, closed.size)
    }
}
