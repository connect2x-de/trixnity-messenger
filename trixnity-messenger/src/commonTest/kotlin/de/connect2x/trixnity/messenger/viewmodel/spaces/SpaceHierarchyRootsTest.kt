package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.core.model.RoomId
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SpaceHierarchyRootsTest {
    private fun room(name: String) = RoomId("!$name:example.org")

    private val a = room("a")
    private val b = room("b")
    private val c = room("c")
    private val d = room("d")
    private val e = room("e")
    private val z = room("z")
    private val unknown = room("unknown")

    @Test
    fun `empty hierarchy has no roots`() {
        spaceHierarchyRoots(emptyMap()) shouldBe SpaceHierarchyRoots(emptySet(), emptySet())
    }

    @Test
    fun `only known parents hide spaces`() {
        spaceHierarchyRoots(mapOf(a to setOf(b, unknown), b to emptySet(), c to emptySet())) shouldBe
            SpaceHierarchyRoots(setOf(a, c), emptySet())
    }

    @Test
    fun `shared descendants do not form a loop`() {
        spaceHierarchyRoots(mapOf(a to setOf(b, c), b to setOf(d), c to setOf(d), d to emptySet())) shouldBe
            SpaceHierarchyRoots(setOf(a), emptySet())
    }

    @Test
    fun `self links do not hide spaces`() {
        spaceHierarchyRoots(mapOf(a to setOf(a))) shouldBe SpaceHierarchyRoots(setOf(a), setOf(a))
    }

    @Test
    fun `all members of a loop become top level regardless of order`() {
        val graph = mapOf(b to setOf(a), a to setOf(b))
        spaceHierarchyRoots(graph) shouldBe SpaceHierarchyRoots(setOf(a, b), setOf(a, b))
        spaceHierarchyRoots(graph.entries.reversed().associate { it.toPair() }) shouldBe
            SpaceHierarchyRoots(setOf(a, b), setOf(a, b))
    }

    @Test
    fun `longer loop does not promote its descendants`() {
        spaceHierarchyRoots(mapOf(c to setOf(d), d to setOf(e, a), e to setOf(c), a to emptySet())) shouldBe
            SpaceHierarchyRoots(setOf(c, d, e), setOf(c, d, e))
    }

    @Test
    fun `loop beneath a root is also flattened`() {
        spaceHierarchyRoots(mapOf(z to setOf(b), a to setOf(b), b to setOf(a))) shouldBe
            SpaceHierarchyRoots(setOf(z, a, b), setOf(a, b))
    }

    @Test
    fun `all connected loops are flattened`() {
        spaceHierarchyRoots(mapOf(c to setOf(d), d to setOf(c, a), a to setOf(b), b to setOf(a))) shouldBe
            SpaceHierarchyRoots(setOf(a, b, c, d), setOf(a, b, c, d))
    }

    @Test
    fun `paths between loops are not themselves loops`() {
        spaceHierarchyRoots(
            mapOf(a to setOf(b), b to setOf(a, c), c to setOf(d), d to setOf(e), e to setOf(d))
        ) shouldBe SpaceHierarchyRoots(setOf(a, b, d, e), setOf(a, b, d, e))
    }

    @Test
    fun `independent loops are flattened`() {
        spaceHierarchyRoots(mapOf(a to setOf(b), b to setOf(a), c to setOf(d), d to setOf(c))) shouldBe
            SpaceHierarchyRoots(setOf(a, b, c, d), setOf(a, b, c, d))
    }

    @Test
    fun `deep hierarchies do not require recursion`() {
        val graph = (0 until 10_000).associate { room(it.toString()) to setOf(room((it + 1).toString())) }
        spaceHierarchyRoots(graph) shouldBe SpaceHierarchyRoots(setOf(room("0")), emptySet())
    }
}
