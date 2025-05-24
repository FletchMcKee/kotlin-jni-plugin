// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class KtjniPluginTest {
  @TempDir lateinit var testProjectDir: Path

  private lateinit var buildFile: File
  private lateinit var settingsFile: File
  private lateinit var srcDir: File
  private lateinit var testFile: File

  @BeforeEach fun setup() {
    val projectDir = testProjectDir.toFile()
    File(projectDir, "gradle.properties").writeText(
      """
      org.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=256m
      org.gradle.caching=false
      org.gradle.daemon=false
      """.trimIndent()
    )
    buildFile = File(projectDir, "build.gradle.kts")
    settingsFile = File(projectDir, "settings.gradle.kts")

    settingsFile.writeText(
      """
      rootProject.name = "test-project"
      """.trimIndent(),
    )
  }

  @ParameterizedTest
  @EnumSource(KotlinJdkVersion::class)
  fun `plugin applies and generates headers for Kotlin Multiplatform`(kotlinJdkVersion: KotlinJdkVersion) {
    val parent = testProjectDir.toFile()
    srcDir = File(parent, "src/jvmMain/kotlin/com/example").apply { mkdirs() }
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

    val result = createTestRunner(parent)
      .withGradleVersion(kotlinJdkVersion.gradle)
      .build()

    assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateKotlinJvmMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertHeaders(parent, "kotlin", "jvmMain")
  }

  @ParameterizedTest
  @EnumSource(KotlinJdkVersion::class)
  fun `plugin applies and generates headers for Kotlin JVM`(kotlinJdkVersion: KotlinJdkVersion) {
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

    val result = createTestRunner(parent)
      .withGradleVersion(kotlinJdkVersion.gradle)
      .build()

    assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateKotlinMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Kotlin plugin creates a compileJava task for compatibility, so :generateJniHeadersCompileJava exists but is a no-op.
    assertThat(result.task(":generateJavaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    // Verify that no Java/Scala/Groovy tasks were executed
    assertThat(result.tasks.map { it.path }).apply {
      doesNotContain(":generateScalaMainJniHeaders")
      doesNotContain(":generateGroovyMainJniHeaders")
    }

    assertHeaders(parent, "kotlin", "main")
  }

  @ParameterizedTest
  @EnumSource(JavaGradleVersion::class)
  fun `plugin applies and generates headers for Java`(javaGradleVersion: JavaGradleVersion) {
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

      java {
        sourceCompatibility = JavaVersion.VERSION_${javaGradleVersion.jdk}
        targetCompatibility = JavaVersion.VERSION_${javaGradleVersion.jdk}
      }
      """.trimIndent(),
    )

    val result = createTestRunner(parent)
      .withGradleVersion(javaGradleVersion.gradle)
      .build()

    assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateJavaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // Verify that no Kotlin/Scala/Groovy tasks were executed
    assertThat(result.tasks.map { it.path }).apply {
      doesNotContain(":generateKotlinMainJniHeaders")
      doesNotContain(":generateScalaMainJniHeaders")
      doesNotContain(":generateGroovyMainJniHeaders")
    }

    assertHeaders(parent, "java", "main")
  }

  @ParameterizedTest
  @EnumSource(ScalaGradleVersion::class)
  fun `plugin applies and generates headers for Scala`(scalaGradleVersion: ScalaGradleVersion) {
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
        implementation("${scalaGradleVersion.scala}")
      }

      java {
        sourceCompatibility = JavaVersion.VERSION_${scalaGradleVersion.jdk}
        targetCompatibility = JavaVersion.VERSION_${scalaGradleVersion.jdk}
      }
      """.trimIndent(),
    )

    val result = createTestRunner(parent)
      .withGradleVersion(scalaGradleVersion.gradle)
      .build()

    assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":generateScalaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Scala plugin creates a compileJava task for compatibility, so :generateJniHeadersCompileJava exists but is a no-op.
    // assertThat(result.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    // Verify that no Kotlin/Groovy tasks were executed
    assertThat(result.tasks.map { it.path }).apply {
      doesNotContain(":generateKotlinMainJniHeaders")
      doesNotContain(":generateGroovyMainJniHeaders")
    }

    assertHeaders(parent, "scala", "main")
  }

  private fun createTestRunner(
    projectDir: File,
    vararg tasks: String = arrayOf("generateJniHeaders", "--info"),
  ): GradleRunner = GradleRunner.create()
    .withProjectDir(projectDir)
    .withPluginClasspath()
    .withArguments(*tasks)
    .withDebug(true)

  private fun assertHeaders(parent: File, language: String, source: String) {
    val headerDir = File(parent, "build/generated/sources/headers/$language/$source")
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

@Suppress("unused") // Invoked from ParameterizedTest
enum class KotlinJdkVersion(val gradle: String, val kotlin: String, val jdk: Int) {
  K1_8_J17("8.5", "1.9.20", 17),
  K1_9_J17("8.9", "1.9.23", 17),
  K2_0_J21("8.11", "2.0.20", 21),
  K2_1_J21("8.12", "2.1.21", 21),
  K2_2_J21("8.14", "2.2.0-RC", 21),
}

@Suppress("unused") // Invoked from ParameterizedTest
enum class JavaGradleVersion(val gradle: String, val jdk: Int) {
  G7_6_J11("7.6", 11),
  G8_0_J17("8.0", 17),
  G8_5_J21("8.5", 21),
  G8_10_J21("8.11", 21),
}

@Suppress("unused") // Invoked from ParameterizedTest
enum class ScalaGradleVersion(val gradle: String, val scala: String, val jdk: Int) {
  S2_12_G7_6("7.6", "org.scala-lang:scala-library:2.12.18", 11),
  S2_12_G8_0("8.0", "org.scala-lang:scala-library:2.12.19", 11),
  S2_13_G8_5("8.5", "org.scala-lang:scala-library:2.13.12", 17),
  S2_13_G8_10("8.10", "org.scala-lang:scala-library:2.13.14", 17),
  S3_3_G8_12("8.12", "org.scala-lang:scala3-library_3:3.3.1", 21),
  S3_4_G8_14("8.14", "org.scala-lang:scala3-library_3:3.4.2", 21),
}
