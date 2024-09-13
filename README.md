# Kotlin Serialization LLM XML

[![Kotlin](https://img.shields.io/badge/kotlin-2.0.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-serialization-llm-xml/0.0.1)](https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-llm-xml/0.0.1)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

Kotlin Serialization LLM XML is an extension of the kotlinx.serialization library, specifically designed for XML-based interactions with Large Language Models (LLMs). It provides a seamless way to generate XML prompts and parse XML responses from LLMs using Kotlin's serialization framework.

## Features

* Extends kotlinx.serialization with LLM-specific functionality
* Supports Kotlin classes marked as `@Serializable` with additional LLM annotations
* Provides XML format optimized for LLM interactions
* Complete multiplatform support: JVM, JS, and Native

## Table of Contents

* [Introduction](#introduction)
* [Setup](#setup)
* [Usage](#usage)
* [LLM-specific Annotations](#llm-specific-annotations)
* [Claude Demo](#claude-demo)
* [Advanced Usage](#advanced-usage)
* [Contributing](#contributing)
* [License](#license)

## Introduction

This library is designed to serialize Kotlin data classes into XML templates. These templates serve as instructions for Large Language Models (LLMs), describing how the LLM should structure its response. The LLM's response, following this template, can then be easily deserialized back into Kotlin objects.

Here's a simple example:

```kotlin
import kotlinx.serialization.*
import dev.genos.kotlinx.serialization.llm.xml.*

@Serializable
data class Greeting(
    @LlmField("from", "the name of the greeter")
    val from: String,

    @LlmField("to", "the name of the person being greeted")
    val to: String,

    @LlmListField("messages")
    val messages: List<Message>
)

@Serializable
data class Message(
    @LlmField("index", "the index for this greeting content entry")
    val index: Int,

    @LlmField("content", "the greeting content for this index")
    val content: String
)

fun main() {
    val xmlTemplate = LlmXml.Default.encodeToString(Greeting.serializer(), Greeting::class)
    println(xmlTemplate)
}
```

This will generate the following XML template for use in an LLM prompt:

```xml
<Greeting>
  <from>{... the name of the greeter ...}</from>
  <to>{... the name of the person being greeted ...}</to>
  <messages>
    <Message>
      <index>{... the index for this greeting content entry ...}</index>
      <content>{... the greeting content for this index ...}</content>
    </Message>
    {... other "Message" entries ...}
  </messages>
</Greeting>
```
## Setup

### Gradle

Add the following to your `build.gradle.kts` file:

```kotlin
plugins {
    kotlin("jvm") version "2.0.0" // or kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.0.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-llm-xml:0.0.1")
}
```

### Maven

Add the following to your `pom.xml` file:

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-serialization-llm-xml</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Usage

1. Mark your data classes with `@Serializable`
2. Use LLM-specific annotations to customize serialization behavior
3. Use `LlmXml.Default.encodeToString()` to generate the XML pattern template for your prompt
4. Send the generated XML to your LLM
5. Use `LlmXml.Default.decodeFromString()` to parse an object from the response

## LLM-specific Annotations

- `@LlmField(name: String, promptDescription: String)`: Specifies the field name and description when serializing to prompt instructions
- `@LlmListField(name: String)`: Specifies a field as a list in the prompt instructions. Note: There are currently limitations with lists of primitive types (see [Limitations](#limitations)).

## Claude Demo

This project includes a demonstration of how to use the library with Anthropic's Claude LLM. The demo showcases a complete round-trip use case:

1. Defining a Kotlin data class with LLM-specific annotations
2. Generating an XML template from the data class
3. Creating a prompt for Claude using the template
4. Sending the prompt to Claude's API
5. Parsing Claude's XML response back into a Kotlin object

### Running the Demo

To run the Claude demo, you have two options for providing your Anthropic API key:

1. Using a .env file:

   Create a `.env` file in the root directory of the project with the following content:

```
ANTHROPIC_API_KEY=sk-ant-your-actual-api-key-here
```
   Then run the demo without any arguments:

```sh
   ./gradlew run
```

2. Alternatively, you can provide a key using a command-line argument:
```sh
   ./gradlew run --args="-k your_api_key_here"
```

The demo will prioritize the API key provided via command-line argument if both methods are used.

The demo will output:
- The generated XML template
- The parsed recipe details

### Demo Code Structure

The demo consists of two main files:

1. `Recipe.kt`: Defines the `Recipe` and `Ingredient` data classes with LLM-specific annotations.
2. `ClaudeDemo.kt`: Contains the main executable that demonstrates the round-trip process.

You can find these files in the `src/jvmMain/kotlin/dev/genos/kotlinx/serialization/llm/xml/demo/` directory.

### Customizing the Demo

Feel free to modify the `Recipe` data class or create your own data classes to experiment with different structures and prompts. You can adjust the prompt in `ClaudeDemo.kt` to generate different types of content.

Remember to handle API keys securely and never commit them to version control.

## Advanced Usage

For more detailed information and advanced usage, please refer to our [LLM XML Guide](docs/llm-xml-guide.md).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
