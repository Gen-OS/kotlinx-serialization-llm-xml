package dev.genos.kotlinx.serialization.llm.xml

interface TestData<T> {
  // deserialization
  val examplesXml: List<String>
  val examples: List<T>

  // serialization
  val promptObject: T
  val prompt: String
}
