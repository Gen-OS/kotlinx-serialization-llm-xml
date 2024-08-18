package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailsResults(
  @LlmListField("emails") val emails: List<EmailResult>,
)

@Serializable
@SerialName("email")
data class EmailResult(
  override val timestamp: Long = 0L,
  @LlmField("emailNumber", "the index for this email") val emailNumber: Int = 0,
  @LlmField("recipientName", "the name of the recipient") val recipientName: String = "",
  @LlmField("recipientEmail", "the email address of the recipient") val recipientEmail: String = "",
  @LlmField("suggestedSubject", "the suggested subject line") val suggestedSubject: String = "",
  @LlmField("suggestedMessage", "the suggested email message content") val suggestedMessage: String = "",
  @LlmField("timeToSend", "YYYY-MM-DD HH:MM:SS") val timeToSend: String = "",
  @LlmField("explanation", "Brief explanation for this contact suggestion") val explanation: String = "",
) : AiOutputResult<EmailResult>()

object EmailResultsTestData : TestData<EmailsResults> {
  override val examplesXml =
    listOf(
      """
    |<EmailsResults>
    |  <emails>
    |    <email>
    |      <emailNumber>1</emailNumber>
    |      <recipientName>John Doe</recipientName>
    |      <recipientEmail>john@example.com</recipientEmail>
    |      <suggestedSubject>Meeting Follow-up</suggestedSubject>
    |      <suggestedMessage>Thank you for your time today...</suggestedMessage>
    |      <timeToSend>2023-06-15 14:30:00</timeToSend>
    |      <explanation>Follow up after the project meeting</explanation>
    |    </email>
    |  </emails>
    |</EmailsResults>
      """.trimMargin(),
    )

  override val examples =
    listOf(
      EmailsResults(
        emails =
        listOf(
          EmailResult(
            emailNumber = 1,
            recipientName = "John Doe",
            recipientEmail = "john@example.com",
            suggestedSubject = "Meeting Follow-up",
            suggestedMessage = "Thank you for your time today...",
            timeToSend = "2023-06-15 14:30:00",
            explanation = "Follow up after the project meeting",
          ),
        ),
      ),
    )

  override val promptObject: EmailsResults
    get() = examples[0]

  override val prompt =
    """
    |<EmailsResults>
    |  <emails>
    |    <email>
    |      <emailNumber>{... the index for this email ...}</emailNumber>
    |      <recipientName>{... the name of the recipient ...}</recipientName>
    |      <recipientEmail>{... the email address of the recipient ...}</recipientEmail>
    |      <suggestedSubject>{... the suggested subject line ...}</suggestedSubject>
    |      <suggestedMessage>{... the suggested email message content ...}</suggestedMessage>
    |      <timeToSend>{... YYYY-MM-DD HH:MM:SS ...}</timeToSend>
    |      <explanation>{... Brief explanation for this contact suggestion ...}</explanation>
    |    </email>
    |    {... other "email" entries ...}
    |  </emails>
    |</EmailsResults>
    """.trimMargin()
}
