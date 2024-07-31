package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder

public interface LlmXmlEncoder :
  Encoder,
  CompositeEncoder {
  public val llmXml: LlmXml

  public fun encodeLlmXmlEntity(entity: LlmXmlEntity)
}
