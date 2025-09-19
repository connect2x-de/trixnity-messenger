package de.connect2x.trixnity.messenger.settings

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class JsonChildTest {
    private val simpleObject = JsonObject(mapOf("dino" to JsonPrimitive("unicorn")))

    @Test
    fun `getJsonChild - empty keys - source`() {
        getJsonChild(
            JsonObject(mapOf("a" to JsonPrimitive("value"))),
        ) shouldBe JsonObject(mapOf("a" to JsonPrimitive("value")))
    }

    @Test
    fun `getJsonChild - property - empty object`() {
        getJsonChild(
            JsonObject(mapOf("a" to JsonPrimitive("value"))),
            "a"
        ) shouldBe JsonObject(mapOf())
    }

    @Test
    fun `getJsonChild - key`() {
        getJsonChild(
            JsonObject(mapOf("a" to simpleObject)),
            "a"
        ) shouldBe simpleObject
    }

    @Test
    fun `getJsonChild - path`() {
        getJsonChild(
            JsonObject(mapOf("a" to JsonObject(mapOf("b" to simpleObject)))),
            "a", "b"
        ) shouldBe simpleObject
    }

    @Test
    fun `getJsonChild - property not found - empty object`() {
        getJsonChild(
            JsonObject(mapOf("a" to JsonObject(mapOf("b.c" to simpleObject)))),
            "a", "b", "c"
        ) shouldBe JsonObject(mapOf())
    }

    @Test
    fun `getJsonChild - allow dot in path`() {
        getJsonChild(
            JsonObject(mapOf("a" to JsonObject(mapOf("b.c" to simpleObject)))),
            "a", "b.c"
        ) shouldBe simpleObject
    }

    @Test
    fun `putJsonChild - empty keys - override into root`() {
        putJsonChild(
            JsonObject(
                mapOf(
                    "dino" to JsonObject(mapOf("a" to JsonPrimitive("unicorn"))),
                    "c" to JsonPrimitive("value")
                )
            ),
            JsonObject(mapOf("dino" to JsonObject(mapOf("b" to JsonPrimitive("unicorn"))))),
        ) shouldBe JsonObject(
            mapOf(
                "dino" to JsonObject(
                    mapOf(
                        "b" to JsonPrimitive("unicorn")
                    )
                ),
                "c" to JsonPrimitive("value"),
            )
        )
    }

    @Test
    fun `putJsonChild - key`() {
        putJsonChild(
            JsonObject(mapOf()),
            simpleObject,
            "a",
        ) shouldBe JsonObject(mapOf("a" to simpleObject))
    }

    @Test
    fun `putJsonChild - path`() {
        putJsonChild(
            JsonObject(mapOf()),
            simpleObject,
            "a", "b",
        ) shouldBe JsonObject(mapOf("a" to JsonObject(mapOf("b" to simpleObject))))
    }

    @Test
    fun `putJsonChild - keep siblings`() {
        putJsonChild(
            JsonObject(
                mapOf(
                    "a" to JsonObject(
                        mapOf(
                            "b" to JsonObject(
                                mapOf(
                                    "c" to JsonPrimitive("value")
                                )
                            ),
                            "d" to simpleObject,
                        )
                    ),
                    "e" to simpleObject,
                )
            ),
            simpleObject,
            "a", "b",
        ) shouldBe JsonObject(
            mapOf(
                "a" to JsonObject(
                    mapOf(
                        "b" to simpleObject,
                        "d" to simpleObject,
                    )
                ),
                "e" to simpleObject,
            )
        )
    }

    @Test
    fun `putJsonChild - property - override`() {
        putJsonChild(
            JsonObject(mapOf("a" to JsonPrimitive("value"))),
            simpleObject,
            "a", "b",
        ) shouldBe JsonObject(mapOf("a" to JsonObject(mapOf("b" to simpleObject))))
    }

    @Test
    fun `putJsonChild - allow dot in path`() {
        putJsonChild(
            JsonObject(mapOf()),
            simpleObject,
            "a", "b.c",
        ) shouldBe JsonObject(mapOf("a" to JsonObject(mapOf("b.c" to simpleObject))))
    }
}
