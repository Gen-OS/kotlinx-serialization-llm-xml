import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
    classpath("org.jreleaser:jreleaser-gradle-plugin:1.13.1")
  }
}

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  id("com.diffplug.spotless") version "6.18.0"
  id("org.jreleaser") version "1.13.1"
  id("maven-publish")
  application
}

apply(plugin = "org.jetbrains.dokka")

group = "dev.genos.kotlinx.serialization.llm.xml"
version = "0.1.0"

repositories {
  gradlePluginPortal()
  google()
  mavenCentral()
}

kotlin {
  explicitApi()

  jvm {
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
    compilations.all {
      kotlinOptions {
        jvmTarget = "1.8"
      }
    }
  }

  js(IR) {
    browser()
    nodejs()
  }

  // Native targets
  linuxX64()
  linuxArm64()
  macosX64()
  macosArm64()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosSimulatorArm64()
  mingwX64()

  sourceSets {
    val commonMain by getting {
      kotlin.srcDir("src/commonMain/kotlin")
      dependencies {
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)
      }
    }
    val commonTest by getting {
      kotlin.srcDir("src/commonTest/kotlin")
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation("com.squareup.okhttp3:okhttp:4.10.0")
        implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
        implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
        implementation("org.slf4j:slf4j-api:2.0.5")
        implementation("ch.qos.logback:logback-classic:1.4.7")
      }
    }
  }

  sourceSets.all {
    languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
  }

  // Configure native targets
  targets.withType<KotlinNativeTarget>().configureEach {
    binaries.all {
      freeCompilerArgs += listOf("-Xallocator=mimalloc")
    }
  }
}

application {
  mainClass.set("dev.genos.kotlinx.serialization.llm.xml.demo.ClaudeDemoKt")
}

tasks.withType<Test> {
  testLogging {
    showStandardStreams = true
    events("passed", "skipped", "failed", "standardOut", "standardError")
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar by tasks.registering(Jar::class) {
  dependsOn(dokkaHtml)
  archiveClassifier.set("javadoc")
  from(dokkaHtml.outputDirectory)
}

// Configure publications for Maven Central
publishing {
  publications.withType<MavenPublication> {
    artifact(tasks.named("javadocJar"))
    artifact(tasks.named("sourcesJar"))

    groupId = "dev.genos"
    artifactId = "kotlinx.serialization.llm.xml"

    pom {
      name.set("kotlinx-serialization-llm-xml")
      description.set(
        """
        Kotlin Serialization LLM XML is an extension of the kotlinx.serialization library,
        specifically designed for XML-based interactions with Large Language Models (LLMs).
        It provides a seamless way to generate XML prompts and parse XML responses from LLMs
        using Kotlin's serialization framework.
        """.trimIndent(),
      )
      url.set("https://github.com/Gen-OS/kotlinx-serialization-llm-xml")
      licenses {
        license {
          name.set("Apache-2.0")
          url.set("https://spdx.org/licenses/Apache-2.0.html")
        }
      }
      developers {
        developer {
          id.set("rjwalters")
          name.set("Robb Walters")
          email.set("robb@genos.dev")
        }
      }
      scm {
        connection.set("scm:git:https://github.com/Gen-OS/kotlinx-serialization-llm-xml.git")
        developerConnection.set("scm:git:ssh://github.com/Gen-OS/kotlinx-serialization-llm-xml.git")
        url.set("https://github.com/Gen-OS/kotlinx-serialization-llm-xml")
      }
    }
  }

  repositories {
    maven {
      name = "stagingLocal"
      url = uri(layout.buildDirectory.dir("staging-deploy"))
    }
  }
}

// JReleaser configuration
jreleaser {
  project {
    copyright.set("2024 GenOS")
  }
  release {
    github {
      name.set(project.name)
    }
  }
  signing {
    active.set(org.jreleaser.model.Active.ALWAYS)
    armored.set(true)
  }
  deploy {
    maven {
      nexus2 {
        create("mavenCentral") {
          active.set(org.jreleaser.model.Active.ALWAYS)
          url.set("https://s01.oss.sonatype.org/service/local")
          snapshotUrl.set("https://s01.oss.sonatype.org/content/repositories/snapshots/")
          closeRepository.set(true)
          releaseRepository.set(true)
          stagingRepositories.add("build/staging-deploy")
        }
      }
    }
  }
}

// Spotless configuration
spotless {
  kotlin {
    target("**/*.kt")
    ktlint("0.48.2").userData(mapOf("disabled_rules" to "no-wildcard-imports"))
    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktlint("0.48.2")
  }
}
