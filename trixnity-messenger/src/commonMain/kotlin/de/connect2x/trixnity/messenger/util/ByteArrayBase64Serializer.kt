package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.utils.decodeBase64
import de.connect2x.trixnity.utils.encodeBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ByteArrayBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ByteArrayBase64Serializer")

    override fun deserialize(decoder: Decoder): ByteArray {
        return decoder.decodeString().decodeBase64()
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(value.encodeBase64())
    }
}
