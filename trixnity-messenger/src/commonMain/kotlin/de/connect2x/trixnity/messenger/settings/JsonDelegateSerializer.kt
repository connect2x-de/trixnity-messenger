package de.connect2x.trixnity.messenger.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

open class JsonDelegateSerializer<T : Map<String, JsonElement>>(
    name: String,
    private val factory: (Map<String, JsonElement>) -> T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name)

    override fun deserialize(decoder: Decoder): T {
        require(decoder is JsonDecoder)
        val delegate =
            (decoder.decodeJsonElement() as? JsonObject) ?: throw SerializationException("header was no Json Object")
        return factory(delegate)
    }

    override fun serialize(encoder: Encoder, value: T) {
        require(encoder is JsonEncoder)
        encoder.encodeJsonElement(JsonObject(value))
    }
}
