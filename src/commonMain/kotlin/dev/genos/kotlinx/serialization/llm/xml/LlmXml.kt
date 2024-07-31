package dev.genos.kotlinx.serialization.llm.xml

import dev.genos.kotlinx.serialization.llm.xml.internal.PrettyPrintLlmXmlComposer
import dev.genos.kotlinx.serialization.llm.xml.internal.StreamingLlmXmlDecoder
import dev.genos.kotlinx.serialization.llm.xml.internal.StreamingLlmXmlEncoder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
public sealed class LlmXml(
  override val serializersModule: SerializersModule,
) : StringFormat {
  public companion object Default : LlmXml(EmptySerializersModule())

  override fun <T> encodeToString(
    serializer: SerializationStrategy<T>,
    value: T,
  ): String {
    val sb = StringBuilder()
    val composer = PrettyPrintLlmXmlComposer(sb, 2)
    val encoder = StreamingLlmXmlEncoder(this, composer)
    serializer.serialize(encoder, value)
    return sb.toString()
  }

  override fun <T> decodeFromString(
    deserializer: DeserializationStrategy<T>,
    string: String,
  ): T {
    val lexer = LlmXmlLexer(string)
    val input = StreamingLlmXmlDecoder(this, lexer)
    return input.decodeSerializableValue(deserializer)
  }
}
