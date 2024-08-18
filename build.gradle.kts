import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

buildscript {
  repositories {
    gradlePluginPortal()
    google()
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
  signing
  application
}

apply(plugin = "org.jetbrains.dokka")

group = "dev.genos"
version = "0.1.1"

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
  mingwX64()

  targets.all {
    compilations.all {
      kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xexplicit-api=strict"
      }
    }
  }

  sourceSets {
    all {
      languageSettings.apply {
        optIn("kotlinx.serialization.ExperimentalSerializationApi")
      }
    }

    matching { it.name.endsWith("Test") }.all {
      languageSettings.apply {
        optIn("kotlin.RequiresOptIn")
        progressiveMode = true
      }
    }

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

  // Configure all compilations
  targets.all {
    compilations.all {
      kotlinOptions {
        if (name.endsWith("Test", ignoreCase = true)) {
          freeCompilerArgs = freeCompilerArgs.filterNot { it.startsWith("-Xexplicit-api=") }
        } else {
          freeCompilerArgs = freeCompilerArgs + "-Xexplicit-api=strict"
        }
      }
    }
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    if (name.contains("Test", ignoreCase = true)) {
      freeCompilerArgs = freeCompilerArgs - "-Xexplicit-api=strict"
    } else {
      freeCompilerArgs = freeCompilerArgs + "-Xexplicit-api=strict"
    }
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
    val targetName = name.replace("Kotlin", "")

    // Set a unique artifactId for each publication
    artifactId = when (name) {
      "kotlinMultiplatform" -> "kotlinx-serialization-llm-xml"
      else -> "kotlinx-serialization-llm-xml-$targetName"
    }

    println("Configuring publication for $name")
    println("Artifact ID: $artifactId")

    // Add javadoc JAR
    val javadocTask = tasks.findByName("javadocJar")
    if (javadocTask != null) {
      artifact(javadocTask) {
        classifier = "javadoc"
      }
      println("Added javadoc JAR to $name")
    } else {
      println("WARNING: javadocJar task not found for $name")
    }

    groupId = "dev.genos"

    pom {
      packaging = "jar"

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

  println("Finished configuring publication: $name")
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  setRequired { gradle.taskGraph.hasTask("publish") }
  sign(publishing.publications)
}

// Configure signing for all publications
afterEvaluate {
  publishing.publications.withType<MavenPublication>().all {
    signing.sign(this)
  }
}

// JReleaser configuration
jreleaser {
  project {
    copyright.set("2024 GenOS")
    name.set("kotlinx-serialization-llm-xml")
    //version.set(project.version.toString())
    //website.set("https://github.com/Gen-OS/kotlinx-serialization-llm-xml")
    description.set("Kotlin Serialization extension for XML-based interactions with Large Language Models")
    authors.set(listOf("Robb Walters"))
    license.set("Apache-2.0")
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
      mavenCentral {
        create("sonatype") {
          active.set(org.jreleaser.model.Active.ALWAYS)
          url.set("https://central.sonatype.com/api/v1/publisher")
          stagingRepository("build/staging-deploy")
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
