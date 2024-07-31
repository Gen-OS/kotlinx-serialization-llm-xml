package dev.genos.kotlinx.serialization.llm.xml.internal

import dev.genos.kotlinx.serialization.llm.xml.LlmField
import dev.genos.kotlinx.serialization.llm.xml.LlmListField
import dev.genos.kotlinx.serialization.llm.xml.LlmXml
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlDecoder
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlEntity
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlLexer
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlLexer.Token
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class LlmXmlElementDecoder(
  private val decoder: StreamingLlmXmlDecoder,
  private val lexer: LlmXmlLexer,
  private val descriptor: SerialDescriptor,
) : CompositeDecoder {
  companion object {
    private val logger = LoggingFacade
  }

  override val serializersModule: SerializersModule = decoder.serializersModule

  private val elementStack = mutableListOf<String>()

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    while (true) {
      val token = lexer.readNextToken()
      logger.debug { "decodeElementIndex token $token" }
      when (token) {
        is LlmXmlLexer.Token.ElementStart -> {
          val index = descriptor.getElementIndex(token.name)
          if (index != UNKNOWN_NAME) {
            elementStack.add(token.name)
            return index
          }
        }

        is LlmXmlLexer.Token.ElementEnd -> {
          if (elementStack.isNotEmpty() && elementStack.last() == token.name) {
            elementStack.removeAt(elementStack.lastIndex)
          }
          if (elementStack.isEmpty()) {
            return DECODE_DONE
          }
        }

        is LlmXmlLexer.Token.DocumentEnd -> return DECODE_DONE
        else -> {} // Skip other tokens
      }
    }
  }

  override fun decodeSequentially(): Boolean = true

  override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
    var size = 0
    var depth = 1
    while (depth > 0) {
      val token = lexer.readNextToken()
      logger.debug { "decodeCollectionSize: $token" }
      when (token) {
        is LlmXmlLexer.Token.ElementStart -> {
          if (depth == 1) size++
          depth++
        }

        is LlmXmlLexer.Token.ElementEnd -> depth--
        else -> {} // Skip other tokens
      }
    }
    lexer.position = 0 // Reset lexer position
    return size
  }

  private fun decodeElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): String? {
    val llmField =
      descriptor.getElementAnnotations(index).filterIsInstance<LlmField>().firstOrNull()

    val content = StringBuilder()
    var depth = 0

    while (llmField != null) {
      var elementName =
        llmField.tagName.takeUnless { it.isEmpty() }
          ?: descriptor.getElementName(index)

      val token = lexer.readNextToken()

      when (token) {
        is LlmXmlLexer.Token.ElementStart -> {
          logger.debug { "enter decodeElement.ElementStart: $token, depth: $depth, target: $elementName" }
          if (token.name == elementName) depth++
        }

        is LlmXmlLexer.Token.Text -> {
          logger.debug { "decodeElement.Text: $token, depth: $depth" }
          if (depth == 1) {
            logger.debug { "capturing text: ${token.content}" }
            content.append(token.content)
          }
        }

        is LlmXmlLexer.Token.ElementEnd -> {
          logger.debug { "enter decodeElement.ElementEnd: $token, depth: $depth, target: $elementName" }
          if (token.name == elementName) {
            depth--
            if (depth == 0) break
          }
        }

        is LlmXmlLexer.Token.DocumentEnd -> {
          logger.debug { "decodeElement.DocumentEnd: $token" }
          break
        }

        is LlmXmlLexer.Token.ElementStartEnd -> {}
        else -> {
          logger.debug { "decodeElement.skipping: $token" }
        } // Skip other tokens
      }
    }
    val contentString = content.toString().trim().ifEmpty { null }
    logger.debug { "decodeElement.contentString: $contentString" }
    return contentString
  }

  override fun decodeStringElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): String {
    logger.debug { "in decodeStringElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index) ?: ""
  }

  override fun decodeBooleanElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Boolean {
    logger.debug { "in decodeBooleanElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.toBoolean() ?: false
  }

  override fun decodeByteElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Byte {
    logger.debug { "in decodeByteElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.toByteOrNull() ?: 0
  }

  override fun decodeShortElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Short {
    logger.debug { "in decodeShortElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.toShortOrNull() ?: 0
  }

  override fun decodeIntElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Int {
    logger.debug { "in decodeIntElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.toIntOrNull() ?: 0
  }

  override fun decodeLongElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Long {
    logger.debug { "in decodeLongElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.toLongOrNull() ?: 0L
  }

  override fun decodeFloatElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Float {
    logger.debug { "in decodeFloatElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.toFloatOrNull() ?: 0f
  }

  override fun decodeDoubleElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Double {
    logger.debug { "in decodeDoubleElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.toDoubleOrNull() ?: 0.0
  }

  override fun decodeCharElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Char {
    logger.debug { "in decodeCharElement: ${descriptor.serialName} $index" }
    return decodeElement(descriptor, index)?.firstOrNull() ?: Char(0)
  }

  override fun <T> decodeSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    deserializer: DeserializationStrategy<T>,
    previousValue: T?,
  ): T {
    when {
      descriptor.getElementAnnotations(index).any { it is LlmListField } -> {
        val llmListField =
          descriptor.getElementAnnotations(index).filterIsInstance<LlmListField>().firstOrNull()

        val elementName =
          llmListField?.tagName?.takeUnless { it.isEmpty() }
            ?: descriptor.serialName
              .split(".")
              .last()

        logger.debug { "******* decodeSerializableElement elementName: $elementName" }

        val startToken = lexer.readNextToken()
        val skippedStartStopToken = lexer.readNextToken()
        val possibleEndToken = lexer.readNextToken()

        logger.debug { "startToken: $startToken" }
        logger.debug { "possibleEndToken: $possibleEndToken" }

        if (startToken is LlmXmlLexer.Token.ElementStart) {
          if (
            skippedStartStopToken !is LlmXmlLexer.Token.DocumentEnd &&
            possibleEndToken !is LlmXmlLexer.Token.DocumentEnd &&
            possibleEndToken !is LlmXmlLexer.Token.ElementEnd
          ) {
            lexer.pushBack(possibleEndToken)
            lexer.pushBack(skippedStartStopToken)
            lexer.pushBack(startToken)
            val listDecoder = ListLlmXmlDecoder(decoder, lexer, elementName)
            return deserializer.deserialize(listDecoder)
          }
        }

        logger.debug { "returning empty list!" }
        @Suppress("UNCHECKED_CAST")
        return emptyList<Any>() as T
      }
      else -> {
        logger.debug { "decoding ${descriptor.getElementName(index)}" }
        return deserializer.deserialize(decoder)
      }
    }
  }

  override fun <T : Any> decodeNullableSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    deserializer: DeserializationStrategy<T?>,
    previousValue: T?,
  ): T? = deserializer.deserialize(decoder)

  override fun endStructure(descriptor: SerialDescriptor) {
    // No-op for LLM XML
  }

  override fun decodeInlineElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Decoder = decoder
}

@OptIn(ExperimentalSerializationApi::class)
internal class ListLlmXmlDecoder(
  private val parentDecoder: StreamingLlmXmlDecoder,
  private val lexer: LlmXmlLexer,
  private val listElementName: String,
) : Decoder by parentDecoder {
  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
    ListLlmXmlElementDecoder(parentDecoder, lexer, listElementName)
}

@OptIn(ExperimentalSerializationApi::class)
internal class ListLlmXmlElementDecoder(
  private val parentDecoder: StreamingLlmXmlDecoder,
  private val lexer: LlmXmlLexer,
  private val listElementName: String,
) : CompositeDecoder {
  companion object {
    private val logger = LoggingFacade
  }

  override val serializersModule: SerializersModule
    get() = parentDecoder.serializersModule

  private var index = 0
  private var currentElementContent = StringBuilder()

  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    currentElementContent.clear()
    while (true) {
      when (val token = lexer.readNextToken()) {
        is LlmXmlLexer.Token.ElementStart -> {
          logger.debug { "decodeElementIndex.ElementStart: $token, index: $index, targetName: $listElementName" }
          if (token.name == listElementName) {
            // Check if the next token is an immediate end tag
            val nextToken = lexer.readNextToken()
            if (nextToken is LlmXmlLexer.Token.ElementEnd && nextToken.name == listElementName) {
              // This is an empty element, return DECODE_DONE
              return CompositeDecoder.DECODE_DONE
            } else {
              logger.debug { "decodeElementIndex.ElementStart pushed $token, index: $index" }
              // Put back the token we just read
              lexer.pushBack(nextToken)
              return index++
            }
          }
        }
        is LlmXmlLexer.Token.ElementEnd -> {
          logger.debug { "decodeElementIndex.ElementEnd: $token, index: $index, targetName: $listElementName" }
          if (token.name == listElementName) {
            logger.debug { "returning DECODE_DONE = ${CompositeDecoder.DECODE_DONE}" }
            return CompositeDecoder.DECODE_DONE
          }
        }
        is LlmXmlLexer.Token.ElementStartEnd -> {
          logger.debug { "decodeElementIndex.ElementStartEnd: $token, index: $index" }
          return index++
        }
        is LlmXmlLexer.Token.Text -> {
          logger.debug { "decodeElementIndex.Text: $token, index: $index" }
          currentElementContent.append(token.content)
        }
        is LlmXmlLexer.Token.DocumentEnd -> {
          logger.debug { "decodeElementIndex.DocumentEnd: $token, index: $index" }
          if (currentElementContent.isEmpty()) {
            logger.debug { "no content available!" }
          }
          return CompositeDecoder.DECODE_DONE
        }
        else -> {
          logger.debug { "decodeElementIndex.skipped: $token" }
        }
      }
    }
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    var depth = 1
    while (depth > 0) {
      val token = lexer.readNextToken()

      when (token) {
        is LlmXmlLexer.Token.ElementStart -> {
          if (token.name == listElementName) {
            depth++
          } else {
            logger.debug { "endStructure pushed $token" }
            // Put back the token we just read
            lexer.pushBack(token)
            return
          }
        }
        is LlmXmlLexer.Token.ElementEnd -> depth--
        is LlmXmlLexer.Token.DocumentEnd -> {
          depth = 0 // Force exit if we unexpectedly reach the end of the document
          logger.warn { "Unexpected end of document while closing structure" }
        }
        else -> {} // Ignore other tokens
      }
      logger.debug { "endStructure: $token, depth: $depth" }
    }
  }

  override fun decodeBooleanElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Boolean = currentElementContent.toString().trim().toBoolean()

  override fun decodeByteElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Byte = currentElementContent.toString().trim().toByte()

  override fun decodeIntElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Int = currentElementContent.toString().trim().toInt()

  override fun decodeLongElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Long = currentElementContent.toString().trim().toLong()

  override fun decodeFloatElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Float = currentElementContent.toString().trim().toFloat()

  override fun decodeDoubleElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Double = currentElementContent.toString().trim().toDouble()

  override fun decodeStringElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): String = currentElementContent.toString().trim()

  override fun <T> decodeSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    deserializer: DeserializationStrategy<T>,
    previousValue: T?,
  ): T {
    logger.debug { "%% decodeSerializableElement" }
    val result = deserializer.deserialize(parentDecoder)
    logger.debug { "%% result: $result" }
    return result
  }

  @ExperimentalSerializationApi
  override fun <T : Any> decodeNullableSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    deserializer: DeserializationStrategy<T?>,
    previousValue: T?,
  ): T? = deserializer.deserialize(parentDecoder)

  // Implement other methods as needed, or leave them as TODO if not required for your use case
  override fun decodeCharElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Char = TODO()

  override fun decodeShortElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Short = TODO()

  override fun decodeInlineElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Decoder = TODO()
}

@OptIn(ExperimentalSerializationApi::class)
internal class StreamingLlmXmlDecoder(
  override val llmXml: LlmXml,
  private val lexer: LlmXmlLexer,
) : LlmXmlDecoder {
  companion object {
    private val logger = LoggingFacade
  }

  override val serializersModule: SerializersModule = llmXml.serializersModule

  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = LlmXmlElementDecoder(this, lexer, descriptor)

  private fun readContent(): String {
    var token = lexer.readNextToken()
    logger.debug { "readContent token $token" }
    return when (token) {
      is LlmXmlLexer.Token.Text -> token.content
      is LlmXmlLexer.Token.AttributeValue -> token.value
      is LlmXmlLexer.Token.ElementStart, is LlmXmlLexer.Token.ElementStartEnd -> {
        // Read until we find the end of this element
        val content = StringBuilder()
        while (true) {
          token = lexer.readNextToken()
          logger.debug { "readContent token $token" }
          when (token) {
            is LlmXmlLexer.Token.Text -> content.append(token.content)
            is LlmXmlLexer.Token.ElementEnd -> break
            else -> {} // Ignore other tokens
          }
        }
        logger.debug { "readContent: $content" }
        content.toString()
      }
      else -> throw IllegalStateException("Unexpected token when reading content: $token")
    }
  }

  override fun decodeBoolean(): Boolean = readContent().toBoolean()

  override fun decodeByte(): Byte = readContent().toByte()

  override fun decodeShort(): Short = readContent().toShort()

  override fun decodeInt(): Int = readContent().toInt()

  override fun decodeLong(): Long = readContent().toLong()

  override fun decodeFloat(): Float = readContent().toFloat()

  override fun decodeDouble(): Double = readContent().toDouble()

  override fun decodeChar(): Char = readContent().single()

  override fun decodeString(): String = readContent()

  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = enumDescriptor.getElementIndex(decodeString())

  override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

  override fun decodeNotNullMark(): Boolean = true

  override fun decodeNull(): Nothing? = null

  override fun decodeLlmXmlEntity(): LlmXmlEntity {
    TODO("Implement if needed for your use case")
  }
}
