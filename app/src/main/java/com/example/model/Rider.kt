package com.example.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try { decoder.decodeString() } catch (_: Exception) { null }
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        if (element is JsonPrimitive) {
            return element.content
        }
        return element.toString()
    }
}

object FlexibleDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeDouble(value)
        }
    }

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try { decoder.decodeDouble() } catch (_: Exception) { null }
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        if (element is JsonPrimitive) {
            return element.doubleOrNull ?: element.content.toDoubleOrNull()
        }
        return null
    }
}

@Serializable
data class Rider(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String? = null,

    @Serializable(with = FlexibleStringSerializer::class)
    val phone: String? = "",

    @Serializable(with = FlexibleStringSerializer::class)
    val password: String? = "",

    val name: String? = null,

    @Serializable(with = FlexibleDoubleSerializer::class)
    val current_latitude: Double? = null,

    @Serializable(with = FlexibleDoubleSerializer::class)
    val current_longitude: Double? = null
)
