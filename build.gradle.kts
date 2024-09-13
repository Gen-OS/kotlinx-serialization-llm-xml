@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  `maven-publish`
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

  wasmJs {
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
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
        implementation("io.github.cdimascio:dotenv-kotlin:6.4.2")
        implementation("org.slf4j:slf4j-api:2.0.16")
        implementation("ch.qos.logback:logback-classic:1.5.8")
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

// application {
//   mainClass.set("dev.genos.kotlinx.serialization.llm.xml.demo.ClaudeDemoKt")
// }

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

// Configure publishing
publishing {
  repositories {
    maven {
      url = uri("https://genos.jfrog.io/artifactory/genos-gradle-dev-local")
      credentials {
        username = System.getenv("GENOS_REPO_USER")
        password = System.getenv("GENOS_REPO_TOKEN")
      }
    }
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/gen-os/littledb")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }

  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])

      groupId = project.group.toString()
      artifactId = project.name
      version = project.version.toString()
    }

    all {
      withBuildIdentifier()
    }
  }
}
