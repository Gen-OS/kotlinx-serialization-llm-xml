package dev.genos.kotlinx.serialization.llm.xml.demo

import dev.genos.kotlinx.serialization.llm.xml.LlmXml
import io.github.cdimascio.dotenv.dotenv
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("ClaudeDemo")

public fun main(args: Array<String>) {
  val parser = ArgParser("claude-demo")
  val apiKey by parser.option(ArgType.String, shortName = "k", description = "Anthropic API Key").default("")
  parser.parse(args)

  // Load API key from .env file or command line argument
  val dotenv =
    dotenv {
      directory = "."
      filename = ".env"
      ignoreIfMalformed = true
      ignoreIfMissing = true
    }

  val finalApiKey =
    when {
      apiKey.isNotEmpty() -> apiKey
      dotenv["ANTHROPIC_API_KEY"] != null -> dotenv["ANTHROPIC_API_KEY"]!!
      else -> {
        logger.error("Anthropic API key not provided. Please use -k option or set ANTHROPIC_API_KEY in .env file.")
        return
      }
    }

  val recipe =
    Recipe(
      name = "",
      description = "",
      ingredients = listOf(Ingredient(name = "", quantity = "", unit = "")),
      instructions = listOf(Step("")),
    )

  val xmlTemplate = LlmXml.Default.encodeToString(recipe)
  logger.info("Generated XML Template:\n$xmlTemplate")

  val prompt =
    """
    Please generate a recipe and provide the output in the following XML format:

    $xmlTemplate

    The recipe should be for a delicious dessert.
    """.trimIndent()

  val client =
    OkHttpClient
      .Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()

  val mediaType = "application/json; charset=utf-8".toMediaType()

  val requestBody =
    """
    {
        "model": "claude-3-opus-20240229",
        "max_tokens": 1000,
        "messages": [
            {
                "role": "user",
                "content": ${prompt.jsonEncode()}
            }
        ]
    }
    """.trimIndent().toRequestBody(mediaType)

  val request =
    Request
      .Builder()
      .url("https://api.anthropic.com/v1/messages")
      .post(requestBody)
      .addHeader("Content-Type", "application/json")
      .addHeader("x-api-key", finalApiKey)
      .addHeader("anthropic-version", "2023-06-01")
      .build()

  try {
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string() ?: throw Exception("Empty response body")

    // Parse the JSON response
    val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

    if (jsonResponse.containsKey("error")) {
      val errorMessage =
        jsonResponse["error"]
          ?.jsonObject
          ?.get("message")
          ?.jsonPrimitive
          ?.content
      logger.error("Error from Anthropic API: $errorMessage")
      return
    }

    val content =
      jsonResponse["content"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("text")
        ?.jsonPrimitive
        ?.content
        ?: throw Exception("No content in response")

    // logger.info("Received response from Claude:\n$content")

    // Extract the XML content from the response
    val xmlResponse = content.substringAfter("<Recipe>").substringBefore("</Recipe>")
    val fullXmlResponse = "<Recipe>$xmlResponse</Recipe>"

    // logger.info("Extracted XML Response:\n$fullXmlResponse")

    try {
      val parsedRecipe = LlmXml.Default.decodeFromString<Recipe>(fullXmlResponse)
      logger.info("Parsed Recipe:")
      logger.info("Name: ${parsedRecipe.name}")
      logger.info("Description: ${parsedRecipe.description}")
      logger.info("Ingredients:")
      parsedRecipe.ingredients.forEach { ingredient ->
        logger.info("- ${ingredient.quantity} ${ingredient.unit} ${ingredient.name}")
      }
      logger.info("Instructions:")
      parsedRecipe.instructions.forEachIndexed { index, instruction ->
        logger.info("${index + 1}. $instruction")
      }
    } catch (e: Exception) {
      logger.error("Error parsing the XML response: ${e.message}")
      logger.error("Raw XML response:\n$fullXmlResponse")
    }
  } catch (e: SocketTimeoutException) {
    logger.error("Request timed out: ${e.message}")
  } catch (e: Exception) {
    logger.error("An error occurred: ${e.message}")
  }
}

public fun String.jsonEncode(): String = "\"" + this.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
