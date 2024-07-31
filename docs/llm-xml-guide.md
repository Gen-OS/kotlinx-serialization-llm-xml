# LLM XML Guide

This guide provides detailed information about using the kotlinx-serialization-llm-xml library for generating XML templates for Large Language Models (LLMs) and parsing their responses.

## Table of Contents

1. [Annotations](#annotations)
2. [Complex Data Structures](#complex-data-structures)
3. [Best Practices](#best-practices)
4. [Error Handling and Common Pitfalls](#error-handling-and-common-pitfalls)

## Annotations

The kotlinx-serialization-llm-xml library provides several annotations to customize the XML output for LLM interactions.

### @LlmField

The `@LlmField` annotation is used to specify the field name and description when serializing to prompt instructions.

Usage:
```kotlin
@LlmField(name = "fieldName", promptDescription = "description of the field")
val myField: String
```

Parameters:
- `name`: The name to use for this field in the XML output. If not specified, the property name will be used.
- `promptDescription`: A description of the field to be used in the prompt. This will be enclosed in curly braces in the XML output.

Example:
```kotlin
@Serializable
data class Person(
    @LlmField(name = "fullName", promptDescription = "the person's full name")
    val name: String,

    @LlmField(promptDescription = "the person's age in years")
    val age: Int
)
```

Generated XML prompt template:
```xml
<Person>
  <fullName>{... the person's full name ...}</fullName>
  <age>{... the person's age in years ...}</age>
</Person>
```

### @LlmListField

The `@LlmListField` annotation is used to specify a field as a list in the prompt instructions.

Usage:
```kotlin
@LlmListField(name = "listName")
val myList: List<MyType>
```

Parameters:
- `name`: The name to use for this list in the XML output. If not specified, the property name will be used.

Example:
```kotlin
@Serializable
data class Family(
    @LlmField(promptDescription = "the family name")
    val surname: String,

    @LlmListField(name = "members")
    val familyMembers: List<Person>
)
```

Generated XML:
```xml
<Family>
  <surname>{... the family name ...}</surname>
  <members>
    <Person>
      <fullName>{... the person's full name ...}</fullName>
      <age>{... the person's age in years ...}</age>
    </Person>
    {... other "Person" entries ...}
  </members>
</Family>
```

## Complex Data Structures

The kotlinx-serialization-llm-xml library can handle complex nested data structures. Here's an example of a more complex data model:

```kotlin
@Serializable
data class Company(
    @LlmField(promptDescription = "the company's official name")
    val name: String,

    @LlmField(promptDescription = "the year the company was founded")
    val foundedYear: Int,

    @LlmListField(name = "departments")
    val departments: List<Department>
)

@Serializable
data class Department(
    @LlmField(promptDescription = "the name of the department")
    val name: String,

    @LlmField(promptDescription = "the department head's full name")
    val head: String,

    @LlmListField(name = "employees")
    val staff: List<Employee>
)

@Serializable
data class Employee(
    @LlmField(promptDescription = "the employee's full name")
    val name: String,

    @LlmField(promptDescription = "the employee's job title")
    val title: String,

    @LlmField(promptDescription = "the employee's years of experience")
    val yearsOfExperience: Int
)
```

This structure will generate a nested XML template that the LLM can use to provide detailed information about a company, its departments, and employees.

## Best Practices

When designing data models for LLM interactions, consider the following best practices:

1. **Be Specific in Descriptions**: Provide clear and specific descriptions in the `promptDescription` parameter. This helps the LLM understand exactly what information is expected.

2. **Use Meaningful Names**: Choose meaningful names for your fields and classes. This makes the XML structure more intuitive for both developers and the LLM.

3. **Limit Nesting Depth**: While the library can handle deep nesting, try to keep your data structures relatively flat. Extremely deep nesting can make the prompts harder for the LLM to process accurately.

4. **Use Appropriate Types**: Choose the most appropriate Kotlin types for your data. For example, use `Int` for numeric values, `Boolean` for true/false values, etc.

5. **Consider Optionality**: If a field is optional, make it nullable in your Kotlin class. This allows the LLM to omit the field if the information is not available or applicable.

6. **Group Related Information**: Use nested classes to group related information. This creates a more organized structure in the XML.

7. **Provide Examples**: In your prompts to the LLM, consider including example values along with the XML template to guide the model's responses.

## Error Handling and Common Pitfalls

When working with kotlinx-serialization-llm-xml, be aware of these potential issues:

1. **Mismatched Types**: Ensure that the types in your Kotlin classes match the expected input from the LLM. For example, if you expect an integer but the LLM provides a string, deserialization will fail.

2. **Missing Required Fields**: Non-nullable fields in your Kotlin classes must be provided by the LLM. If they're missing, deserialization will throw an exception.

3. **Incorrect XML Structure**: If the LLM generates XML that doesn't match your defined structure, deserialization will fail. Always wrap your deserialization in a try-catch block to handle potential errors.

4. **Large Datasets**: Be cautious when working with very large datasets, as they can lead to performance issues or out-of-memory errors.

5. **Inconsistent Naming**: Ensure consistency between the names used in your Kotlin classes and the XML. Inconsistencies can lead to missing data or deserialization failures.

Example of error handling:

```kotlin
try {
    val result = LlmXml.Default.decodeFromString<Company>(xmlResponse)
    // Process the result
} catch (e: SerializationException) {
    println("Failed to deserialize XML: ${e.message}")
    // Handle the error (e.g., ask the LLM for clarification or reformatting)
}
```

By following these guidelines and being aware of potential pitfalls, you can effectively use kotlinx-serialization-llm-xml to interact with LLMs using structured XML data.
