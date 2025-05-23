// Copyright 2025, Colin McKee
// SPDX-License-Identifier: Apache-2.0
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  `kotlin-dsl`
  `java-gradle-plugin`
  alias(libs.plugins.binary.compatibility.validator)
  alias(libs.plugins.maven.publish)
}

kotlin {
  explicitApi()
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

dependencies {
  // These need to be bundled with the plugin.
  implementation(libs.asm)
  implementation(libs.asm.tree)
  // Need to look into whether this needs to be bundled.
  implementation(libs.kotlin.gradle.plugin)

  // Provided by Gradle runtime.
  compileOnly(gradleApi())
  compileOnly(localGroovy())

  // Test libraries
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.google.truth)
  testImplementation(gradleTestKit())

  testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
  plugins {
    create("ktjni") {
      id = "io.github.fletchmckee.ktjni"
      implementationClass = "io.github.fletchmckee.ktjni.KtjniPlugin"
    }
  }
}

// This module exists in two contexts:
// 1. In the root project where it's built as a publishable artifact
// 2. In the build-support includeBuild where it's used for internal development
if (rootProject.name == "ktjni") {
  mavenPublishing {
    configure(
      GradlePlugin(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = true,
      ),
    )
  }
} else {
  // Use a separate build directory when included in build-support to prevent build cache conflicts and configuration pollution between
  // contexts.
  layout.buildDirectory.set(File(rootProject.rootDir, "build/plugin"))
}
