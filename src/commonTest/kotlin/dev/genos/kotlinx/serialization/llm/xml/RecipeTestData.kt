package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
  @LlmField(promptDescription = "the name of the recipe")
  val name: String,
  @LlmField(promptDescription = "a brief description of the recipe")
  val description: String,
  @LlmListField(tagName = "ingredients")
  val ingredients: List<Ingredient>,
  @LlmListField(tagName = "instructions", listPromptDescription = "all of the steps required to prepare the recipe")
  val instructions: List<Step>,
)

@Serializable
data class Ingredient(
  @LlmField(promptDescription = "the name of the ingredient")
  val name: String,
  @LlmField(promptDescription = "the quantity of the ingredient")
  val quantity: String,
  @LlmField(promptDescription = "the unit of measurement for the ingredient")
  val unit: String,
)

@Serializable
data class Step(
  @LlmField(promptDescription = "one step in the instructions for the recipe")
  val text: String,
)

object RecipeTestData : TestData<Recipe> {
  override val examplesXml =
    listOf(
      """
      <Recipe>
        <name>Chocolate Chip Cookies</name>
        <description>Delicious homemade chocolate chip cookies</description>
        <ingredients>
          <Ingredient>
            <name>All-purpose flour</name>
            <quantity>2 1/4</quantity>
            <unit>cups</unit>
          </Ingredient>
          <Ingredient>
            <name>Chocolate chips</name>
            <quantity>2</quantity>
            <unit>cups</unit>
          </Ingredient>
        </ingredients>
        <instructions>
          <Step>
            <text>Preheat oven to 375°F (190°C)</text>
          </Step>
          <Step>
            <text>Mix ingredients and bake for 9-11 minutes</text>
          </Step>
        </instructions>
      </Recipe>
      """.trimIndent(),
    )

  override val examples =
    listOf(
      Recipe(
        name = "Chocolate Chip Cookies",
        description = "Delicious homemade chocolate chip cookies",
        ingredients =
        listOf(
          Ingredient(
            name = "All-purpose flour",
            quantity = "2 1/4",
            unit = "cups",
          ),
          Ingredient(
            name = "Chocolate chips",
            quantity = "2",
            unit = "cups",
          ),
        ),
        instructions =
        listOf(
          Step("Preheat oven to 375°F (190°C)"),
          Step("Mix ingredients and bake for 9-11 minutes"),
        ),
      ),
    )

  override val promptObject: Recipe
    get() = examples[0]

  override val prompt =
    """
    |<Recipe>
    |  <name>{... the name of the recipe ...}</name>
    |  <description>{... a brief description of the recipe ...}</description>
    |  <ingredients>
    |    <Ingredient>
    |      <name>{... the name of the ingredient ...}</name>
    |      <quantity>{... the quantity of the ingredient ...}</quantity>
    |      <unit>{... the unit of measurement for the ingredient ...}</unit>
    |    </Ingredient>
    |    {... other "Ingredient" entries ...}
    |  </ingredients>
    |  <instructions>
    |    {... all of the steps required to prepare the recipe ...}
    |    <Step>
    |      <text>{... one step in the instructions for the recipe ...}</text>
    |    </Step>
    |    {... other "Step" entries ...}
    |  </instructions>
    |</Recipe>
    """.trimMargin()
}
