// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
package io.github.fletchmckee.ktjni

import java.io.File
import java.util.Properties
import org.gradle.testkit.runner.GradleRunner

// Credit to the SqlDelight team.
// https://github.com/sqldelight/sqldelight/blob/master/sqldelight-gradle-plugin/src/instrumentationTest/kotlin/app/cash/sqldelight/AndroidHome.kt
internal fun androidHome(): String {
  System.getenv("ANDROID_SDK_ROOT")?.let { return it.withInvariantPathSeparators() }
  System.getenv("ANDROID_HOME")?.let { return it.withInvariantPathSeparators() }

  val localProp = File(File(System.getProperty("user.dir")).parentFile, "local.properties")
  if (localProp.exists()) {
    val prop = Properties()
    localProp.inputStream().use {
      prop.load(it)
    }
    val sdkHome = prop.getProperty("sdk.dir")
    if (sdkHome != null) {
      return sdkHome.withInvariantPathSeparators()
    }
  }
  error("Missing 'ANDROID_HOME' environment variable or local.properties with 'sdk.dir'")
}

internal fun GradleRunner.withAndroidConfiguration(projectRoot: File): GradleRunner {
  File(projectRoot, "gradle.properties").writeText(
    """
      org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
      android.useAndroidX=true
    """.trimIndent(),
  )
  File(projectRoot, "local.properties").apply {
    if (!exists()) writeText("sdk.dir=${androidHome()}\n")
  }
  return withProjectDir(projectRoot)
    .withTestKitDir(File("build/gradle-test-kit").absoluteFile)
}

@Suppress("unused") // Invoked from ParameterizedTest
enum class AndroidVersion(val gradle: String, val agp: String, val kotlin: String, val jdk: Int) {
  K1_8_J17("8.5", "8.1.4", "1.9.20", 17),
  K1_9_J17("8.9", "8.5.2", "1.9.23", 17),
  K2_0_J21("8.11", "8.7.3", "2.0.20", 21),
  K2_1_J21("8.12", "8.8.0", "2.1.21", 21),
  K2_2_J21("8.14", "8.9.0", "2.2.0-RC", 21),
}
