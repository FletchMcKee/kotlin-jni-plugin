// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import com.google.common.truth.Truth.assertThat
import io.github.fletchmckee.ktjni.util.AndroidVersion
import io.github.fletchmckee.ktjni.util.assertConfigurationCacheReused
import io.github.fletchmckee.ktjni.util.assertJavaAndroidTestsNoSource
import io.github.fletchmckee.ktjni.util.assertKotlinAndroidTestsNoSource
import io.github.fletchmckee.ktjni.util.assertNotIn
import io.github.fletchmckee.ktjni.util.withAndroidConfiguration
import io.github.fletchmckee.ktjni.util.withCommonConfiguration
import io.github.fletchmckee.ktjni.util.writeCommonSettingsFile
import io.github.fletchmckee.ktjni.util.writeJavaAndroidLibraryBuildFile
import io.github.fletchmckee.ktjni.util.writeJavaBuildFile
import io.github.fletchmckee.ktjni.util.writeJavaExampleFile
import io.github.fletchmckee.ktjni.util.writeKmpBuildFile
import io.github.fletchmckee.ktjni.util.writeKotlinAndroidLibraryBuildFile
import io.github.fletchmckee.ktjni.util.writeKotlinExampleFile
import io.github.fletchmckee.ktjni.util.writeKotlinJvmBuildFile
import io.github.fletchmckee.ktjni.util.writeScalaBuildFile
import io.github.fletchmckee.ktjni.util.writeScalaExampleFile
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class KtjniPluginTest {
  @TempDir lateinit var projectRoot: File

  private lateinit var buildFile: File
  private lateinit var settingsFile: File
  private lateinit var srcDir: File
  private lateinit var testFile: File

  @BeforeEach fun setup() {
    val localCacheDir = File(projectRoot, "local-cache")
    buildFile = File(projectRoot, "build.gradle.kts")
    settingsFile = File(projectRoot, "settings.gradle.kts")

    settingsFile.writeCommonSettingsFile(localCacheDir)
  }

  @ParameterizedTest
  @EnumSource(KotlinJdkVersion::class)
  fun `plugin applies and generates headers for Kotlin Multiplatform Kotlin classes`(kotlinJdkVersion: KotlinJdkVersion) {
    srcDir = File(projectRoot, "src/jvmMain/kotlin/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.kt")
    testFile.writeKotlinExampleFile()
    buildFile.writeKmpBuildFile(kotlinJdkVersion)

    val firstRun = createTestRunner(projectRoot).build()

    assertThat(firstRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateKotlinJvmMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Java task should run but have NO-SOURCE as its result.
    assertThat(firstRun.task(":generateJavaJvmMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)

    val secondRun = createTestRunner(projectRoot).build()
    // Verify tasks are restored from cache.
    assertThat(secondRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateKotlinJvmMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

    secondRun.assertConfigurationCacheReused()
    assertHeaders(projectRoot, "build/generated/ktjni/kotlin/jvmMain")
  }

  @ParameterizedTest
  @EnumSource(KotlinJdkVersion::class)
  fun `plugin applies and generates headers for Kotlin Multiplatform Java classes`(kotlinJdkVersion: KotlinJdkVersion) {
    srcDir = File(projectRoot, "src/jvmMain/java/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.java")
    testFile.writeJavaExampleFile()
    buildFile.writeKmpBuildFile(kotlinJdkVersion)

    val firstRun = createTestRunner(projectRoot).build()

    assertThat(firstRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateJavaJvmMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Kotlin task should run but have NO-SOURCE as its result.
    assertThat(firstRun.task(":generateKotlinJvmMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)

    val secondRun = createTestRunner(projectRoot).build()
    // Verify tasks are restored from cache.
    assertThat(secondRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateJavaJvmMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

    secondRun.assertConfigurationCacheReused()
    assertHeaders(projectRoot, "build/generated/ktjni/java/jvmMain")
  }

  @ParameterizedTest
  @EnumSource(KotlinJdkVersion::class)
  fun `plugin applies and generates headers for Kotlin JVM`(kotlinJdkVersion: KotlinJdkVersion) {
    srcDir = File(projectRoot, "src/main/kotlin/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.kt")
    testFile.writeKotlinExampleFile()
    buildFile.writeKotlinJvmBuildFile(kotlinJdkVersion)

    val firstRun = createTestRunner(projectRoot).build()

    assertThat(firstRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateKotlinMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Kotlin plugin creates a compileJava task for compatibility, so :generateJniHeadersCompileJava exists but is a no-op.
    assertThat(firstRun.task(":generateJavaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    // Verify that no Java/Scala/Groovy tasks were executed
    firstRun.assertNotIn(
      ":generateScalaMainJniHeaders",
      ":generateGroovyMainJniHeaders",
    )

    val secondRun = createTestRunner(projectRoot).build()
    // Verify tasks are restored from cache.
    assertThat(secondRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateKotlinMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

    secondRun.assertConfigurationCacheReused()
    assertHeaders(projectRoot, "build/generated/ktjni/kotlin/main")
  }

  @ParameterizedTest
  @EnumSource(AndroidVersion::class)
  fun `plugin applies and generates headers for Kotlin Android`(kotlinAndroid: AndroidVersion) {
    srcDir = File(projectRoot, "src/main/kotlin/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.kt")
    testFile.writeKotlinExampleFile()
    buildFile.writeKotlinAndroidLibraryBuildFile(kotlinAndroid)

    val firstRun = createAndroidTestRunner(projectRoot).build()

    assertThat(firstRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateKotlinDebugJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateKotlinReleaseJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertKotlinAndroidTestsNoSource(firstRun)

    val secondRun = createAndroidTestRunner(projectRoot).build()
    // Verify tasks are restored from cache.
    assertThat(secondRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateKotlinDebugJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateKotlinReleaseJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertKotlinAndroidTestsNoSource(secondRun)

    secondRun.assertConfigurationCacheReused()
    assertHeaders(projectRoot, "build/generated/ktjni/kotlin/debug")
    assertHeaders(projectRoot, "build/generated/ktjni/kotlin/release")
  }

  @ParameterizedTest
  @EnumSource(AndroidVersion::class)
  fun `plugin applies and generates headers for Java Android Library`(javaAndroid: AndroidVersion) {
    srcDir = File(projectRoot, "src/main/java/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.java")
    testFile.writeJavaExampleFile()
    buildFile.writeJavaAndroidLibraryBuildFile(javaAndroid)

    val firstRun = createAndroidTestRunner(projectRoot).build()

    assertThat(firstRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateJavaDebugJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateJavaReleaseJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // Unit test and Android test variants should exist but be NO_SOURCE since we only have main sources
    assertJavaAndroidTestsNoSource(firstRun)

    val secondRun = createAndroidTestRunner(projectRoot).build()
    // Verify tasks are restored from cache.
    assertThat(secondRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateJavaDebugJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateJavaReleaseJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

    secondRun.assertConfigurationCacheReused()
    assertHeaders(projectRoot, "build/generated/ktjni/java/debug")
    assertHeaders(projectRoot, "build/generated/ktjni/java/release")
  }

  @ParameterizedTest
  @EnumSource(JavaGradleVersion::class)
  fun `plugin applies and generates headers for Java`(javaGradleVersion: JavaGradleVersion) {
    srcDir = File(projectRoot, "src/main/java/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.java")
    testFile.writeJavaExampleFile()
    buildFile.writeJavaBuildFile(javaGradleVersion)

    val firstRun = createTestRunner(projectRoot).build()

    assertThat(firstRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateJavaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // Verify that no Kotlin/Scala/Groovy tasks were executed
    firstRun.assertNotIn(
      ":generateKotlinMainJniHeaders",
      ":generateScalaMainJniHeaders",
      ":generateGroovyMainJniHeaders",
    )

    val secondRun = createTestRunner(projectRoot).build()
    // Verify tasks are restored from cache.
    assertThat(secondRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateJavaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

    secondRun.assertConfigurationCacheReused()
    assertHeaders(projectRoot, "build/generated/ktjni/java/main")
  }

  @ParameterizedTest
  @EnumSource(ScalaGradleVersion::class)
  fun `plugin applies and generates headers for Scala`(scalaGradleVersion: ScalaGradleVersion) {
    srcDir = File(projectRoot, "src/main/scala/com/example").apply { mkdirs() }
    testFile = File(srcDir, "Example.scala")
    testFile.writeScalaExampleFile()
    buildFile.writeScalaBuildFile(scalaGradleVersion)

    val firstRun = createTestRunner(projectRoot).build()

    assertThat(firstRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(firstRun.task(":generateScalaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    // The Scala plugin creates a compileJava task for compatibility, so :generateJavaMainJniHeaders exists but is a no-op.
    assertThat(firstRun.task(":generateJavaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
    // Verify that no Kotlin/Groovy tasks were executed
    firstRun.assertNotIn(
      ":generateKotlinMainJniHeaders",
      ":generateGroovyMainJniHeaders",
    )

    val secondRun = createTestRunner(projectRoot).build()
    // Verify tasks are restored from cache.
    assertThat(secondRun.task(":generateJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(secondRun.task(":generateScalaMainJniHeaders")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

    secondRun.assertConfigurationCacheReused()
    assertHeaders(projectRoot, "build/generated/ktjni/scala/main")
  }

  private fun createTestRunner(
    projectDir: File,
    vararg tasks: String = arrayOf("--configuration-cache", "--info"),
  ): GradleRunner = GradleRunner.create()
    .forwardOutput()
    .withCommonConfiguration(projectDir)
    .withPluginClasspath()
    .withArguments(*arrayOf("generateJniHeaders") + tasks)

  private fun createAndroidTestRunner(
    projectDir: File,
    vararg tasks: String = arrayOf("--configuration-cache", "--info"),
  ): GradleRunner = GradleRunner.create()
    .forwardOutput()
    .withAndroidConfiguration(projectDir)
    .withPluginClasspath()
    .withArguments(*arrayOf("generateJniHeaders") + tasks)

  private fun assertHeaders(parent: File, path: String) {
    val headerDir = File(parent, path)
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
