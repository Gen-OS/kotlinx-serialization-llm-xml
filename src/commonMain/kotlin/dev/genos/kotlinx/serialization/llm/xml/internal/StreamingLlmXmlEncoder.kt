package dev.genos.kotlinx.serialization.llm.xml.internal

import dev.genos.kotlinx.serialization.llm.xml.LlmField
import dev.genos.kotlinx.serialization.llm.xml.LlmListField
import dev.genos.kotlinx.serialization.llm.xml.LlmXml
import dev.genos.kotlinx.serialization.llm.xml.LlmXml.Default.encodeToString
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlEncoder
import dev.genos.kotlinx.serialization.llm.xml.LlmXmlEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class StreamingLlmXmlEncoder(
  override val llmXml: LlmXml,
  private val composer: PrettyPrintLlmXmlComposer,
) : LlmXmlEncoder {
  override val serializersModule: SerializersModule = llmXml.serializersModule

  private var startTagClosed = false

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
    startTagClosed = false
    composer.append("<")
    composer.append(descriptor.serialName.substringAfterLast('.')).append(">")
    composer.indent()
    return StreamingLlmXmlEncoder(llmXml, composer)
  }

  override fun endStructure(descriptor: SerialDescriptor) {
    startTagClosed = true
    composer.unindent().appendLine()
    composer.append("</").append(descriptor.serialName.substringAfterLast('.')).append(">")
  }

  override fun encodeNull(): Unit = throw SerializationException("'null' is not supported by default")

  private fun encodeValue(value: Any) {
    val quoteChar = if (startTagClosed) "" else "\""
    composer.append(quoteChar).append(value.toString()).append(quoteChar)
  }

  private fun encodeElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Any,
  ) {
    val llmField =
      descriptor.getElementAnnotations(index).filterIsInstance<LlmField>().firstOrNull()
    if (llmField != null && llmField.promptDescription.isNotEmpty()) {
      val tagName = llmField.tagName.ifEmpty { descriptor.getElementName(index) }
      composer.appendLine()
      composer.append("<").append(tagName).append(">")
      val promptValue = llmField.promptDescription.ifEmpty { value.toString() }
      composer.append("{... ").append(promptValue).append(" ...}")
      composer.append("</").append(tagName).append(">")
    }
  }

  override fun <T> encodeSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    serializer: SerializationStrategy<T>,
    value: T,
  ) {
    val llmListField = descriptor.getElementAnnotations(index).filterIsInstance<LlmListField>().firstOrNull()

    if (llmListField != null) {
      val tagName = llmListField.tagName.ifEmpty { descriptor.getElementName(index) }
      val listPromptDescription = llmListField.listPromptDescription.ifEmpty { null }

      val serializerDescriptor = serializer.descriptor
      check(serializerDescriptor.kind == StructureKind.LIST) { "LlmListField can only annotate a list!" }

      val element =
        serializerDescriptor
          .getElementDescriptor(0)

      val elementKind = element.kind
      val elementPromptDescription = llmListField.elementPromptDescription.ifEmpty { "$elementKind" }

      val elementName =
        element
          .serialName
          .split(".")
          .last()

      val elementTagName = llmListField.elementTagName.ifEmpty { elementName }

      println("list tagName: $tagName")
      println("elementTagName: $elementTagName")
      println("elementKind: $elementKind")

      composer.appendLine()
      composer.append("<$tagName>").indent().appendLine()
      if (listPromptDescription != null) {
        composer.append("{... $listPromptDescription ...}").appendLine()
      }

      when (elementKind) {
        is PrimitiveKind -> {
          composer.append("<$elementTagName>")
          composer.append("{... $elementPromptDescription ...}")
          composer.append("</$elementTagName>").appendLine()
        }
        is StructureKind -> {
          // Serialize the list to a string
          var serializedContent = encodeToString(serializer, value)
          serializedContent = serializedContent.replace(Regex("^<ArrayList>\\s*|\\s*</ArrayList>$"), "")

          val contentLines = serializedContent.lines().map { it.trim() }
          val elementOpeningTag = contentLines.first()
          val elementClosingTag = contentLines.last()
          var internalLines = contentLines.drop(1).dropLast(1)

          val repeatedOpeningTagIndex =
            contentLines.indexOfFirst { it.contains(elementOpeningTag) && contentLines.indexOf(it) > 0 }
          if (repeatedOpeningTagIndex >= 0) {
            // slice the contentLines to remove keep only the first entry
            internalLines = internalLines.slice(0 until repeatedOpeningTagIndex - 1)
          }

          // Write the opening tag and indent
          composer.append(elementOpeningTag).indent().appendLine()

          // Write the inner content
          if (internalLines.isNotEmpty()) {
            internalLines.dropLast(1).forEach { line ->
              composer.append(line).appendLine()
            }
            internalLines.lastOrNull()?.let { lastLine ->
              composer.append(lastLine).unindent().appendLine()
            }
          } else {
            composer.unindent().appendLine()
          }
          composer.append(elementClosingTag).appendLine()
        }
        else -> {
          throw SerializationException("Unsupported element kind: $elementKind")
        }
      }

      composer.append("{... other \"$elementTagName\" entries ...}").unindent().appendLine()
      composer.append("</${llmListField.tagName}>")
    } else {
      encodeSerializableValue(serializer, value)
    }
  }

  override fun encodeBoolean(value: Boolean) = encodeValue(value)

  override fun encodeByte(value: Byte) = encodeValue(value)

  override fun encodeShort(value: Short) = encodeValue(value)

  override fun encodeInt(value: Int) = encodeValue(value)

  override fun encodeLong(value: Long) = encodeValue(value)

  override fun encodeFloat(value: Float) = encodeValue(value)

  override fun encodeDouble(value: Double) = encodeValue(value)

  override fun encodeChar(value: Char) = encodeValue(value)

  override fun encodeString(value: String) = encodeValue(value)

  override fun encodeEnum(
    enumDescriptor: SerialDescriptor,
    index: Int,
  ): Unit = encodeValue(index)

  override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

  // Delegating implementation of CompositeEncoder
  override fun encodeBooleanElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Boolean,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeByteElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Byte,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeShortElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Short,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeIntElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Int,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeLongElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Long,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeFloatElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Float,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeDoubleElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Double,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeCharElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: Char,
  ): Unit = encodeElement(descriptor, index, value)

  override fun encodeStringElement(
    descriptor: SerialDescriptor,
    index: Int,
    value: String,
  ): Unit = encodeElement(descriptor, index, value)

  @ExperimentalSerializationApi
  override fun <T : Any> encodeNullableSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    serializer: SerializationStrategy<T>,
    value: T?,
  ) {
    TODO("Not yet implemented")
  }

  @ExperimentalSerializationApi
  override fun encodeInlineElement(
    descriptor: SerialDescriptor,
    index: Int,
  ): Encoder {
    TODO("Not yet implemented")
  }

  override fun encodeLlmXmlEntity(entity: LlmXmlEntity) {
    encodeSerializableValue(LlmXmlEntitySerializer, entity)
  }
}
