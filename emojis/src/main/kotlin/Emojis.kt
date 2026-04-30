import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Emoji(
    val number: Int,
    val emoji: String,
    val description: String,
    @SerialName("translated_descriptions") val translatedDescriptions: Map<String, String?>,
)

/**
 * Helper to generate emojis with only German translation.
 */
fun main() {
    String.Companion::class.java.getResourceAsStream("/emojis.json")?.let { jsonStream ->
        val json = Json {
            ignoreUnknownKeys = true
        }.decodeFromString<List<Emoji>>(jsonStream.readBytes().toString(Charsets.UTF_8))
        val map =
            """mapOf(${
                json.joinToString(",") { emoji ->
                    """${emoji.number} to Pair("${emoji.emoji}", mapOf(${
                        """"en" to "${emoji.description}", """ +
                                emoji.translatedDescriptions.map { translation ->
                                    """"${translation.key}" to "${translation.value}""""
                                }.joinToString { it }
                    }))"""
                }
            })"""
    }
}
