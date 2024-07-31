package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.Serializable

@Serializable
data class Greeting(
  @LlmField("from", "the name of the greeter") val from: String,
  @LlmField("to", "the name of the person being greeted") val to: String,
  @LlmListField("messages") val messages: List<Message>,
)

@Serializable
data class Message(
  @LlmField("index", "the index for this greeting content entry") val index: Int,
  @LlmField("content", "the greeting content for this index") val content: String,
)

object GreetingTestData : TestData<Greeting> {
  override val examplesXml =
    listOf(
      """
      <Greeting>
        <from>AI</from>
        <to>Human</to>
        <messages>
          <Message>
            <index>0</index>
            <content>Hello, how can I assist you today?</content>
          </Message>
        </messages>
      </Greeting>
      """.trimIndent(),
      """
      <Greeting>
        <from>AI</from>
        <to>Human</to>
        <messages>
          <Message>
            <index>0</index>
            <content>Hello, how can I assist you today?</content>
          </Message>
          <Message>
            <index>1</index>
            <content>I'm here to help with any questions you may have.</content>
          </Message>
        </messages>
      </Greeting>
      """.trimIndent(),
      """
      <Greeting>
        <from>AI</from>
        <to>Human</to>
      </Greeting>
      """.trimIndent(),
      """
      <Greeting>
        <from>AI</from>
        <to>Human</to>
        <messages></messages>
      </Greeting>
      """.trimIndent(),
    )

  override val examples =
    listOf(
      Greeting(
        from = "AI",
        to = "Human",
        messages =
          listOf(
            Message(
              index = 0,
              content = "Hello, how can I assist you today?",
            ),
          ),
      ),
      Greeting(
        from = "AI",
        to = "Human",
        messages =
          listOf(
            Message(
              index = 0,
              content = "Hello, how can I assist you today?",
            ),
            Message(
              index = 1,
              content = "I'm here to help with any questions you may have.",
            ),
          ),
      ),
      Greeting(
        from = "AI",
        to = "Human",
        messages = emptyList(),
      ),
      Greeting(
        from = "AI",
        to = "Human",
        messages = emptyList(),
      ),
    )

  override val promptObject: Greeting
    get() = examples[0]

  override val prompt =
    """
    |<Greeting>
    |  <from>{... the name of the greeter ...}</from>
    |  <to>{... the name of the person being greeted ...}</to>
    |  <messages>
    |    <Message>
    |      <index>{... the index for this greeting content entry ...}</index>
    |      <content>{... the greeting content for this index ...}</content>
    |    </Message>
    |    {... other "Message" entries ...}
    |  </messages>
    |</Greeting>
    """.trimMargin()
}
