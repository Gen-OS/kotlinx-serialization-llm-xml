@file:OptIn(ExperimentalSerializationApi::class)

package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class LlmXmlDecoderTest {
  private val default = LlmXml.Default

  @Ignore
  @Test
  fun coachDecoding() {
    CoachResultTestData.examplesXml.forEachIndexed { index, xml ->
      val decoded = default.decodeFromString<CoachResult>(xml)
      val expected = CoachResultTestData.examples[index]

      assertEquals(expected.overview, decoded.overview)
      assertEquals(expected.positiveFeedback, decoded.positiveFeedback)
      assertEquals(expected.areasForImprovement, decoded.areasForImprovement)
      assertEquals(expected.actionItems, decoded.actionItems)
      assertEquals(expected, decoded)
    }
  }

  @Ignore
  @Test
  fun greetingDecoding() {
    GreetingTestData.examplesXml.forEachIndexed { index, xml ->
      val decoded = default.decodeFromString<Greeting>(xml)
      val expected = GreetingTestData.examples[index]
      assertEquals(expected, decoded)
    }
  }

  @Ignore
  @Test
  fun emailResultsDecoding() {
    EmailResultsTestData.examplesXml.forEachIndexed { index, xml ->
      val decoded = default.decodeFromString<EmailsResults>(xml)
      val expected = EmailResultsTestData.examples[index]
      assertEquals(expected, decoded)
    }
  }

  @Test
  fun recipeDecoding() {
    RecipeTestData.examplesXml.forEachIndexed { index, xml ->
      val decoded = default.decodeFromString<Recipe>(xml)
      val expected = RecipeTestData.examples[index]
      assertEquals(expected, decoded)
    }
  }
}
