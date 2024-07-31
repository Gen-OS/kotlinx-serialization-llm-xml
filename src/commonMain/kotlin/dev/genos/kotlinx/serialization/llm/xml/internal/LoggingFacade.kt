package dev.genos.kotlinx.serialization.llm.xml.internal

public object LoggingFacade {
  private var isEnabled: Boolean = false

  public fun debug(message: () -> String) {
    if (isEnabled) {
      println("[DEBUG] ${message()}")
    }
  }

  public fun info(message: () -> String) {
    if (isEnabled) {
      println("[INFO] ${message()}")
    }
  }

  public fun warn(message: () -> String) {
    if (isEnabled) {
      println("[WARN] ${message()}")
    }
  }

  // Add other log levels as needed (warn, error, etc.)
}
