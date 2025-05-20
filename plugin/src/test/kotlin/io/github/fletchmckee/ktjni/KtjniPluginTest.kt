// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Path
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class KtjniPluginTest {
  @TempDir
  lateinit var testProjectDir: Path

  private lateinit var buildFile: File
  private lateinit var settingsFile: File
  private lateinit var srcDir: File
  private lateinit var testFile: File

  @BeforeEach
  fun setup() {
    val projectDir = testProjectDir.toFile()
    buildFile = File(projectDir, "build.gradle.kts")
    settingsFile = File(projectDir, "settings.gradle.kts")

    settingsFile.writeText(
      """
      rootProject.name = "test-project"
      """.trimIndent(),
    )
  }

  @Test
  fun `plugin applies and generates headers for Kotlin`() {
    val parent = testProjectDir.toFile()
    srcDir = File(parent, "src/main/kotlin/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.kt")
    testFile.writeText(
      """
      package com.example

      class Example {
        external fun exampleNative(): Int
      }
      """.trimIndent(),
    )

    buildFile.writeText(
      """
      plugins {
        kotlin("jvm") version "1.9.23"
        id("io.github.fletchmckee.ktjni")
      }

      repositories {
        mavenCentral()
      }
      """.trimIndent(),
    )

    val result = createTestRunner(parent)

    assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateJniHeadersCompileKotlin")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Kotlin plugin creates a compileJava task for compatibility, so :generateJniHeadersCompileJava exists but is a no-op.
    assertThat(result.task(":generateJniHeadersCompileJava")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    // Verify that no Scala/Groovy tasks were executed
    assertThat(result.tasks.map { it.path }).apply {
      doesNotContain(":generateJniHeadersCompileScala")
      doesNotContain(":generateJniHeadersCompileGroovy")
    }

    assertHeaders(parent, "kotlin")
  }

  @Test
  fun `plugin applies and generates headers for Java`() {
    val parent = testProjectDir.toFile()
    srcDir = File(parent, "src/main/java/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.java")
    testFile.writeText(
      """
      package com.example;

      public class Example {
        public native int exampleNative();
      }
      """.trimIndent(),
    )

    buildFile.writeText(
      """
      plugins {
        java
        id("io.github.fletchmckee.ktjni")
      }

      repositories {
        mavenCentral()
      }
      """.trimIndent(),
    )

    val result = createTestRunner(parent)

    assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateJniHeadersCompileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // Verify that no Kotlin/Scala/Groovy tasks were executed
    assertThat(result.tasks.map { it.path }).apply {
      doesNotContain(":generateJniHeadersCompileKotlin")
      doesNotContain(":generateJniHeadersCompileScala")
      doesNotContain(":generateJniHeadersCompileGroovy")
    }

    assertHeaders(parent, "java")
  }

  @Test
  fun `plugin applies and generates headers for Scala`() {
    val parent = testProjectDir.toFile()
    srcDir = File(parent, "src/main/scala/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.scala")
    testFile.writeText(
      """
      package com.example

      class Example {
        @native
        def exampleNative(): Int
      }
      """.trimIndent(),
    )

    buildFile.writeText(
      """
      plugins {
        scala
        id("io.github.fletchmckee.ktjni")
      }

      repositories {
        mavenCentral()
      }

      dependencies {
        implementation("org.scala-lang:scala-library:2.12.18")
      }
      """.trimIndent(),
    )

    val result = createTestRunner(parent)

    assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateJniHeadersCompileScala")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Scala plugin creates a compileJava task for compatibility, so :generateJniHeadersCompileJava exists but is a no-op.
    assertThat(result.task(":generateJniHeadersCompileJava")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    // Verify that no Kotlin/Groovy tasks were executed
    assertThat(result.tasks.map { it.path }).apply {
      doesNotContain(":generateJniHeadersCompileKotlin")
      doesNotContain(":generateJniHeadersCompileGroovy")
    }

    assertHeaders(parent, "scala")
  }

  private fun createTestRunner(
    projectDir: File,
    vararg tasks: String = arrayOf("generateJniHeaders", "--info"),
  ): BuildResult = GradleRunner.create()
    .withProjectDir(projectDir)
    .withPluginClasspath()
    .withArguments(*tasks)
    .withDebug(true)
    .build()

  private fun assertHeaders(parent: File, language: String) {
    val headerDir = File(parent, "build/generated/sources/headers/$language")
    assertThat(headerDir.exists()).isTrue()

    val headerFile = File(headerDir, "com_example_Example.h")
    assertThat(headerFile.exists()).isTrue()

    val headerContent = headerFile.readText()
    assertThat(headerContent).isEqualTo(expectedOutcome)
  }

  private companion object {
    val expectedOutcome = """
      /* DO NOT EDIT THIS FILE - it is machine generated */
      #include <jni.h>
      /* Header for class com_example_Example */

      #ifndef _Included_com_example_Example
      #define _Included_com_example_Example
      #ifdef __cplusplus
      extern "C" {
      #endif

      /*
       * Class:     com_example_Example
       * Method:    exampleNative
       * Signature: ()I
       */
      JNIEXPORT jint JNICALL Java_com_example_Example_exampleNative
        (JNIEnv *, jobject);

      #ifdef __cplusplus
      }
      #endif
      #endif

    """.trimIndent()
  }
}
