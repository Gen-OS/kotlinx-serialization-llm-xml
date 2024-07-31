package dev.genos.kotlinx.serialization.llm.xml

public sealed interface LlmXmlEntity {
  public sealed interface ContentEntity : LlmXmlEntity

  public data class Document(
    public val root: Element,
  ) : LlmXmlEntity

  public data class Value(
    public val value: String,
  ) : ContentEntity

  public data class Element(
    public val name: String,
    public val namespace: String?,
    public val attributes: List<Attribute> = emptyList(),
    public val content: List<ContentEntity> = emptyList(),
  ) : ContentEntity

  public data class Attribute(
    public val name: String,
    public val value: String,
    public val prefix: String? = null,
  ) : LlmXmlEntity

  public data class Comment(
    val value: String,
  ) : ContentEntity
}
