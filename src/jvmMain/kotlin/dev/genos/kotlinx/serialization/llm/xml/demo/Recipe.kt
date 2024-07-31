package dev.genos.kotlinx.serialization.llm.xml.demo

import dev.genos.kotlinx.serialization.llm.xml.LlmField
import dev.genos.kotlinx.serialization.llm.xml.LlmListField
import kotlinx.serialization.Serializable

@Serializable
public data class Recipe(
  @LlmField(promptDescription = "the name of the recipe")
  val name: String,
  @LlmField(promptDescription = "a brief description of the recipe")
  val description: String,
  @LlmListField(tagName = "ingredients")
  val ingredients: List<Ingredient>,
  @LlmListField(tagName = "instructions", listPromptDescription = "the instructions for preparing the recipe")
  val instructions: List<Step>,
)

@Serializable
public data class Ingredient(
  @LlmField(promptDescription = "the name of the ingredient")
  val name: String,
  @LlmField(promptDescription = "the quantity of the ingredient")
  val quantity: String,
  @LlmField(promptDescription = "the unit of measurement for the ingredient")
  val unit: String,
)

@Serializable
public data class Step(
  @LlmField(tagName = "step", promptDescription = "one step in the instructions for the recipe")
  val step: String,
)
