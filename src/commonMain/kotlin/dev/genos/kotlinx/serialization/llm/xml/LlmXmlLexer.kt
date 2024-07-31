package dev.genos.kotlinx.serialization.llm.xml

public class LlmXmlLexer(
  private val source: String,
) {
  public var position: Int = 0

  private val pushedBackTokens: MutableList<Token> = mutableListOf()

  private var lastToken: Token = Token.None

  public fun copy(): LlmXmlLexer {
    val other = LlmXmlLexer(source)
    other.position = position
    other.lastToken = lastToken
    return other
  }

  public fun next(): Char? = if (position < source.length) source[position++] else null

  public fun peek(): Char? = if (position < source.length) source[position] else null

  public fun skipToChar(char: Char) {
    var c = next()
    while (c != null) {
      when (c) {
        char -> return
        else -> c = next()
      }
    }
  }

  public fun skipWhitespace() {
    var c = peek()
    while (c != null) {
      when (c) {
        ' ', '\n', '\t', '\r' -> {
          next()
          c = peek()
        }
        else -> return
      }
    }
  }

  public fun requireChar(char: Char) {
    skipWhitespace()
    require(peek() != null) { "Unexpected end of file" }
    require(peek() == char) { "Unexpected token ${peek()}, expecting $char" }
  }

  public data class QualifiedName(
    val name: String,
    val namespace: String? = null,
  )

  private fun readElementName(): QualifiedName {
    skipWhitespace()
    var start = position
    var namespace: String? = null
    while (true) {
      when (next()) {
        null -> throw IllegalArgumentException("Unexpected end of file")
        ':' -> {
          namespace = source.substring(start, position - 1)
          start = position
        }
        '\r', '\n', '\t', ' ', '>', '/' -> break
      }
    }
    val name = source.substring(start, --position)
    return QualifiedName(name, namespace)
  }

  private fun readAttributeName(): QualifiedName {
    skipWhitespace()
    var start = position
    var namespace: String? = null
    while (true) {
      when (peek()) {
        null -> throw IllegalArgumentException("Unexpected end of file")
        ':' -> {
          namespace = source.substring(start, position++)
          start = position
        }
        '\r', '\t', '\n', ' ', '=' -> break
        else -> position++
      }
    }
    val name = source.substring(start, position)
    return QualifiedName(name, namespace)
  }

  private fun readAttributeValue(): String {
    skipWhitespace()
    var quote = next()
    while (quote != null && quote != '\'' && quote != '"') {
      quote = next()
    }
    requireNotNull(quote) { "Unexpected end of file" }

    val s = position
    var c = next()
    while (true) {
      when (c) {
        null -> throw IllegalArgumentException("Unexpected end of file")
        '<', '&' -> throw IllegalArgumentException("Invalid character in attribute name: $c")
        quote -> break
        else -> c = next()
      }
    }
    return source.substring(s, position - 1)
  }

  public fun readText(): String {
    skipWhitespace()
    val text = StringBuilder()
    while (true) {
      when (val c = next()) {
        null -> throw IllegalArgumentException("Unexpected end of file")
        '<' -> {
          if (peek() == '!' && source.substring(position, position + 8) == "![CDATA[") {
            position += 8
            val end = source.indexOf("]]>", position)
            text.append(source.substring(position, end))
            position = end + 3
          } else {
            position--
            break
          }
        }
        else -> text.append(c)
      }
    }
    return text.toString().trim()
  }

  public fun pushBack(token: Token) {
    pushedBackTokens.add(token)
  }

  public fun readNextToken(): Token {
    if (pushedBackTokens.isNotEmpty()) {
      return pushedBackTokens.removeAt(pushedBackTokens.lastIndex)
    }

    when (lastToken) {
      is Token.DocumentEnd -> return lastToken
      is Token.None, Token.ElementStartEnd, is Token.ElementEnd, is Token.Text -> {
        skipWhitespace()
        while (true) {
          skipWhitespace()
          when (peek()) {
            null -> return Token.DocumentEnd.also { lastToken = it }
            '<' -> {
              next() // consume the bracket
              when (peek()) {
                '!', '?' -> {
                  skipToChar('>')
                }
                '/' -> {
                  next() // consume the slash
                  val elementName = readElementName()
                  skipWhitespace()
                  next() // Consume the closing bracket
                  return Token
                    .ElementEnd(elementName.name)
                    .also { lastToken = it }
                }
                else -> {
                  val elementName = readElementName()
                  return Token
                    .ElementStart(elementName.name)
                    .also { lastToken = it }
                }
              }
            }
            '/' -> {
              skipToChar('>')
              return Token.ElementEnd()
            }
            else -> {
              return Token.Text(readText()).also { lastToken = it }
            }
          }
        }
      }
      is Token.ElementStart, is Token.AttributeValue, is Token.AttributeEnd -> {
        while (true) {
          skipWhitespace()
          return when (peek()) {
            '/' -> {
              skipToChar('>')
              Token.ElementEnd()
            }
            '>' -> {
              next() // consume the bracket
              Token.ElementStartEnd
            }
            else -> {
              val qname = readAttributeName()
              Token.AttributeName(qname.name)
            }
          }.also { lastToken = it }
        }
      }
      is Token.AttributeName -> {
        skipWhitespace()
        return if (peek() == '=') {
          position++
          Token.AttributeValue(readAttributeValue()).also { lastToken = it }
        } else {
          Token.AttributeEnd.also { lastToken = it }
        }
      }
    }
  }

  public sealed interface Token {
    public object None : Token

    public data class ElementStart(
      val name: String,
    ) : Token

    public object ElementStartEnd : Token

    public data class ElementEnd(
      val name: String? = null,
    ) : Token

    public data class AttributeName(
      val name: String,
    ) : Token

    public data class AttributeValue(
      val value: String,
    ) : Token

    public object AttributeEnd : Token

    public data class Text(
      val content: String,
    ) : Token

    public object DocumentEnd : Token
  }
}
