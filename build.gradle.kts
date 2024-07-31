
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  application
  `maven-publish`
  signing
}

group = "dev.genos.kotlinx.serialization.llm"
version = "0.1.0"


repositories {
  gradlePluginPortal()
  google()
  mavenCentral()
}

kotlin {
  explicitApi()

  jvmToolchain(17)

  jvm {
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
    compilations.all {
      kotlinOptions {
        jvmTarget = "11"
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
      // Add the memory allocation implementations
      freeCompilerArgs += listOf("-Xallocator=mimalloc")
    }
  }
}

application {
  mainClass.set("dev.genos.kotlinx.serialization.llm.xml.demo.ClaudeDemoKt")
}

// Configure all test tasks
tasks.withType<Test> {
  testLogging {
    showStandardStreams = true
    events("passed", "skipped", "failed", "standardOut", "standardError")
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(11)
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      pom {
        name.set("Kotlin Serialization LLM XML")
        description.set("XML serialization for Large Language Models in Kotlin")
        url.set("https://github.com/yourusername/kotlinx-serialization-llm-xml")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
          connection.set("scm:git:git://github.com/rjwalters/kotlinx-serialization-llm-xml.git")
          developerConnection.set("scm:git:ssh://github.com/rjwalters/kotlinx-serialization-llm-xml.git")
          url.set("https://github.com/rjwalters/kotlinx-serialization-llm-xml")
        }
      }
    }
  }
  repositories {
    maven {
      url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
      credentials {
        username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
        password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
      }
    }
  }
}

signing {
  sign(publishing.publications["mavenJava"])
}
