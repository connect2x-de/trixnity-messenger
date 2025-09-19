package de.connect2x.trixnity.messenger.settings

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class JsonMergeTest {

    @Test
    fun `disjoint keys`() {
        val source = JsonObject(mapOf("a" to JsonPrimitive(1)))
        val update = JsonObject(mapOf("b" to JsonPrimitive(2)))

        jsonMerge(source, update) shouldBe JsonObject(
            mapOf(
                "a" to JsonPrimitive(1),
                "b" to JsonPrimitive(2),
            )
        )
    }

    @Test
    fun `override primitive`() {
        val source = JsonObject(mapOf("a" to JsonPrimitive(1)))
        val update = JsonObject(mapOf("a" to JsonPrimitive(42)))

        jsonMerge(source, update) shouldBe JsonObject(
            mapOf("a" to JsonPrimitive(42))
        )
    }

    @Test
    fun `nested objects`() {
        val source = JsonObject(
            mapOf(
                "a" to JsonObject(
                    mapOf(
                        "b" to JsonPrimitive(1),
                        "c" to JsonPrimitive(2),
                    )
                )
            )
        )
        val update = JsonObject(
            mapOf(
                "a" to JsonObject(
                    mapOf(
                        "b" to JsonPrimitive(10),
                        "d" to JsonPrimitive(20),
                    )
                )
            )
        )

        jsonMerge(source, update) shouldBe JsonObject(
            mapOf(
                "a" to JsonObject(
                    mapOf(
                        "b" to JsonPrimitive(10),
                        "c" to JsonPrimitive(2),
                        "d" to JsonPrimitive(20),
                    )
                )
            )
        )
    }

    @Test
    fun `nested object replaced by primitive`() {
        val source = JsonObject(
            mapOf("a" to JsonObject(mapOf("b" to JsonPrimitive(1))))
        )
        val update = JsonObject(
            mapOf("a" to JsonPrimitive(100))
        )

        jsonMerge(source, update) shouldBe JsonObject(
            mapOf("a" to JsonPrimitive(100))
        )
    }

    @Test
    fun `primitive replaced by nested object`() {
        val source = JsonObject(mapOf("a" to JsonPrimitive(1)))
        val update = JsonObject(
            mapOf("a" to JsonObject(mapOf("b" to JsonPrimitive(2))))
        )

        jsonMerge(source, update) shouldBe JsonObject(
            mapOf("a" to JsonObject(mapOf("b" to JsonPrimitive(2))))
        )
    }

    @Test
    fun `multiple nested levels`() {
        val source = JsonObject(
            mapOf(
                "x" to JsonObject(
                    mapOf(
                        "y" to JsonObject(
                            mapOf(
                                "z" to JsonPrimitive("deep")
                            )
                        )
                    )
                )
            )
        )
        val update = JsonObject(
            mapOf(
                "x" to JsonObject(
                    mapOf(
                        "y" to JsonObject(
                            mapOf(
                                "w" to JsonPrimitive("added")
                            )
                        )
                    )
                )
            )
        )

        jsonMerge(source, update) shouldBe JsonObject(
            mapOf(
                "x" to JsonObject(
                    mapOf(
                        "y" to JsonObject(
                            mapOf(
                                "z" to JsonPrimitive("deep"),
                                "w" to JsonPrimitive("added"),
                            )
                        )
                    )
                )
            )
        )
    }
}
