package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.SerializationException

public open class LlmXmlSerializationException(
  message: String? = null,
  cause: Throwable? = null,
) : SerializationException(message, cause)
