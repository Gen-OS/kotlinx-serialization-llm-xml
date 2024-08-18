package dev.genos.kotlinx.serialization.llm.xml

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

@ExperimentalSerializationApi
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class LlmField(
  val tagName: String = "",
  val promptDescription: String = "",
)

@ExperimentalSerializationApi
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class LlmListField(
  val tagName: String = "",
  val listPromptDescription: String = "",
  val elementTagName: String = "",
  val elementPromptDescription: String = "",
)
