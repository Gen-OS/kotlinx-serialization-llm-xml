package dev.genos.kotlinx.serialization.llm.xml.internal

public interface Composer {
  public fun indent(): Composer

  public fun unindent(): Composer

  public fun newElement(): Composer

  public fun newAttribute(): Composer

  public fun append(value: String): Composer

  public fun appendLine(): Composer
}

public class LlmXmlComposer(
  private val sb: StringBuilder,
) : Composer {
  override fun indent(): Composer = this

  override fun unindent(): Composer = this

  override fun newElement(): Composer = this

  override fun newAttribute(): Composer = also { sb.append(" ") }

  override fun append(value: String): Composer = also { sb.append(value) }

  override fun appendLine(): LlmXmlComposer = this
}

internal class PrettyPrintLlmXmlComposer(
  private val sb: StringBuilder,
  private val indent: Int = 2,
) : Composer {
  private var level = 0

  override fun indent(): Composer =
    also {
      level++
    }

  override fun unindent(): Composer =
    also {
      level--
    }

  override fun newElement(): Composer =
    also {
      appendLine()
    }

  override fun newAttribute(): Composer =
    also {
      appendLine().append(" ".repeat(indent))
    }

  override fun append(value: String): Composer = also { sb.append(value) }

  override fun appendLine(): Composer =
    also {
      sb.appendLine()
      sb.append(" ".repeat(level * indent))
    }
}
