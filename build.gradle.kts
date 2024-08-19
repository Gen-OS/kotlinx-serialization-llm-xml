import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.signatory.pgp.PgpSignatoryFactory
import java.io.ByteArrayInputStream

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
  signing
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  id("com.diffplug.spotless") version "6.18.0"
  id("org.jreleaser") version "1.13.1"
  id("maven-publish")
  application
}

apply(plugin = "org.jetbrains.dokka")
apply(plugin = "signing")

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
  targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
    binaries.all {
      freeCompilerArgs += listOf("-Xallocator=mimalloc")
      binaryOption("bundleKlibPart", "true")
    }

    val targetName = name

    // Create JAR task for each native target with the correct naming
    tasks.register<Jar>("${targetName}Jar") {
      from(compilations["main"].output.allOutputs)
      // Remove the classifier to match the expected naming convention
      archiveClassifier.set("")
      // Set the correct name for the JAR
      archiveBaseName.set("kotlinx-serialization-llm-xml-$targetName")
    }

    // Configure Maven publication for native targets
    mavenPublication {
      artifactId = "kotlinx-serialization-llm-xml-$targetName"
      artifact(tasks["${targetName}Jar"])
      pom {
        packaging = "jar"
      }
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

afterEvaluate {
  tasks.withType<AbstractPublishToMaven>().configureEach {
    // Find the corresponding signing task
    val signingTask = tasks.withType<Sign>().find { it.name == "sign${name.capitalize()}Publication" }

    // Set up task dependencies
    dependsOn(tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>())
    dependsOn(tasks.withType<Jar>())

    // Ensure correct task ordering
    signingTask?.let { mustRunAfter(it) }
  }
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar by tasks.registering(Jar::class) {
  dependsOn(dokkaHtml)
  archiveClassifier.set("javadoc")
  from(dokkaHtml.outputDirectory)
}

tasks.named<Jar>("jsJar") {
  dependsOn(kotlin.js().compilations["main"].compileKotlinTask)

  // Remove the classifier to match the expected naming convention
  archiveClassifier.set("")
  // Set the correct name for the JAR
  archiveBaseName.set("kotlinx-serialization-llm-xml-js")
  // Ensure the extension is .jar
  archiveExtension.set("jar")

  // Include the KLIB file
  from(kotlin.js().compilations["main"].output.allOutputs) {
    include("*.klib")
  }

  // Include JavaScript files
  from(kotlin.js().compilations["main"].compileKotlinTask.destinationDirectory) {
    include("*.js")
  }

  // Explicitly set the output directory
  destinationDirectory.set(layout.buildDirectory.dir("libs"))

  // Add some logging
  doFirst {
    println("jsJar task is starting")
    println("Output directory: ${destinationDirectory.get()}")
    println("Archive file name: ${archiveFileName.get()}")
  }

  doLast {
    println("jsJar task has finished")
    if (archiveFile.get().asFile.exists()) {
      println("JAR file exists")
    } else {
      println("JAR file does not exist")
    }
  }
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

    println("\nConfiguring publication for $name")
    println("Artifact ID: $artifactId")

    // Add main artifact (JAR)
    val jarTaskName = when {
      name == "kotlinMultiplatform" -> "allMetadataJar"
      else -> "${targetName.decapitalize()}Jar"
    }
    val jarTask = tasks.findByName(jarTaskName) as? Jar
    if (jarTask != null) {
      artifact(jarTask)
      println("Added ${jarTask.name} to $name")
    } else {
      println("WARNING: Main JAR task not found for publication $name")
    }

    // Add javadoc JAR
    val javadocTask = tasks.findByName("javadocJar")
    if (javadocTask != null) {
      artifact(javadocTask) {
        classifier = "javadoc"
      }
      println("Added javadocJar to $name")
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

  println("\nFinished configuring publications for $name")
}

// JReleaser configuration
jreleaser {
  project {
    copyright.set("2024 GenOS")
    name.set("kotlinx-serialization-llm-xml")
    // version.set(project.version.toString())
    // website.set("https://github.com/Gen-OS/kotlinx-serialization-llm-xml")
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

tasks.withType<Jar>().configureEach {
  doLast {
    val outputFile = archiveFile.get().asFile
    if (outputFile.exists()) {
      println("JAR file created: ${outputFile.absolutePath}")
      println("JAR file size: ${outputFile.length()} bytes")
    } else {
      println("Warning: JAR file not created: ${outputFile.absolutePath}")
    }
  }
}

val signingKeyId: String? = project.findProperty("signing.keyId") as String?
val signingKey: String? = project.findProperty("signing.secretKey") as String?
val signingPassword: String? = project.findProperty("signing.password") as String?

signing {
  useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
  sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
  val publicationName = name
    .substringAfter("sign")
    .substringBefore("Publication")
    .decapitalize()

  println("Configuring $name for publication: $publicationName")

  val jarTask = tasks.findByName("${publicationName}Jar") as? Jar
  if (jarTask != null) {
    dependsOn(jarTask)
    inputs.files(jarTask.archiveFile)
  }

  val publication = publishing.publications.findByName(publicationName) as? MavenPublication

  doFirst {
    println("Executing signing task: ${this.name}")
    println("Signing key ID available: ${!signingKeyId.isNullOrEmpty()}")
    println("Signing key available: ${signingKey.isNullOrEmpty()}")
    println("Signing password available: ${!signingPassword.isNullOrEmpty()}")

    println("Input files for $name:")
    inputs.files.forEach { file ->
      println("  path: ${file.absolutePath}")
      println("  exists: ${file.exists()}")
      println("  size: ${file.length()} bytes")
    }

    if (publication == null) {
      println("ERROR: Publication not found for $publicationName")
    }

    if (signatory == null) {
      println("ERROR: signatory not found for $publicationName")
    }

    /* I'm not sure why the signatory is always null... when I come back to this
       my plan is to try to go back to specifying keys from files and rings rather
       than use in memory signing.
     */
  }
}


tasks.register("testSign") {
  doLast {
    val dummyFile = file("${buildDir}/dummy.txt")
    dummyFile.writeText("This is a test file for signing")

    println("Signing configuration:")
    println("Signing key ID: ${project.findProperty("signing.keyId")}")
    println("Signing key available: ${project.findProperty("signing.secretKey").toString().isNotEmpty()}")
    println("Signing password available: ${project.findProperty("signing.password").toString().isNotEmpty()}")

    try {
      signing.sign(dummyFile)
      println("Signing task executed successfully")
    } catch (e: Exception) {
      println("Signing task failed: ${e.message}")
      e.printStackTrace()
    }

    if (file("${buildDir}/dummy.txt.asc").exists()) {
      println("Test signing successful: ${buildDir}/dummy.txt.asc created")
    } else {
      println("Test signing failed: ${buildDir}/dummy.txt.asc not created")
    }
  }
}
