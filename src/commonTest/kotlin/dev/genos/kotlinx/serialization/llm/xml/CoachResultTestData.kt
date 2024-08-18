package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("point")
data class ObservationPoint(
  @LlmField(tagName = "observation", promptDescription = "Area that needs improvement") val observation: String = "",
  @LlmField(tagName = "advice", promptDescription = "Specific advice to address this area") val advice: String = "",
)

@Serializable
@SerialName("coach_feedback")
data class CoachResult(
  override val timestamp: Long = 0L,
  @LlmListField(
    "overview",
    listPromptDescription = "",
    elementTagName = "point",
    elementPromptDescription = "Key observation about the user's day",
  )val overview: List<String> = emptyList(),
  @LlmListField(
    "positive_feedback",
    listPromptDescription = "",
    elementTagName = "point",
    elementPromptDescription = "Positive habit or behavior observed",
  )val positiveFeedback: List<String> = emptyList(),
  @LlmListField(
    "areas_for_improvement",
    listPromptDescription = "",
    elementTagName = "",
    elementPromptDescription = "Area that needs improvement",
  ) val areasForImprovement: List<ObservationPoint> = emptyList(),
  @LlmListField(
    "action_items",
    listPromptDescription = "",
    elementTagName = "item",
    elementPromptDescription = "Concrete action for the user to implement",
  )val actionItems: List<String> = emptyList(),
) : AiOutputResult<CoachResult>()

object CoachResultTestData : TestData<CoachResult> {
  override val examplesXml =
    listOf(
      """
  |<coach_feedback>
  |    <overview>
  |        <point>Your day showed a mix of productivity and potential areas for improvement.</point>
  |        <point>You demonstrated good focus during work hours but had some challenges with time management.</point>
  |    </overview>
  |    <positive_feedback>
  |        <point>Great job on completing that challenging project ahead of schedule!</point>
  |        <point>You maintained a consistent exercise routine, which is excellent for your overall well-being.</point>
  |    </positive_feedback>
  |    <areas_for_improvement>
  |        <point>
  |            <observation>You spent a significant amount of time on social media during work hours.</observation>
  |            <advice>Try using a website blocker or app to limit social media access during designated work periods.</advice>
  |        </point>
  |        <point>
  |            <observation>Your sleep schedule seems irregular, with late nights affecting morning productivity.</observation>
  |            <advice>Aim for a consistent sleep schedule, going to bed and waking up at the same time each day to improve your energy levels.</advice>
  |        </point>
  |    </areas_for_improvement>
  |    <action_items>
  |        <item>Set up a website blocker to limit social media access between 9 AM and 5 PM.</item>
  |        <item>Establish a bedtime routine and aim to be in bed by 10:30 PM each night.</item>
  |        <item>Schedule short breaks every 90 minutes during work to maintain focus and avoid burnout.</item>
  |    </action_items>
  |</coach_feedback>
      """.trimMargin(),
    )

  override val examples =
    listOf(
      CoachResult(
        timestamp = 0L,
        overview =
        listOf(
          "Your day showed a mix of productivity and potential areas for improvement.",
          "You demonstrated good focus during work hours but had some challenges with time management.",
        ),
        positiveFeedback =
        listOf(
          "Great job on completing that challenging project ahead of schedule!",
          "You maintained a consistent exercise routine, which is excellent for your overall well-being.",
        ),
        areasForImprovement =
        listOf(
          ObservationPoint(
            observation = "You spent a significant amount of time on social media during work hours.",
            advice = "Try using a website blocker or app to limit social media access during designated work periods.",
          ),
          ObservationPoint(
            observation = "Your sleep schedule seems irregular, with late nights affecting morning productivity.",
            advice = "Aim for a consistent sleep schedule, going to bed and waking up at the same time each day to improve your energy levels.",
          ),
        ),
        actionItems =
        listOf(
          "Set up a website blocker to limit social media access between 9 AM and 5 PM.",
          "Establish a bedtime routine and aim to be in bed by 10:30 PM each night.",
          "Schedule short breaks every 90 minutes during work to maintain focus and avoid burnout.",
        ),
      ),
    )

  override val promptObject: CoachResult
    get() = examples[0]

  override val prompt =
    """
    |<coach_feedback>
    |  <overview>
    |    <point>{... Key observation about the user's day ...}</point>
    |    {... other "point" entries ...}
    |  </overview>
    |  <positive_feedback>
    |    <point>{... Positive habit or behavior observed ...}</point>
    |    {... other "point" entries ...}
    |  </positive_feedback>
    |  <areas_for_improvement>
    |    <point>
    |      <observation>{... Area that needs improvement ...}</observation>
    |      <advice>{... Specific advice to address this area ...}</advice>
    |    </point>
    |    {... other "point" entries ...}
    |  </areas_for_improvement>
    |  <action_items>
    |    <item>{... Concrete action for the user to implement ...}</item>
    |    {... other "item" entries ...}
    |  </action_items>
    |</coach_feedback>
    """.trimMargin()
}
