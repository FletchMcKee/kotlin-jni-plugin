// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni.util

import com.google.common.truth.Truth.assertThat
import io.github.fletchmckee.ktjni.JavaGradleVersion
import io.github.fletchmckee.ktjni.KotlinJdkVersion
import io.github.fletchmckee.ktjni.ScalaGradleVersion
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

internal fun GradleRunner.withCommonConfiguration(projectRoot: File): GradleRunner {
  File(projectRoot, "gradle.properties").writeText(
    """
      org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
    """.trimIndent(),
  )
  return withProjectDir(projectRoot)
    .withTestKitDir(File("build/gradle-test-kit").absoluteFile)
}

internal fun File.writeCommonSettingsFile(localCacheDir: File) = writeText(
  """
  rootProject.name = "test-project"

  dependencyResolutionManagement {
    repositories {
      mavenLocal()
      mavenCentral()
      google()
    }
  }

  buildCache {
    local {
      directory = file("${localCacheDir.toURI()}")
    }
  }

  """.trimIndent(),
)

internal fun File.writeKotlinExampleFile() = writeText(
  """
  package com.example

  class Example {
    external fun exampleNative(): Int
  }

  """.trimIndent(),
)

internal fun File.writeJavaExampleFile() = writeText(
  """
  package com.example;

  public class Example {
    public native int exampleNative();
  }

  """.trimIndent(),
)

internal fun File.writeScalaExampleFile() = writeText(
  """
  package com.example

  class Example {
    @native
    def exampleNative(): Int
  }

  """.trimIndent(),
)

internal fun File.writeKmpBuildFile(kotlinJdkVersion: KotlinJdkVersion) = writeText(
  """
  import org.jetbrains.kotlin.gradle.dsl.JvmTarget
  import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

  plugins {
    kotlin("multiplatform") version "${kotlinJdkVersion.kotlin}"
    id("io.github.fletchmckee.ktjni")
  }

  repositories {
    mavenCentral()
  }

  kotlin {
    jvm {
      compilations.all {
        kotlinOptions {
          jvmTarget = "${kotlinJdkVersion.jdk}"
        }
      }

      java {
        sourceCompatibility = JavaVersion.VERSION_${kotlinJdkVersion.jdk}
        targetCompatibility = JavaVersion.VERSION_${kotlinJdkVersion.jdk}
      }
    }
  }
  """.trimIndent(),
)

internal fun File.writeKotlinJvmBuildFile(kotlinJdkVersion: KotlinJdkVersion) = writeText(
  """
  import org.jetbrains.kotlin.gradle.dsl.JvmTarget
  import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

  plugins {
    kotlin("jvm") version "${kotlinJdkVersion.kotlin}"
    id("io.github.fletchmckee.ktjni")
  }

  repositories {
    mavenCentral()
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_${kotlinJdkVersion.jdk}
    targetCompatibility = JavaVersion.VERSION_${kotlinJdkVersion.jdk}
  }

  tasks.withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.valueOf("JVM_${kotlinJdkVersion.jdk}"))
    }
  }
  """.trimIndent(),
)

internal fun File.writeJavaBuildFile(javaGradleVersion: JavaGradleVersion) = writeText(
  """
  plugins {
    java
    id("io.github.fletchmckee.ktjni")
  }

  repositories {
    mavenCentral()
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_${javaGradleVersion.jdk}
    targetCompatibility = JavaVersion.VERSION_${javaGradleVersion.jdk}
  }

  """.trimIndent(),
)

internal fun File.writeScalaBuildFile(scalaGradleVersion: ScalaGradleVersion) = writeText(
  """
  plugins {
    scala
    id("io.github.fletchmckee.ktjni")
  }

  repositories {
    mavenCentral()
  }

  dependencies {
    implementation("${scalaGradleVersion.scala}")
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_${scalaGradleVersion.jdk}
    targetCompatibility = JavaVersion.VERSION_${scalaGradleVersion.jdk}
  }

  """.trimIndent(),
)

internal fun BuildResult.assertConfigurationCacheReused() {
  assertThat(output).contains("Reusing configuration cache")
}

internal fun assertKotlinAndroidTestsNoSource(result: BuildResult) {
  // There is no ReleaseAndroidTest variant.
  assertThat(result.task(":generateKotlinDebugAndroidTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
  assertThat(result.task(":generateKotlinDebugUnitTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
  assertThat(result.task(":generateKotlinReleaseUnitTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
}

internal fun assertJavaAndroidTestsNoSource(result: BuildResult) {
  // There is no ReleaseAndroidTest variant.
  assertThat(result.task(":generateJavaDebugAndroidTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
  assertThat(result.task(":generateJavaDebugUnitTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
  assertThat(result.task(":generateJavaReleaseUnitTestJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
}

internal fun BuildResult.assertNotIn(vararg taskPaths: String) {
  assertThat(this.tasks.map { it.path }).containsNoneIn(taskPaths)
}

internal fun String.withInvariantPathSeparators() = replace("\\", "/")
