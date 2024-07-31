package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.encoding.Decoder

public interface LlmXmlDecoder : Decoder {
  public val llmXml: LlmXml

  public fun decodeLlmXmlEntity(): LlmXmlEntity
}
