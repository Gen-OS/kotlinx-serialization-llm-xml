package dev.genos.kotlinx.serialization.llm.xml.internal

import dev.genos.kotlinx.serialization.llm.xml.LlmXmlDecoder
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlEncoder
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlEntity
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlEntity.Attribute
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlEntity.Value
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@PublishedApi
internal object LlmXmlEntitySerializer : KSerializer<LlmXmlEntity> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("com.ryanharter.kotlinx.serialization.xml.XmlEntity") {
      element("XmlValue", LlmXmlValueSerializer.descriptor)
      element("XmlAttribute", LlmXmlAttributeSerializer.descriptor)
    }

  override fun serialize(
    encoder: Encoder,
    value: LlmXmlEntity,
  ) {
    verify(encoder)
    when (value) {
      is Attribute -> encoder.encodeSerializableValue(LlmXmlAttributeSerializer, value)
      is Value -> encoder.encodeSerializableValue(LlmXmlValueSerializer, value)
      else -> {} // no op
    }
  }

  override fun deserialize(decoder: Decoder): LlmXmlEntity {
    val input = decoder.asLlmXmlDecoder()
    return input.decodeLlmXmlEntity()
  }
}

@PublishedApi
internal object LlmXmlAttributeSerializer : KSerializer<Attribute> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(
      "dev.genos.kotlinx.serialization.xml.LlmXmlEntity.LlmXmlAttribute",
      PrimitiveKind.STRING,
    )

  override fun serialize(
    encoder: Encoder,
    value: Attribute,
  ) {
    verify(encoder)
    PairSerializer(String.serializer(), String.serializer()).serialize(
      encoder,
      value.name to value.value,
    )
  }

  override fun deserialize(decoder: Decoder): Attribute {
    val result = decoder.asLlmXmlDecoder().decodeLlmXmlEntity()
    if (result !is Attribute) throw IllegalArgumentException("Unexpected XML entity, expected XmlAttribute, got ${result::class}")
    return result
  }
}

private object LlmXmlValueSerializer : KSerializer<Value> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(
      "com.ryanharter.kotlinx.serialization.xml.XmlValue",
      PrimitiveKind.STRING,
    )

  override fun serialize(
    encoder: Encoder,
    value: Value,
  ) {
    verify(encoder)
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): Value {
    val result = decoder.asLlmXmlDecoder().decodeLlmXmlEntity()
    check(result is Value) { "Unexpected LlmXml entity, expected LlmXmlValue, got ${result::class}" }
    return result
  }
}

private fun verify(decoder: Decoder) {
  decoder.asLlmXmlDecoder()
}

private fun verify(encoder: Encoder) {
  encoder.asLlmXmlEncoder()
}

internal fun Decoder.asLlmXmlDecoder() =
  this as? LlmXmlDecoder
    ?: throw IllegalStateException(
      "This serializer can be used only with LlmXml format. " +
        "Expected Decoder to be LlmXmlDecoder, got ${this::class}",
    )

internal fun Encoder.asLlmXmlEncoder() =
  this as? LlmXmlEncoder
    ?: throw IllegalStateException(
      "This serializer can be used only with LlmXml format. " +
        "Expected Encoder to be LlmXmlEncoder, got ${this::class}",
    )
