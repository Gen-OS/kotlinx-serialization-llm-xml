@file:OptIn(ExperimentalSerializationApi::class)

package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AiOutputResult<T> {
  abstract val timestamp: Long
}

class LlmXmlEncoderTest {
  private val default = LlmXml.Default

  @Test
  fun coachEncoding() {
    val xml = default.encodeToString(CoachResultTestData.promptObject)
    val expected = CoachResultTestData.prompt.trimIndent()
    assertEquals(expected, xml)
  }

  @Test
  fun greetingEncoding() {
    val xml = default.encodeToString(GreetingTestData.promptObject)
    val expected = GreetingTestData.prompt
    assertEquals(expected, xml)
  }

  @Test
  fun emailResultsEncoding() {
    val xml = default.encodeToString(EmailResultsTestData.promptObject)
    val expected = EmailResultsTestData.prompt
    assertEquals(expected, xml)
  }

  @Test
  fun recipeEncoding() {
    val xml = default.encodeToString(RecipeTestData.promptObject)
    val expected = RecipeTestData.prompt.trimIndent()
    assertEquals(expected, xml)
  }
}
